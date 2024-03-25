package com.looker.droidify

import android.annotation.SuppressLint
import android.app.Application
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.appcompat.app.AppCompatDelegate
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import androidx.work.NetworkType
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.disk.DiskCache
import coil.memory.MemoryCache
import com.looker.core.common.Constants
import com.looker.core.common.cache.Cache
import com.looker.core.common.extension.getInstalledPackagesCompat
import com.looker.core.common.extension.jobScheduler
import com.looker.core.common.log
import com.looker.core.datastore.SettingsRepository
import com.looker.core.datastore.get
import com.looker.core.datastore.model.AutoSync
import com.looker.core.datastore.model.InstallerType
import com.looker.core.datastore.model.ProxyPreference
import com.looker.core.datastore.model.ProxyType
import com.looker.droidify.content.ProductPreferences
import com.looker.droidify.database.Database
import com.looker.droidify.index.RepositoryUpdater
import com.looker.droidify.receivers.InstalledAppReceiver
import com.looker.droidify.service.Connection
import com.looker.droidify.service.SyncService
import com.looker.droidify.sync.SyncPreference
import com.looker.droidify.sync.toJobNetworkType
import com.looker.droidify.utility.extension.toInstalledItem
import com.looker.droidify.work.CleanUpWorker
import com.looker.installer.InstallManager
import com.looker.installer.installers.root.RootPermissionHandler
import com.looker.installer.installers.shizuku.ShizukuPermissionHandler
import com.looker.network.Downloader
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collectIndexed
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch
import java.net.InetSocketAddress
import java.net.Proxy
import javax.inject.Inject
import kotlin.time.Duration.Companion.INFINITE
import kotlin.time.Duration.Companion.hours
import com.looker.core.common.R as CommonR

@HiltAndroidApp
class MainApplication : Application(), ImageLoaderFactory, Configuration.Provider {

    private val parentJob = SupervisorJob()
    private val appScope = CoroutineScope(Dispatchers.Default + parentJob)

    @Inject
    lateinit var settingsRepository: SettingsRepository

    @Inject
    lateinit var installer: InstallManager

    @Inject
    lateinit var downloader: Downloader

    @Inject
    lateinit var shizukuPermissionHandler: ShizukuPermissionHandler

    @Inject
    lateinit var rootPermissionHandler: RootPermissionHandler

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    override fun onCreate() {
        super.onCreate()

        val databaseUpdated = Database.init(this)
        ProductPreferences.init(this, appScope)
        RepositoryUpdater.init(appScope, downloader)
        listenApplications()
        checkLanguage()
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
                settingsRepository.get { installerType }.collect {
                    if (it == InstallerType.SHIZUKU) handleShizukuInstaller()
                    if (it == InstallerType.ROOT) {
                        if (!rootPermissionHandler.isGranted) {
                            settingsRepository.setInstallerType(InstallerType.Default)
                        }
                    }
                }
            }
            installer()
        }
    }

    private fun CoroutineScope.handleShizukuInstaller() = launch {
        shizukuPermissionHandler.state.collect { (isGranted, isAlive, _) ->
            if (isAlive && isGranted) {
                settingsRepository.setInstallerType(InstallerType.SHIZUKU)
                return@collect
            }
            if (isAlive) {
                settingsRepository.setInstallerType(InstallerType.Default)
                shizukuPermissionHandler.requestPermission()
                return@collect
            }
            settingsRepository.setInstallerType(InstallerType.Default)
        }
    }

    private fun listenApplications() {
        registerReceiver(
            InstalledAppReceiver(packageManager),
            IntentFilter().apply {
                addAction(Intent.ACTION_PACKAGE_ADDED)
                addAction(Intent.ACTION_PACKAGE_REMOVED)
                addDataScheme("package")
            }
        )
        val installedItems =
            packageManager.getInstalledPackagesCompat()
                ?.map { it.toInstalledItem() }
                ?: return
        Database.InstalledAdapter.putAll(installedItems)
    }

    private fun checkLanguage() {
        appScope.launch {
            val lastSetLanguage = settingsRepository.getInitial().language
            val systemSetLanguage = AppCompatDelegate.getApplicationLocales().toLanguageTags()
            if (systemSetLanguage != lastSetLanguage && lastSetLanguage != "system") {
                settingsRepository.setLanguage(systemSetLanguage)
            }
        }
    }

    private fun updatePreference() {
        appScope.launch {
            launch {
                settingsRepository.get { unstableUpdate }.drop(1).collect {
                    forceSyncAll()
                }
            }
            launch {
                settingsRepository.get { autoSync }.collectIndexed { index, syncMode ->
                    // Don't update sync job on initial collect
                    updateSyncJob(index > 0, syncMode)
                }
            }
            launch {
                settingsRepository.get { cleanUpInterval }.drop(1).collect {
                    if (it == INFINITE) {
                        CleanUpWorker.removeAllSchedules(applicationContext)
                    } else {
                        CleanUpWorker.scheduleCleanup(applicationContext, it)
                    }
                }
            }
            launch {
                settingsRepository.get { proxy }.collect(::updateProxy)
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
                } catch (e: IllegalArgumentException) {
                    log(e)
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
        if (autoSync == AutoSync.NEVER) {
            jobScheduler?.cancel(Constants.JOB_ID_SYNC)
            return
        }
        val jobScheduler = jobScheduler
        val syncConditions = when (autoSync) {
            AutoSync.ALWAYS -> SyncPreference(NetworkType.CONNECTED)
            AutoSync.WIFI_ONLY -> SyncPreference(NetworkType.UNMETERED)
            AutoSync.WIFI_PLUGGED_IN -> SyncPreference(NetworkType.UNMETERED, pluggedIn = true)
            else -> null
        }
        val isCompleted = jobScheduler?.allPendingJobs
            ?.any { it.id == Constants.JOB_ID_SYNC } == false
        if ((force || isCompleted) && syncConditions != null) {
            val period = 12.hours.inWholeMilliseconds
            val job = SyncService.Job.create(
                context = this,
                periodMillis = period,
                networkType = syncConditions.toJobNetworkType(),
                isCharging = syncConditions.pluggedIn,
                isBatteryLow = syncConditions.batteryNotLow
            )
            jobScheduler?.schedule(job)
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

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()
}
