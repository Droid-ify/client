package com.looker.droidify.ui.settings

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.looker.core.common.extension.toLocale
import com.looker.core.datastore.UserPreferencesRepository
import com.looker.core.datastore.model.*
import com.looker.droidify.work.CleanUpWorker
import com.looker.installer.installers.shizuku.ShizukuPermissionHandler
import com.topjohnwu.superuser.Shell
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.time.Duration
import com.looker.core.common.R as CommonR

@HiltViewModel
class SettingsViewModel
@Inject constructor(
	private val userPreferencesRepository: UserPreferencesRepository,
	private val shizukuPermissionHandler: ShizukuPermissionHandler
) : ViewModel() {

	val userPreferencesFlow get() = userPreferencesRepository.userPreferencesFlow

	private val _snackbarStringId = MutableSharedFlow<Int>()
	val snackbarStringId = _snackbarStringId.asSharedFlow()

	fun setLanguage(language: String) {
		viewModelScope.launch {
			val appLocale = LocaleListCompat.create(language.toLocale())
			AppCompatDelegate.setApplicationLocales(appLocale)
			userPreferencesRepository.setLanguage(language)
		}
	}

	fun setTheme(theme: Theme) {
		viewModelScope.launch {
			userPreferencesRepository.setTheme(theme)
		}
	}

	fun setDynamicTheme(enable: Boolean) {
		viewModelScope.launch {
			userPreferencesRepository.setDynamicTheme(enable)
		}
	}

	fun setCleanUpInterval(interval: Duration) {
		viewModelScope.launch {
			userPreferencesRepository.setCleanUpInterval(interval)
		}
	}

	fun forceCleanup(context: Context) {
		viewModelScope.launch {
			CleanUpWorker.force(context)
		}
	}

	fun setAutoSync(autoSync: AutoSync) {
		viewModelScope.launch {
			userPreferencesRepository.setAutoSync(autoSync)
		}
	}

	fun setNotifyUpdates(enable: Boolean) {
		viewModelScope.launch {
			userPreferencesRepository.enableNotifyUpdates(enable)
		}
	}

	fun setAutoUpdate(enable: Boolean) {
		viewModelScope.launch {
			userPreferencesRepository.setAutoUpdate(enable)
		}
	}

	fun setUnstableUpdates(enable: Boolean) {
		viewModelScope.launch {
			userPreferencesRepository.enableUnstableUpdates(enable)
		}
	}

	fun setIncompatibleUpdates(enable: Boolean) {
		viewModelScope.launch {
			userPreferencesRepository.enableIncompatibleVersion(enable)
		}
	}

	fun setProxyType(proxyType: ProxyType) {
		viewModelScope.launch {
			userPreferencesRepository.setProxyType(proxyType)
		}
	}

	fun setProxyHost(proxyHost: String) {
		viewModelScope.launch {
			userPreferencesRepository.setProxyHost(proxyHost)
		}
	}

	fun setProxyPort(proxyPort: Int) {
		viewModelScope.launch {
			userPreferencesRepository.setProxyPort(proxyPort)
		}
	}

	fun setInstaller(installerType: InstallerType) {
		viewModelScope.launch {
			when (installerType) {
				InstallerType.SHIZUKU -> {
					val isInstalled = shizukuPermissionHandler.isInstalled()
					val isAlive = shizukuPermissionHandler.isBinderAlive.first()
					when {
						!isInstalled -> _snackbarStringId.emit(CommonR.string.shizuku_not_installed)
						!isAlive -> _snackbarStringId.emit(CommonR.string.shizuku_not_alive)
						else -> userPreferencesRepository.setInstallerType(InstallerType.SHIZUKU)
					}
				}

				InstallerType.ROOT -> {
					Shell.getShell()
					val isRooted = Shell.isAppGrantedRoot()
					userPreferencesRepository.setInstallerType(
						if (isRooted == true) InstallerType.ROOT
						else InstallerType.SESSION
					)
				}

				else -> userPreferencesRepository.setInstallerType(installerType)
			}
		}
	}
}