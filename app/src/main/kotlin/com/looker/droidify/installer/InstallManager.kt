package com.looker.droidify.installer

import android.content.Context
import android.util.Log
import com.looker.droidify.data.model.PackageName
import com.looker.droidify.database.Database
import com.looker.droidify.datastore.SettingsRepository
import com.looker.droidify.datastore.get
import com.looker.droidify.datastore.model.InstallerType
import com.looker.droidify.installer.installers.Installer
import com.looker.droidify.installer.installers.LegacyInstaller
import com.looker.droidify.installer.installers.root.RootInstaller
import com.looker.droidify.installer.installers.session.SessionInstaller
import com.looker.droidify.installer.installers.shizuku.ShizukuInstaller
import com.looker.droidify.installer.installers.dhizuku.DhizukuInstaller
import com.looker.droidify.installer.model.InstallItem
import com.looker.droidify.installer.model.InstallState
import com.looker.droidify.service.SyncService
import com.looker.droidify.utility.common.Constants
import com.looker.droidify.utility.common.cache.Cache
import com.looker.droidify.utility.common.extension.getPackageInfoCompat
import com.looker.droidify.utility.common.extension.notificationManager
import com.looker.droidify.utility.common.extension.updateAsMutable
import com.looker.droidify.utility.common.log
import com.looker.droidify.utility.extension.toInstalledItem
import com.looker.droidify.utility.notifications.createInstallNotification
import com.looker.droidify.utility.notifications.installNotification
import com.looker.droidify.utility.notifications.removeInstallNotification
import com.looker.droidify.utility.notifications.updatesAvailableNotification
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.ConcurrentHashMap

