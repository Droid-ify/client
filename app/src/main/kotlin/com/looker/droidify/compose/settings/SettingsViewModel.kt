package com.looker.droidify.compose.settings

import android.content.Context
import android.net.Uri
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.material3.SnackbarHostState
import androidx.core.os.LocaleListCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.looker.droidify.BuildConfig
import com.looker.droidify.R
import com.looker.droidify.data.PrivacyRepository
import com.looker.droidify.data.StringHandler
import com.looker.droidify.database.Database
import com.looker.droidify.database.RepositoryExporter
import com.looker.droidify.datastore.CustomButtonRepository
import com.looker.droidify.datastore.Settings
import com.looker.droidify.datastore.SettingsRepository
import com.looker.droidify.datastore.model.AutoSync
import com.looker.droidify.datastore.model.CustomButton
import com.looker.droidify.datastore.model.InstallerType
import com.looker.droidify.datastore.model.LegacyInstallerComponent
import com.looker.droidify.datastore.model.ProxyType
import com.looker.droidify.datastore.model.Theme
import com.looker.droidify.installer.installers.initSui
import com.looker.droidify.installer.installers.isMagiskGranted
import com.looker.droidify.installer.installers.isShizukuAlive
import com.looker.droidify.installer.installers.isShizukuGranted
import com.looker.droidify.installer.installers.isShizukuInstalled
import com.looker.droidify.installer.installers.requestPermissionListener
import com.looker.droidify.utility.common.extension.asStateFlow
import com.looker.droidify.utility.common.extension.updateAsMutable
import com.looker.droidify.work.CleanUpWorker
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.*
import javax.inject.Inject
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val privacyRepository: PrivacyRepository,
    private val repositoryExporter: RepositoryExporter,
    private val customButtonRepository: CustomButtonRepository,
    private val handler: StringHandler,
) : ViewModel() {

    val snackbarHostState = SnackbarHostState()

    val settings = settingsRepository.data.asStateFlow(Settings())

    val customButtons: StateFlow<List<CustomButton>> = customButtonRepository.buttons
        .asStateFlow(emptyList())

    private val _isBackgroundAllowed = MutableStateFlow(true)
    val isBackgroundAllowed = _isBackgroundAllowed.asStateFlow()

    fun updateBackgroundAccessState(allowed: Boolean) {
        _isBackgroundAllowed.value = allowed
    }

    fun showSnackbar(@StringRes messageRes: Int) {
        viewModelScope.launch {
            snackbarHostState.showSnackbar(handler.getString(messageRes))
        }
    }

    fun setLanguage(language: String) {
        viewModelScope.launch {
            val appLocale = LocaleListCompat.create(language.toLocale())
            AppCompatDelegate.setApplicationLocales(appLocale)
            settingsRepository.setLanguage(language)
        }
    }

    fun setTheme(theme: Theme) {
        viewModelScope.launch {
            settingsRepository.setTheme(theme)
        }
    }

    fun setDynamicTheme(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setDynamicTheme(enabled)
        }
    }

    fun setHomeScreenSwiping(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setHomeScreenSwiping(enabled)
        }
    }

    fun setAutoUpdate(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setAutoUpdate(enabled)
        }
    }

    fun setNotifyUpdates(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.enableNotifyUpdates(enabled)
        }
    }

    fun setUnstableUpdates(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.enableUnstableUpdates(enabled)
        }
    }

    fun setIgnoreSignature(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setIgnoreSignature(enabled)
        }
    }

    fun setIncompatibleUpdates(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.enableIncompatibleVersion(enabled)
        }
    }

    fun setAutoSync(autoSync: AutoSync) {
        viewModelScope.launch {
            settingsRepository.setAutoSync(autoSync)
        }
    }

    fun setCleanUpInterval(interval: Duration) {
        viewModelScope.launch {
            settingsRepository.setCleanUpInterval(interval)
        }
    }

    fun forceCleanup(context: Context) {
        viewModelScope.launch {
            CleanUpWorker.force(context)
        }
    }

    fun setInstaller(context: Context, installerType: InstallerType) {
        viewModelScope.launch {
            when (installerType) {
                InstallerType.SHIZUKU -> handleShizukuInstaller(context, installerType)
                InstallerType.ROOT -> handleRootInstaller(installerType)
                InstallerType.LEGACY -> {
                    settingsRepository.setDeleteApkOnInstall(false)
                    settingsRepository.setInstallerType(installerType)
                }
                else -> settingsRepository.setInstallerType(installerType)
            }
        }
    }

    private suspend fun handleShizukuInstaller(context: Context, installerType: InstallerType) {
        if (isShizukuInstalled(context) || initSui(context)) {
            when {
                !isShizukuAlive() -> showSnackbar(R.string.shizuku_not_alive)
                isShizukuGranted() -> settingsRepository.setInstallerType(installerType)
                else -> {
                    if (requestPermissionListener()) {
                        settingsRepository.setInstallerType(installerType)
                    }
                }
            }
        } else {
            showSnackbar(R.string.shizuku_not_installed)
        }
    }

    private suspend fun handleRootInstaller(installerType: InstallerType) {
        if (isMagiskGranted()) {
            settingsRepository.setInstallerType(installerType)
        }
    }

    fun setLegacyInstallerComponent(component: LegacyInstallerComponent?) {
        viewModelScope.launch {
            settingsRepository.setLegacyInstallerComponent(component)
        }
    }

    fun setDeleteApkOnInstall(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setDeleteApkOnInstall(enabled)
        }
    }

    fun setInstallForAllUsers(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setInstallForAllUsers(enabled)
        }
    }

    fun setDownloadStatisticsEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setDownloadStatisticsEnabled(enabled)
            if (!enabled) {
                privacyRepository.clearDownloadStats()
            }
        }
    }

    fun setReproducibilityLogsEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setRBLogsEnabled(enabled)
            if (!enabled) {
                privacyRepository.clearRbLogs()
            }
        }
    }

    fun setProxyType(proxyType: ProxyType) {
        viewModelScope.launch {
            settingsRepository.setProxyType(proxyType)
            showSnackbar(R.string.proxy_restart_required)
        }
    }

    fun setProxyHost(host: String) {
        viewModelScope.launch {
            settingsRepository.setProxyHost(host)
            showSnackbar(R.string.proxy_restart_required)
        }
    }

    fun setProxyPort(port: String) {
        viewModelScope.launch {
            val portInt = port.toIntOrNull()
            if (portInt == null) {
                showSnackbar(R.string.proxy_port_error_not_int)
            } else {
                settingsRepository.setProxyPort(portInt)
                showSnackbar(R.string.proxy_restart_required)
            }
        }
    }

    fun exportSettings(uri: Uri) {
        viewModelScope.launch {
            settingsRepository.export(uri)
        }
    }

    fun importSettings(uri: Uri) {
        viewModelScope.launch {
            settingsRepository.import(uri)
        }
    }

    fun exportRepos(uri: Uri) {
        viewModelScope.launch {
            val repos = Database.RepositoryAdapter.getAll()
            repositoryExporter.export(repos, uri)
        }
    }

    fun importRepos(uri: Uri) {
        viewModelScope.launch {
            val repos = repositoryExporter.import(uri)
            Database.RepositoryAdapter.importRepos(repos)
        }
    }

    fun addCustomButton(button: CustomButton) {
        viewModelScope.launch {
            customButtonRepository.addButton(button)
        }
    }

    fun updateCustomButton(button: CustomButton) {
        viewModelScope.launch {
            customButtonRepository.updateButton(button)
        }
    }

    fun removeCustomButton(buttonId: String) {
        viewModelScope.launch {
            customButtonRepository.removeButton(buttonId)
        }
    }

    fun exportCustomButtons(uri: Uri) {
        viewModelScope.launch {
            customButtonRepository.exportToUri(uri).onFailure {
                showSnackbar(R.string.file_format_error_DESC)
            }
        }
    }

    fun importCustomButtons(uri: Uri) {
        viewModelScope.launch {
            customButtonRepository.importFromUri(uri).fold(
                onSuccess = { count ->
                    if (count > 0) {
                        showSnackbar(R.string.custom_buttons_imported)
                    }
                },
                onFailure = {
                    showSnackbar(R.string.file_format_error_DESC)
                }
            )
        }
    }

    companion object {
        val cleanUpIntervals: List<Duration> = listOf(
            6.hours,
            12.hours,
            18.hours,
            1.days,
            2.days,
            Duration.INFINITE,
        )

        val localeCodesList: List<String> = BuildConfig.DETECTED_LOCALES
            .toList()
            .updateAsMutable { add(0, "system") }
    }
}

private fun String.toLocale(): Locale = when {
    contains("-r") -> Locale(substring(0, 2), substring(4))
    contains("_") -> Locale(substring(0, 2), substring(3))
    else -> Locale(this)
}
