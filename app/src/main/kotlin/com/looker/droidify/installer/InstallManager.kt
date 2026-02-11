package com.looker.droidify.installer

import android.content.Context
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
import com.looker.droidify.installer.model.InstallItem
import com.looker.droidify.installer.model.InstallState
import com.looker.droidify.service.SyncService
import com.looker.droidify.utility.common.Constants
import com.looker.droidify.utility.common.cache.Cache
import com.looker.droidify.utility.common.extension.addAndCompute
import com.looker.droidify.utility.common.extension.filter
import com.looker.droidify.utility.common.extension.notificationManager
import com.looker.droidify.utility.common.extension.updateAsMutable
import com.looker.droidify.utility.notifications.createInstallNotification
import com.looker.droidify.utility.notifications.installNotification
import com.looker.droidify.utility.notifications.removeInstallNotification
import com.looker.droidify.utility.notifications.updatesAvailableNotification
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class InstallManager(
    private val context: Context,
    private val settingsRepository: SettingsRepository
) {

    private val installItems = Channel<InstallItem>()
    private val uninstallItems = Channel<PackageName>()

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
        val currentQueue = mutableSetOf<String>()
        installItems.filter { item ->
            currentQueue.addAndCompute(item.packageName.name) { isAdded ->
                if (isAdded) {
                    updateState { put(item.packageName, InstallState.Pending) }
                }
            }
        }.consumeEach { item ->
            if (state.value.containsKey(item.packageName)) {
                updateState { put(item.packageName, InstallState.Installing) }
                notificationManager?.installNotification(
                    packageName = item.packageName.name,
                    notification = context.createInstallNotification(
                        appName = item.packageName.name,
                        state = InstallState.Installing,
                    )
                )
                val result = installer.use { it.install(item) }
                if (result == InstallState.Installed) {
                    if (deleteApkPreference.first()) {
                        val apkFile = Cache.getReleaseFile(context, item.installFileName)
                        apkFile.delete()
                    }
                }
                if (result == InstallState.Installed && SyncService.autoUpdating) {
                    val updates = Database.ProductAdapter.getUpdates(skipSignature.first())
                    when {
                        updates.isEmpty() -> {
                            SyncService.autoUpdating = false
                            notificationManager?.cancel(Constants.NOTIFICATION_ID_UPDATES)
                        }
                        updates.map { it.packageName } != SyncService.autoUpdateStartedFor -> {
                            notificationManager?.notify(
                                Constants.NOTIFICATION_ID_UPDATES,
                                updatesAvailableNotification(context, updates),
                            )
                        }
                    }
                }
                notificationManager?.removeInstallNotification(item.packageName.name)
                updateState { put(item.packageName, result) }
                currentQueue.remove(item.packageName.name)
            }
        }
    }

    private fun CoroutineScope.uninstaller() = launch {
        uninstallItems.consumeEach {
            installer.uninstall(it)
        }
    }

    private suspend fun setInstaller(installerType: InstallerType) {
        lock.withLock {
            _installer = when (installerType) {
                InstallerType.LEGACY -> LegacyInstaller(context, settingsRepository)
                InstallerType.SESSION -> SessionInstaller(context)
                InstallerType.SHIZUKU -> ShizukuInstaller(context)
                InstallerType.ROOT -> RootInstaller(context)
            }
        }
    }

    private inline fun updateState(block: MutableMap<PackageName, InstallState>.() -> Unit) {
        state.update { it.updateAsMutable(block) }
    }
}
