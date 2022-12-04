package com.looker.feature_settings

import android.content.pm.PackageManager
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.looker.core.datastore.UserPreferencesRepository
import com.looker.core.datastore.model.AutoSync
import com.looker.core.datastore.model.InstallerType
import com.looker.core.datastore.model.ProxyType
import com.looker.core.datastore.model.Theme
import com.topjohnwu.superuser.Shell
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import rikka.shizuku.Shizuku
import javax.inject.Inject
import kotlin.time.Duration

@HiltViewModel
class SettingsViewModel
@Inject constructor(
	private val userPreferencesRepository: UserPreferencesRepository
) : ViewModel() {

	val userPreferencesFlow = userPreferencesRepository.userPreferencesFlow

	fun setLanguage(language: String) {
		viewModelScope.launch {
			val appLocale: LocaleListCompat =
				LocaleListCompat.forLanguageTags(language)
			AppCompatDelegate.setApplicationLocales(appLocale)
			userPreferencesRepository.setLanguage(language)
		}
	}

	fun setTheme(theme: Theme) {
		viewModelScope.launch {
			userPreferencesRepository.setTheme(theme)
		}
	}

	fun setToolbarState(collapsing: Boolean) {
		viewModelScope.launch {
			userPreferencesRepository.setCollapsingToolbar(collapsing)
		}
	}

	fun setCleanUpDuration(duration: Duration) {
		viewModelScope.launch {
			userPreferencesRepository.setCleanUpDuration(duration)
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
					val haveShizuku = checkShizukuPermission()
					userPreferencesRepository.setInstallerType(
						if (haveShizuku) InstallerType.SHIZUKU else InstallerType.SESSION
					)
				}
				InstallerType.ROOT -> {
					val isRooted = Shell.rootAccess()
					userPreferencesRepository.setInstallerType(
						if (isRooted) InstallerType.ROOT else InstallerType.SESSION
					)
				}
				else -> {
					userPreferencesRepository.setInstallerType(installerType)
				}
			}
		}
	}

	private fun checkShizukuPermission(): Boolean {
		if (Shizuku.isPreV11()) return false
		return if (Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED) true
		else if (Shizuku.shouldShowRequestPermissionRationale()) false
		else {
			Shizuku.requestPermission(SHIZUKU_PERMISSION_REQUEST_CODE)
			false
		}
	}

	private val permissionListener =
		Shizuku.OnRequestPermissionResultListener { _, grantResult ->
			viewModelScope.launch {
				val granted = grantResult == PackageManager.PERMISSION_GRANTED
				if (!granted) userPreferencesRepository.setInstallerType(InstallerType.SESSION)
			}
		}

	init {
		Shizuku.addRequestPermissionResultListener(permissionListener)
	}

	override fun onCleared() {
		super.onCleared()
		Shizuku.removeRequestPermissionResultListener(permissionListener)
	}

}

private const val SHIZUKU_PERMISSION_REQUEST_CODE = 87263