package com.looker.droidify

import android.annotation.SuppressLint
import android.app.Application
import android.app.job.JobInfo
import android.app.job.JobScheduler
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import androidx.work.NetworkType
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.disk.DiskCache
import coil.memory.MemoryCache
import com.looker.core.common.Constants
import com.looker.core.common.Util
import com.looker.core.common.cache.Cache
import com.looker.core.common.sdkAbove
import com.looker.core.datastore.UserPreferences
import com.looker.core.datastore.UserPreferencesRepository
import com.looker.core.datastore.model.AutoSync
import com.looker.core.datastore.model.ProxyType
import com.looker.droidify.content.ProductPreferences
import com.looker.droidify.database.Database
import com.looker.droidify.index.RepositoryUpdater
import com.looker.droidify.network.Downloader
import com.looker.droidify.service.Connection
import com.looker.droidify.service.SyncService
import com.looker.droidify.utility.Utils.toInstalledItem
import com.looker.droidify.utility.extension.android.Android
import com.looker.droidify.utility.extension.toJobNetworkType
import com.looker.droidify.work.AutoSyncWorker.SyncConditions
import com.looker.droidify.work.CleanUpWorker
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import java.net.InetSocketAddress
import java.net.Proxy
import javax.inject.Inject
import kotlin.time.Duration.Companion.hours

@HiltAndroidApp
class MainApplication : Application(), ImageLoaderFactory {

	private val appScope = CoroutineScope(Dispatchers.IO)

	@Inject
	lateinit var userPreferenceRepository: UserPreferencesRepository

	private val userPreferenceFlow get() = userPreferenceRepository.userPreferencesFlow
	private val initialSetup
		get() = flow { emit(userPreferenceRepository.fetchInitialPreferences()) }

	override fun onCreate() {
		super.onCreate()

		val databaseUpdated = Database.init(this)
		ProductPreferences.init(this)
		RepositoryUpdater.init()
		listenApplications()
		updatePreference()

		if (databaseUpdated) forceSyncAll()
	}

	override fun onTerminate() {
		super.onTerminate()
		appScope.cancel("Application Terminated")
	}

	private fun listenApplications() {
		registerReceiver(object : BroadcastReceiver() {
			override fun onReceive(context: Context, intent: Intent) {
				val packageName =
					intent.data?.let { if (it.scheme == "package") it.schemeSpecificPart else null }
				if (packageName != null) {
					when (intent.action.orEmpty()) {
						Intent.ACTION_PACKAGE_ADDED,
						Intent.ACTION_PACKAGE_REMOVED,
						-> {
							val packageInfo = try {
								if (Util.isTiramisu) {
									packageManager.getPackageInfo(
										packageName,
										PackageManager.PackageInfoFlags.of(Android.PackageManager.signaturesFlag.toLong())
									)
								} else {
									@Suppress("DEPRECATION")
									packageManager.getPackageInfo(
										packageName, Android.PackageManager.signaturesFlag
									)
								}
							} catch (e: Exception) {
								null
							}
							if (packageInfo != null) {
								Database.InstalledAdapter.put(packageInfo.toInstalledItem())
							} else {
								Database.InstalledAdapter.delete(packageName)
							}
						}
					}
				}
			}
		}, IntentFilter().apply {
			addAction(Intent.ACTION_PACKAGE_ADDED)
			addAction(Intent.ACTION_PACKAGE_REMOVED)
			addDataScheme("package")
		})
		val installedItems = try {
			if (Util.isTiramisu) {
				packageManager.getInstalledPackages(
					PackageManager.PackageInfoFlags.of(Android.PackageManager.signaturesFlag.toLong())
				).map { it.toInstalledItem() }
			} else {
				@Suppress("DEPRECATION")
				packageManager.getInstalledPackages(Android.PackageManager.signaturesFlag)
					.map { it.toInstalledItem() }
			}
		} catch (e: Exception) {
			null
		}
		installedItems?.let { Database.InstalledAdapter.putAll(it) }
	}

	private fun updatePreference() {
		appScope.launch {
			initialSetup.collect { initialPreference ->
				var lastAutoSync = initialPreference.autoSync
				var lastCleanupDuration = initialPreference.cleanUpDuration
				var lastProxy = initialPreference.proxyType
				var lastProxyHost = initialPreference.proxyHost
				var lastProxyPort = initialPreference.proxyPort
				var lastUnstableUpdate = initialPreference.unstableUpdate
				updateSyncJob(false, lastAutoSync)
				updateProxy(initialPreference)
				CleanUpWorker.scheduleCleanup(applicationContext, lastCleanupDuration)
				userPreferenceFlow.collect { newPreference ->
					if (newPreference.proxyType != lastProxy || newPreference.proxyPort != lastProxyPort || newPreference.proxyHost != lastProxyHost) {
						lastProxy = newPreference.proxyType
						lastProxyPort = newPreference.proxyPort
						lastProxyHost = newPreference.proxyHost
						updateProxy(newPreference)
					} else if (lastUnstableUpdate != newPreference.unstableUpdate) {
						lastUnstableUpdate = newPreference.unstableUpdate
						forceSyncAll()
					} else if (lastAutoSync != newPreference.autoSync) {
						lastAutoSync = newPreference.autoSync
						updateSyncJob(true, lastAutoSync)
					} else if (newPreference.cleanUpDuration != lastCleanupDuration) {
						lastCleanupDuration = newPreference.cleanUpDuration
						CleanUpWorker.scheduleCleanup(
							applicationContext, newPreference.cleanUpDuration
						)
					}
				}
			}
		}
	}

	private fun updateSyncJob(force: Boolean, autoSync: AutoSync) {
		val jobScheduler = getSystemService(JOB_SCHEDULER_SERVICE) as JobScheduler
		val syncConditions = when (autoSync) {
			AutoSync.ALWAYS -> SyncConditions(networkType = NetworkType.CONNECTED)
			AutoSync.WIFI_ONLY -> SyncConditions(networkType = NetworkType.UNMETERED)
			AutoSync.WIFI_PLUGGED_IN -> SyncConditions(
				networkType = NetworkType.UNMETERED, pluggedIn = true
			)
			AutoSync.NEVER -> SyncConditions(
				networkType = NetworkType.NOT_REQUIRED, canSync = false
			)
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
							if (Util.isNougat) setPeriodic(period, JobInfo.getMinFlexMillis())
							else setPeriodic(period)
						}.build()
					)
					Unit
				}
			}::class.java
		}
	}

	private fun updateProxy(userPreferences: UserPreferences) {
		val type = userPreferences.proxyType
		val host = userPreferences.proxyHost
		val port = userPreferences.proxyPort
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
		val determinedProxy = socketAddress?.let { Proxy(androidProxyType, it) }
		Downloader.proxy = determinedProxy
	}

	private fun forceSyncAll() {
		Database.RepositoryAdapter.getAll(null).forEach {
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

	override fun newImageLoader(): ImageLoader = ImageLoader.Builder(this).memoryCache {
		MemoryCache.Builder(this).maxSizePercent(0.25).build()
	}.diskCache {
		DiskCache.Builder().directory(Cache.getImagesDir(this)).maxSizePercent(0.05).build()
	}.crossfade(350).build()

}