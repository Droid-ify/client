package com.looker.droidify

import android.annotation.SuppressLint
import android.app.Application
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.StrictMode
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatDelegate
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import androidx.work.NetworkType
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.SingletonImageLoader
import coil3.asImage
import coil3.disk.DiskCache
import coil3.disk.directory
import coil3.memory.MemoryCache
import coil3.request.crossfade
import com.looker.droidify.content.ProductPreferences
import com.looker.droidify.data.InstalledRepository
import com.looker.droidify.database.Database
import com.looker.droidify.datastore.SettingsRepository
import com.looker.droidify.datastore.get
import com.looker.droidify.datastore.model.AutoSync
import com.looker.droidify.datastore.model.ProxyPreference
import com.looker.droidify.datastore.model.ProxyType
import com.looker.droidify.index.RepositoryUpdater
import com.looker.droidify.installer.InstallManager
import com.looker.droidify.network.Downloader
import com.looker.droidify.receivers.InstalledAppReceiver
import com.looker.droidify.service.Connection
import com.looker.droidify.service.SyncService
import com.looker.droidify.sync.SyncPreference
import com.looker.droidify.sync.toJobNetworkType
import com.looker.droidify.utility.common.Constants
import com.looker.droidify.utility.common.cache.Cache
import com.looker.droidify.utility.common.extension.getDrawableCompat
import com.looker.droidify.utility.common.extension.getInstalledPackagesCompat
import com.looker.droidify.utility.common.extension.jobScheduler
import com.looker.droidify.utility.common.log
import com.looker.droidify.utility.extension.toInstalledItem
import com.looker.droidify.work.CleanUpWorker
import dagger.hilt.android.HiltAndroidApp
import java.net.InetSocketAddress
import java.net.Proxy
import javax.inject.Inject
import kotlin.time.Duration.Companion.INFINITE
import kotlin.time.Duration.Companion.hours
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collectIndexed
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch

@HiltAndroidApp
class Droidify : Application(), SingletonImageLoader.Factory, Configuration.Provider {

    private val parentJob = SupervisorJob()
    private val appScope = CoroutineScope(Dispatchers.Default + parentJob)

    @Inject
    lateinit var settingsRepository: SettingsRepository

    @Inject
    lateinit var installer: InstallManager

    @Inject
    lateinit var downloader: Downloader

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    @Inject
    lateinit var installedRepository: InstalledRepository

    override fun onCreate() {
        super.onCreate()

//        if (BuildConfig.DEBUG && SdkCheck.isOreo) strictThreadPolicy()

        val databaseUpdated = Database.init(this)
        ProductPreferences.init(this, appScope)
        RepositoryUpdater.init(appScope, downloader)
        listenApplications()
        checkLanguage()
        updatePreference()
        appScope.launch { installer() }

        if (databaseUpdated) forceSyncAll()
    }

    override fun onTerminate() {
        super.onTerminate()
        appScope.cancel("Application Terminated")
        installer.close()
    }

    private fun listenApplications() {
        appScope.launch(Dispatchers.Default) {
            registerReceiver(
                InstalledAppReceiver(packageManager, installedRepository),
                IntentFilter().apply {
                    addAction(Intent.ACTION_PACKAGE_ADDED)
                    addAction(Intent.ACTION_PACKAGE_REMOVED)
                    addDataScheme("package")
                },
            )
            val installedItems =
                packageManager.getInstalledPackagesCompat()
                    ?.map { it.toInstalledItem() }
                    ?: return@launch
            installedRepository.putAll(installedItems)
        }
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
                isBatteryLow = syncConditions.batteryNotLow,
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
        Connection(
            SyncService::class.java,
            onBind = { connection, binder ->
                binder.sync(SyncService.SyncRequest.FORCE)
                connection.unbind(this)
            },
        ).bind(this)
    }

    class BootReceiver : BroadcastReceiver() {
        @SuppressLint("UnsafeProtectedBroadcastReceiver")
        override fun onReceive(context: Context, intent: Intent) = Unit
    }

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun newImageLoader(context: PlatformContext): ImageLoader {
        val memoryCache = MemoryCache.Builder()
            .maxSizePercent(context, 0.25)
            .build()

        val diskCache = DiskCache.Builder()
            .directory(Cache.getImagesDir(this))
            .maxSizePercent(0.05)
            .build()

        return ImageLoader.Builder(this)
            .memoryCache(memoryCache)
            .diskCache(diskCache)
            .error(getDrawableCompat(R.drawable.ic_cannot_load).asImage())
            .crossfade(350)
            .build()
    }
}

@RequiresApi(Build.VERSION_CODES.O)
fun strictThreadPolicy() {
    StrictMode.setThreadPolicy(
        StrictMode.ThreadPolicy.Builder()
            .detectDiskReads()
            .detectDiskWrites()
            .detectNetwork()
            .detectUnbufferedIo()
            .penaltyLog()
            .build(),
    )
    StrictMode.setVmPolicy(
        StrictMode.VmPolicy.Builder()
            .detectAll()
            .penaltyLog()
            .build(),
    )
}
