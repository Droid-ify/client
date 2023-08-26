package com.looker.droidify

import android.annotation.SuppressLint
import android.app.Application
import android.app.job.JobInfo
import android.app.job.JobScheduler
import android.content.*
import android.os.Build
import androidx.work.NetworkType
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.disk.DiskCache
import coil.memory.MemoryCache
import com.looker.core.common.Constants
import com.looker.core.common.SdkCheck
import com.looker.core.common.cache.Cache
import com.looker.core.common.extension.getInstalledPackagesCompat
import com.looker.core.common.sdkAbove
import com.looker.core.datastore.UserPreferencesRepository
import com.looker.core.datastore.getProperty
import com.looker.core.datastore.model.*
import com.looker.droidify.content.ProductPreferences
import com.looker.droidify.database.Database
import com.looker.droidify.index.RepositoryUpdater
import com.looker.droidify.receivers.InstalledAppReceiver
import com.looker.droidify.service.Connection
import com.looker.droidify.service.SyncService
import com.looker.droidify.sync.SyncPreference
import com.looker.droidify.sync.toJobNetworkType
import com.looker.droidify.utility.Utils.toInstalledItem
import com.looker.droidify.work.CleanUpWorker
import com.looker.installer.InstallManager
import com.looker.installer.installers.shizuku.ShizukuPermissionHandler
import com.looker.network.Downloader
import com.topjohnwu.superuser.Shell
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.net.InetSocketAddress
import java.net.Proxy
import javax.inject.Inject
import kotlin.time.Duration.Companion.INFINITE
import kotlin.time.Duration.Companion.ZERO
import kotlin.time.Duration.Companion.hours
import com.looker.core.common.R as CommonR

@HiltAndroidApp
class MainApplication : Application(), ImageLoaderFactory {

	private val appScope = CoroutineScope(Dispatchers.Default)

	@Inject
	lateinit var userPreferencesRepository: UserPreferencesRepository
	private val userPreferenceFlow get() = userPreferencesRepository.userPreferencesFlow

	@Inject
	lateinit var installer: InstallManager

	@Inject
	lateinit var downloader: Downloader

	@Inject
	lateinit var shizukuPermissionHandler: ShizukuPermissionHandler

	override fun onCreate() {
		super.onCreate()

		val databaseUpdated = Database.init(this)
		ProductPreferences.init(this, appScope)
		RepositoryUpdater.init(appScope, downloader)
		listenApplications()
		updatePreference()
		setupInstaller()

		if (databaseUpdated) forceSyncAll()
	}

	override fun onTerminate() {
		super.onTerminate()
		appScope.cancel("Application Terminated")
		installer.close()
	}

	private fun setupInstaller() {
		appScope.launch {
			launch {
				combine(
					userPreferenceFlow.getProperty { installerType },
					flowOf(shizukuPermissionHandler.isInstalled()),
					shizukuPermissionHandler.isGranted,
					shizukuPermissionHandler.isBinderAlive
				) { installerType, isInstalled, isGranted, isAlive ->
					if (installerType == InstallerType.SHIZUKU) {
						if (!isAlive || !isInstalled) {
							userPreferencesRepository.setInstallerType(InstallerType.SESSION)
						}
						if (isAlive && !isGranted) shizukuPermissionHandler.requestPermission()
					}
					if (installerType == InstallerType.ROOT) Shell.getShell()
				}.collect()
			}
			installer()
		}
	}

	private fun listenApplications() {
		registerReceiver(InstalledAppReceiver(packageManager), IntentFilter().apply {
			addAction(Intent.ACTION_PACKAGE_ADDED)
			addAction(Intent.ACTION_PACKAGE_REMOVED)
			addDataScheme("package")
		})
		val installedItems =
			packageManager.getInstalledPackagesCompat()?.map { it.toInstalledItem() }
		installedItems?.let { Database.InstalledAdapter.putAll(it) }
	}