class InstallManager(
    private val context: Context,
    private val settingsRepository: SettingsRepository,
) {

    private val installItems = Channel<InstallItem>(Channel.UNLIMITED)
    private val uninstallItems = Channel<PackageName>(Channel.UNLIMITED)
    private val installCompletions = ConcurrentHashMap<String, CompletableDeferred<InstallState>>()

    val state = MutableStateFlow<Map<PackageName, InstallState>>(emptyMap())

    private var _installer: Installer? = null
        set(value) {
            field?.close()
            field = value
        }
    private val installer: Installer get() = _installer!!

    private val lock = Mutex()
    private val skipSignature = settingsRepository.get { ignoreSignature }
    private val installerPreference = settingsRepository.get { installerType }
    private val deleteApkPreference = settingsRepository.get { deleteApkOnInstall }
    private val notificationManager by lazy { context.notificationManager }

    suspend operator fun invoke() = coroutineScope {
        setupInstaller()
        installer()
        uninstaller()
    }

    fun close() {
        _installer = null
        uninstallItems.close()
        installItems.close()
    }

    suspend infix fun install(installItem: InstallItem) {
        installItems.send(installItem)
    }

    suspend fun installAndAwait(installItem: InstallItem): InstallState {
        val key = installItem.packageName.name
        installCompletions[key]?.let { inFlight ->
            log("Joining in-flight install: $key", TAG)
            return inFlight.await()
        }
        val deferred = CompletableDeferred<InstallState>()
        installCompletions[key] = deferred
        installItems.send(installItem)
        return deferred.await()
    }

    suspend infix fun uninstall(packageName: PackageName) {
        uninstallItems.send(packageName)
    }

    infix fun remove(packageName: PackageName) {
        updateState { remove(packageName) }
    }

    infix fun setFailed(packageName: PackageName) {
        updateState { put(packageName, InstallState.Failed) }
    }

    private fun CoroutineScope.setupInstaller() = launch {
        installerPreference.collectLatest(::setInstaller)
    }

    private fun CoroutineScope.installer() = launch {
        installItems.consumeEach { item ->
            val key = item.packageName.name
            log("Install started: $key", TAG)
            val result = try {
                updateState { put(item.packageName, InstallState.Installing) }
                notificationManager?.installNotification(
                    packageName = key,
                    notification = context.createInstallNotification(
                        appName = key,
                        state = InstallState.Installing,
                    ),
                )
                try {
                    installer.use { it.install(item) }
                } catch (e: Exception) {
                    log("Install failed: $key — ${e.message}", TAG, Log.ERROR)
                    InstallState.Failed
                }.also { installResult ->
                    notificationManager?.removeInstallNotification(key)
                    if (installResult == InstallState.Installed) {
                        updateState { remove(item.packageName) }
                        log("Install succeeded: $key", TAG)
                        if (installer !is LegacyInstaller) {
                            refreshInstalledPackage(key)
                            if (deleteApkPreference.first() && !SyncService.autoUpdating) {
                                Cache.getReleaseFile(context, item.installFileName).delete()
                            }
                        }
                        if (SyncService.autoUpdating) {
                            val updates = Database.ProductAdapter.getUpdates(skipSignature.first())
                            when {
                                updates.isEmpty() -> {
                                    SyncService.autoUpdating = false
                                    notificationManager?.cancel(Constants.NOTIFICATION_ID_UPDATES)
                                    log("Update-all batch complete", TAG)
                                }
                                updates.map { it.packageName } != SyncService.autoUpdateStartedFor -> {
                                    notificationManager?.notify(
                                        Constants.NOTIFICATION_ID_UPDATES,
                                        updatesAvailableNotification(context, updates),
                                    )
                                }
                            }
                        }
                    } else {
                        updateState { put(item.packageName, installResult) }
                        log("Install finished with $installResult: $key", TAG, Log.WARN)
                    }
                }
            } catch (e: Exception) {
                log("Install queue error: $key — ${e.message}", TAG, Log.ERROR)
                InstallState.Failed
            }
            installCompletions.remove(key)?.complete(result)
        }
    }

    private fun CoroutineScope.uninstaller() = launch {
        uninstallItems.consumeEach { packageName ->
            val key = packageName.name
            log("Uninstall queued: $key", TAG)
            updateState { put(packageName, InstallState.Uninstalling) }
            try {
                log("Uninstall started: $key", TAG)
                notificationManager?.installNotification(
                    packageName = key,
                    notification = context.createInstallNotification(
                        appName = key,
                        state = InstallState.Uninstalling,
                    ),
                )
                try {
                    installer.uninstall(packageName)
                } catch (e: Exception) {
                    log("Uninstall failed: $key — ${e.message}", TAG, Log.ERROR)
                    throw e
                }
                notificationManager?.removeInstallNotification(key)
                updateState { remove(packageName) }
                refreshUninstalledPackage(key)
                log("Uninstall succeeded: $key", TAG)
            } catch (e: Exception) {
                log("Uninstall error (cleanup): $key — ${e.message}", TAG, Log.ERROR)
                notificationManager?.removeInstallNotification(key)
                updateState { remove(packageName) }
            }
        }
    }

    private suspend fun setInstaller(installerType: InstallerType) {
        lock.withLock {
            _installer = when (installerType) {
                InstallerType.LEGACY -> LegacyInstaller(context, settingsRepository)
                InstallerType.SESSION -> SessionInstaller(context)
                InstallerType.SHIZUKU -> ShizukuInstaller(context)
                InstallerType.DHIZUKU -> DhizukuInstaller(context)
                InstallerType.ROOT -> RootInstaller(context)
            }
        }
    }

    private inline fun updateState(block: MutableMap<PackageName, InstallState>.() -> Unit) {
        state.update { it.updateAsMutable(block) }
    }

    /**
     * Silent installers (Session/Shizuku/Dhizuku) may not deliver [Intent.ACTION_PACKAGE_ADDED]
     * to our dynamic receiver when install runs in another UID. Refresh the local DB explicitly.
     */
    private suspend fun refreshInstalledPackage(packageName: String) {
        repeat(REFRESH_INSTALLED_ATTEMPTS) { attempt ->
            val packageInfo = context.packageManager.getPackageInfoCompat(packageName)
            if (packageInfo != null) {
                Database.InstalledAdapter.put(packageInfo.toInstalledItem())
                return
            }
            if (attempt < REFRESH_INSTALLED_ATTEMPTS - 1) {
                delay(REFRESH_INSTALLED_DELAY_MS)
            }
        }
    }

    private suspend fun refreshUninstalledPackage(packageName: String) {
        repeat(REFRESH_INSTALLED_ATTEMPTS) { attempt ->
            if (context.packageManager.getPackageInfoCompat(packageName) == null) {
                Database.InstalledAdapter.delete(packageName)
                return
            }
            if (attempt < REFRESH_INSTALLED_ATTEMPTS - 1) {
                delay(REFRESH_INSTALLED_DELAY_MS)
            }
        }
    }

    companion object {
        private const val TAG = "DroidifyUpdateAll"
        private const val REFRESH_INSTALLED_ATTEMPTS = 20
        private const val REFRESH_INSTALLED_DELAY_MS = 250L
    }
}
