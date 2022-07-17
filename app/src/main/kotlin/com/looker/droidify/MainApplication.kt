package com.looker.droidify

import android.annotation.SuppressLint
import android.app.Application
import android.app.job.JobInfo
import android.app.job.JobScheduler
import android.content.*
import coil.ImageLoader
import coil.ImageLoaderFactory
import com.looker.core_common.Common
import com.looker.droidify.content.Cache
import com.looker.droidify.content.Preferences
import com.looker.droidify.content.ProductPreferences
import com.looker.droidify.database.Database
import com.looker.droidify.index.RepositoryUpdater
import com.looker.droidify.network.CoilDownloader
import com.looker.droidify.network.Downloader
import com.looker.droidify.service.Connection
import com.looker.droidify.service.SyncService
import com.looker.droidify.utility.Utils.setLanguage
import com.looker.droidify.utility.Utils.toInstalledItem
import com.looker.droidify.utility.extension.android.Android
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.net.InetSocketAddress
import java.net.Proxy

class MainApplication : Application(), ImageLoaderFactory {

	override fun onCreate() {
		super.onCreate()

		val databaseUpdated = Database.init(this)
		Preferences.init(this)
		ProductPreferences.init(this)
		RepositoryUpdater.init()
		listenApplications()
		listenPreferences()

		if (databaseUpdated) {
			forceSyncAll()
		}

		Cache.cleanup(this)
		updateSyncJob(false)
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
								packageManager.getPackageInfo(
									packageName,
									Android.PackageManager.signaturesFlag
								)
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
		val installedItems =
			packageManager.getInstalledPackages(Android.PackageManager.signaturesFlag)
				.map { it.toInstalledItem() }
		Database.InstalledAdapter.putAll(installedItems)
	}

	private fun listenPreferences() {
		updateProxy()
		var lastAutoSync = Preferences[Preferences.Key.AutoSync]
		var lastUpdateUnstable = Preferences[Preferences.Key.UpdateUnstable]
		var lastLanguage = Preferences[Preferences.Key.Language]
		CoroutineScope(Dispatchers.Default).launch {
			Preferences.subject.collect {
				if (it == Preferences.Key.ProxyType || it == Preferences.Key.ProxyHost || it == Preferences.Key.ProxyPort) {
					updateProxy()
				} else if (it == Preferences.Key.AutoSync) {
					val autoSync = Preferences[Preferences.Key.AutoSync]
					if (lastAutoSync != autoSync) {
						lastAutoSync = autoSync
						updateSyncJob(true)
					}
				} else if (it == Preferences.Key.UpdateUnstable) {
					val updateUnstable = Preferences[Preferences.Key.UpdateUnstable]
					if (lastUpdateUnstable != updateUnstable) {
						lastUpdateUnstable = updateUnstable
						forceSyncAll()
					}
				} else if (it == Preferences.Key.Language) {
					val language = Preferences[Preferences.Key.Language]
					if (language != lastLanguage) {
						lastLanguage = language
						val refresh = Intent.makeRestartActivityTask(
							ComponentName(
								baseContext,
								MainActivity::class.java
							)
						)
						applicationContext.startActivity(refresh)
					}
				}
			}
		}
	}

	private fun updateSyncJob(force: Boolean) {
		val jobScheduler = getSystemService(JOB_SCHEDULER_SERVICE) as JobScheduler
		val reschedule = force || !jobScheduler.allPendingJobs.any { it.id == Common.JOB_ID_SYNC }
		if (reschedule) {
			val autoSync = Preferences[Preferences.Key.AutoSync]
			when (autoSync) {
				Preferences.AutoSync.Never -> {
					jobScheduler.cancel(Common.JOB_ID_SYNC)
				}
				Preferences.AutoSync.Wifi, Preferences.AutoSync.Always -> {
					val period = 12 * 60 * 60 * 1000L // 12 hours
					val wifiOnly = autoSync == Preferences.AutoSync.Wifi
					jobScheduler.schedule(JobInfo
						.Builder(
							Common.JOB_ID_SYNC,
							ComponentName(this, SyncService.Job::class.java)
						)
						.setRequiredNetworkType(if (wifiOnly) JobInfo.NETWORK_TYPE_UNMETERED else JobInfo.NETWORK_TYPE_ANY)
						.apply {
							if (Android.sdk(26)) {
								setRequiresBatteryNotLow(true)
								setRequiresStorageNotLow(true)
							}
							if (Android.sdk(24)) {
								setPeriodic(period, JobInfo.getMinFlexMillis())
							} else {
								setPeriodic(period)
							}
						}
						.build())
					Unit
				}
			}::class.java
		}
	}

	private fun updateProxy() {
		val type = Preferences[Preferences.Key.ProxyType].proxyType
		val host = Preferences[Preferences.Key.ProxyHost]
		val port = Preferences[Preferences.Key.ProxyPort]
		val socketAddress = when (type) {
			Proxy.Type.DIRECT -> {
				null
			}
			Proxy.Type.HTTP, Proxy.Type.SOCKS -> {
				try {
					InetSocketAddress.createUnresolved(host, port)
				} catch (e: Exception) {
					e.printStackTrace()
					null
				}
			}
		}
		val proxy = socketAddress?.let { Proxy(type, socketAddress) }
		Downloader.proxy = proxy
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

	override fun newImageLoader(): ImageLoader {
		return ImageLoader.Builder(this)
			.callFactory(CoilDownloader.Factory(Cache.getImagesDir(this)))
			.crossfade(true)
			.build()
	}
}

class ContextWrapperX(base: Context) : ContextWrapper(base) {
	companion object {
		fun wrap(context: Context): ContextWrapper {
			val config = context.setLanguage()
			return ContextWrapperX(context.createConfigurationContext(config))
		}
	}
}