	private fun updatePreference() {
		appScope.launch {
			launch {
				userPreferenceFlow.getProperty { unstableUpdate }.drop(1).collect {
					forceSyncAll()
				}
			}
			launch {
				userPreferenceFlow.getProperty { autoSync }.collectIndexed { index, syncMode ->
					// Don't update sync job on initial collect
					updateSyncJob(index > 0, syncMode)
				}
			}
			launch {
				userPreferenceFlow.getProperty { cleanUpInterval }.collect {
					when (it) {
						INFINITE -> CleanUpWorker.removeAllSchedules(applicationContext)
						ZERO -> CleanUpWorker.force(applicationContext)
						else -> CleanUpWorker.scheduleCleanup(applicationContext, it)
					}
				}
			}
			launch {
				userPreferenceFlow
					.getProperty { proxy }
					.collect(::updateProxy)
			}
		}
	}

	private fun updateProxy(proxyPreference: ProxyPreference) {
		val type = proxyPreference.type
		val host = proxyPreference.host
		val port = proxyPreference.port
		val socketAddress = when (type) {
			ProxyType.DIRECT -> null
			ProxyType.HTTP, ProxyType.SOCKS -> {
				try {
					InetSocketAddress.createUnresolved(host, port)
				} catch (e: Exception) {
					e.printStackTrace()
					null
				}
			}
		}
		val androidProxyType = when (type) {
			ProxyType.DIRECT -> Proxy.Type.DIRECT
			ProxyType.HTTP -> Proxy.Type.HTTP
			ProxyType.SOCKS -> Proxy.Type.SOCKS
		}
		val determinedProxy = socketAddress?.let { Proxy(androidProxyType, it) } ?: Proxy.NO_PROXY
		downloader.setProxy(determinedProxy)
	}

	private fun updateSyncJob(force: Boolean, autoSync: AutoSync) {
		val jobScheduler = getSystemService(JOB_SCHEDULER_SERVICE) as JobScheduler
		val syncConditions = when (autoSync) {
			AutoSync.ALWAYS -> SyncPreference(NetworkType.CONNECTED)
			AutoSync.WIFI_ONLY -> SyncPreference(NetworkType.UNMETERED)
			AutoSync.WIFI_PLUGGED_IN -> SyncPreference(NetworkType.UNMETERED, pluggedIn = true)
			AutoSync.NEVER -> SyncPreference(NetworkType.NOT_REQUIRED, canSync = false)
		}
		val reschedule =
			force || !jobScheduler.allPendingJobs.any { it.id == Constants.JOB_ID_SYNC }
		if (reschedule) {
			when (autoSync) {
				AutoSync.NEVER -> jobScheduler.cancel(Constants.JOB_ID_SYNC)
				else -> {
					val period = 12.hours.inWholeMilliseconds
					jobScheduler.schedule(
						JobInfo.Builder(
							Constants.JOB_ID_SYNC,
							ComponentName(this, SyncService.Job::class.java)
						).setRequiredNetworkType(syncConditions.toJobNetworkType()).apply {
							sdkAbove(sdk = Build.VERSION_CODES.O) {
								setRequiresCharging(syncConditions.pluggedIn)
								setRequiresBatteryNotLow(syncConditions.batteryNotLow)
								setRequiresStorageNotLow(true)
							}
							if (SdkCheck.isNougat) setPeriodic(period, JobInfo.getMinFlexMillis())
							else setPeriodic(period)
						}.build()
					)
					Unit
				}
			}::class.java
		}
	}

	private fun forceSyncAll() {
		Database.RepositoryAdapter.getAll().forEach {
			if (it.lastModified.isNotEmpty() || it.entityTag.isNotEmpty()) {
				Database.RepositoryAdapter.put(it.copy(lastModified = "", entityTag = ""))
			}
		}
		Connection(SyncService::class.java, onBind = { connection, binder ->
			binder.sync(SyncService.SyncRequest.FORCE)
			connection.unbind(this)
		}).bind(this)
	}

	class BootReceiver : BroadcastReceiver() {
		@SuppressLint("UnsafeProtectedBroadcastReceiver")
		override fun onReceive(context: Context, intent: Intent) = Unit
	}

	override fun newImageLoader(): ImageLoader {

		val memoryCache = MemoryCache.Builder(this)
			.maxSizePercent(0.25)
			.build()

		val diskCache = DiskCache.Builder()
			.directory(Cache.getImagesDir(this))
			.maxSizePercent(0.05)
			.build()

		return ImageLoader.Builder(this)
			.memoryCache(memoryCache)
			.diskCache(diskCache)
			.error(CommonR.drawable.ic_cannot_load)
			.crossfade(350)
			.build()
	}
}