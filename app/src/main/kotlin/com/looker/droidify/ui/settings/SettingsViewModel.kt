package com.looker.droidify.ui.settings

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.looker.core.common.extension.toLocale
import com.looker.core.datastore.Settings
import com.looker.core.datastore.SettingsRepository
import com.looker.core.datastore.model.*
import com.looker.droidify.work.CleanUpWorker
import com.looker.installer.installers.shizuku.ShizukuPermissionHandler
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.time.Duration
import com.looker.core.common.R as CommonR

@HiltViewModel
class SettingsViewModel
@Inject constructor(
	private val settingsRepository: SettingsRepository,
	private val shizukuPermissionHandler: ShizukuPermissionHandler
) : ViewModel() {

	private val initialSetting = flow {
		emit(settingsRepository.fetchInitialPreferences())
	}
	val settingsFlow get() = settingsRepository.settingsFlow

	private val _snackbarStringId = MutableSharedFlow<Int>()
	val snackbarStringId = _snackbarStringId.asSharedFlow()

	fun <T> getSetting(block: Settings.() -> T): Flow<T> = settingsRepository.get(block)

	fun <T> getInitialSetting(block: Settings.() -> T): Flow<T> = initialSetting.map { it.block() }

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

	fun setDynamicTheme(enable: Boolean) {
		viewModelScope.launch {
			settingsRepository.setDynamicTheme(enable)
		}
	}

	fun setHomeScreenSwiping(enable: Boolean) {
		viewModelScope.launch {
			settingsRepository.setHomeScreenSwiping(enable)
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

	fun setAutoSync(autoSync: AutoSync) {
		viewModelScope.launch {
			settingsRepository.setAutoSync(autoSync)
		}
	}

	fun setNotifyUpdates(enable: Boolean) {
		viewModelScope.launch {
			settingsRepository.enableNotifyUpdates(enable)
		}
	}

	fun setAutoUpdate(enable: Boolean) {
		viewModelScope.launch {
			settingsRepository.setAutoUpdate(enable)
		}
	}

	fun setUnstableUpdates(enable: Boolean) {
		viewModelScope.launch {
			settingsRepository.enableUnstableUpdates(enable)
		}
	}

	fun setIncompatibleUpdates(enable: Boolean) {
		viewModelScope.launch {
			settingsRepository.enableIncompatibleVersion(enable)
		}
	}

	fun setProxyType(proxyType: ProxyType) {
		viewModelScope.launch {
			settingsRepository.setProxyType(proxyType)
		}
	}

	fun setProxyHost(proxyHost: String) {
		viewModelScope.launch {
			settingsRepository.setProxyHost(proxyHost)
		}
	}

	fun setProxyPort(proxyPort: String) {
		viewModelScope.launch {
			try {
				settingsRepository.setProxyPort(proxyPort.toInt())
			} catch (e: NumberFormatException) {
				_snackbarStringId.emit(CommonR.string.proxy_port_error_not_int)
			}
		}
	}

	fun setInstaller(installerType: InstallerType) {
		viewModelScope.launch {
			settingsRepository.setInstallerType(installerType)
			if (installerType == InstallerType.SHIZUKU) handleShizuku()
		}
	}

	private fun handleShizuku() {
		viewModelScope.launch {
			val isInstalled = shizukuPermissionHandler.isInstalled()
			val isAlive = shizukuPermissionHandler.isBinderAlive.first()
			val isGranted = shizukuPermissionHandler.isGranted.first()
			if (isAlive && isGranted) cancel()
			if (isInstalled) {
				if (!isAlive) {
					_snackbarStringId.emit(CommonR.string.shizuku_not_alive)
				}
			} else {
				_snackbarStringId.emit(CommonR.string.shizuku_not_installed)
			}
		}
	}
}