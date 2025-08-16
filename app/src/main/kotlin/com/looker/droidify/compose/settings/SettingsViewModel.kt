package com.looker.droidify.compose.settings

import android.content.Context
import android.net.Uri
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.looker.droidify.R
import com.looker.droidify.database.Database
import com.looker.droidify.database.RepositoryExporter
import com.looker.droidify.datastore.Settings
import com.looker.droidify.datastore.SettingsRepository
import com.looker.droidify.datastore.model.AutoSync
import com.looker.droidify.datastore.model.InstallerType
import com.looker.droidify.datastore.model.InstallerType.ROOT
import com.looker.droidify.datastore.model.InstallerType.SHIZUKU
import com.looker.droidify.datastore.model.LegacyInstallerComponent
import com.looker.droidify.datastore.model.ProxyType
import com.looker.droidify.datastore.model.Theme
import com.looker.droidify.installer.installers.initSui
import com.looker.droidify.installer.installers.isMagiskGranted
import com.looker.droidify.installer.installers.isShizukuAlive
import com.looker.droidify.installer.installers.isShizukuGranted
import com.looker.droidify.installer.installers.isShizukuInstalled
import com.looker.droidify.installer.installers.requestPermissionListener
import com.looker.droidify.work.CleanUpWorker
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.*
import javax.inject.Inject
import kotlin.time.Duration
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface SettingUiState {
    data object Loading : SettingUiState
    data class Success(val settings: Settings) : SettingUiState
}

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val repositoryExporter: RepositoryExporter,
) : ViewModel() {

    val state: StateFlow<SettingUiState> = settingsRepository
        .data
        .map<Settings, SettingUiState> { SettingUiState.Success(it) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = SettingUiState.Loading,
        )

    private val _snackbarStringId = MutableSharedFlow<Int>()
    val snackbarStringId = _snackbarStringId.asSharedFlow()

    fun setLanguage(language: String) {
        viewModelScope.launch {
            val appLocale = LocaleListCompat.create(language.toLocale())
            AppCompatDelegate.setApplicationLocales(appLocale)
            settingsRepository.setLanguage(language)
        }
    }

    fun setTheme(theme: Theme) {
        viewModelScope.launch { settingsRepository.setTheme(theme) }
    }

    fun setDynamicTheme(enable: Boolean) {
        viewModelScope.launch { settingsRepository.setDynamicTheme(enable) }
    }

    fun setHomeScreenSwiping(enable: Boolean) {
        viewModelScope.launch { settingsRepository.setHomeScreenSwiping(enable) }
    }

    fun setCleanUpInterval(interval: Duration) {
        viewModelScope.launch { settingsRepository.setCleanUpInterval(interval) }
    }

    fun forceCleanup(context: Context) {
        viewModelScope.launch { CleanUpWorker.force(context) }
    }

    fun setAutoSync(autoSync: AutoSync) {
        viewModelScope.launch { settingsRepository.setAutoSync(autoSync) }
    }

    fun setNotifyUpdates(enable: Boolean) {
        viewModelScope.launch { settingsRepository.enableNotifyUpdates(enable) }
    }

    fun setAutoUpdate(enable: Boolean) {
        viewModelScope.launch { settingsRepository.setAutoUpdate(enable) }
    }

    fun setUnstableUpdates(enable: Boolean) {
        viewModelScope.launch { settingsRepository.enableUnstableUpdates(enable) }
    }

    fun setIgnoreSignature(enable: Boolean) {
        viewModelScope.launch { settingsRepository.setIgnoreSignature(enable) }
    }

    fun setIncompatibleUpdates(enable: Boolean) {
        viewModelScope.launch { settingsRepository.enableIncompatibleVersion(enable) }
    }

    fun setProxyType(proxyType: ProxyType) {
        viewModelScope.launch { settingsRepository.setProxyType(proxyType) }
    }

    fun setProxyHost(proxyHost: String) {
        viewModelScope.launch { settingsRepository.setProxyHost(proxyHost) }
    }

    fun setProxyPort(proxyPort: String) {
        viewModelScope.launch {
            try {
                settingsRepository.setProxyPort(proxyPort.toInt())
            } catch (_: NumberFormatException) {
                createSnackbar(R.string.proxy_port_error_not_int)
            }
        }
    }

    fun setInstaller(context: Context, installerType: InstallerType) {
        viewModelScope.launch {
            when (installerType) {
                SHIZUKU -> {
                    if (isShizukuInstalled(context) || initSui(context)) {
                        if (!isShizukuAlive()) {
                            createSnackbar(R.string.shizuku_not_alive)
                            return@launch
                        } else if (isShizukuGranted()) {
                            settingsRepository.setInstallerType(installerType)
                        } else if (!isShizukuGranted()) {
                            if (requestPermissionListener()) {
                                settingsRepository.setInstallerType(installerType)
                            }
                        }
                    } else {
                        createSnackbar(R.string.shizuku_not_installed)
                    }
                }
                ROOT -> {
                    if (isMagiskGranted()) {
                        settingsRepository.setInstallerType(installerType)
                    }
                }
                else -> {
                    settingsRepository.setInstallerType(installerType)
                }
            }
        }
    }

    fun setLegacyInstallerComponentComponent(component: LegacyInstallerComponent?) {
        viewModelScope.launch { settingsRepository.setLegacyInstallerComponent(component) }
    }

    fun exportSettings(file: Uri) {
        viewModelScope.launch { settingsRepository.export(file) }
    }

    fun importSettings(file: Uri) {
        viewModelScope.launch { settingsRepository.import(file) }
    }

    fun exportRepos(file: Uri) {
        viewModelScope.launch {
            val repos = Database.RepositoryAdapter.getAll()
            repositoryExporter.export(repos, file)
        }
    }

    fun importRepos(file: Uri) {
        viewModelScope.launch {
            val repos = repositoryExporter.import(file)
            Database.RepositoryAdapter.importRepos(repos)
        }
    }

    fun createSnackbar(@StringRes message: Int) {
        viewModelScope.launch { _snackbarStringId.emit(message) }
    }
}

private fun String.toLocale(): Locale = when {
    contains("-r") -> Locale(substring(0, 2), substring(4))
    contains("_") -> Locale(substring(0, 2), substring(3))
    else -> Locale(this)
}
