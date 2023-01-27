package com.looker.feature_settings

import android.content.pm.PackageManager
import android.util.Log
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

	fun setDynamicTheme(enable: Boolean) {
		viewModelScope.launch {
			userPreferencesRepository.setDynamicTheme(enable)
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
					Shizuku.addRequestPermissionResultListener(permissionListener)
					val haveShizuku = checkShizukuPermission()
					userPreferencesRepository.setInstallerType(
						if (haveShizuku) InstallerType.SHIZUKU else InstallerType.SESSION
					)
				}
				InstallerType.ROOT -> {
					val isRooted = Shell.isAppGrantedRoot()
					userPreferencesRepository.setInstallerType(
						if (isRooted == true) InstallerType.ROOT
						else InstallerType.SESSION
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
		return if (Shizuku.pingBinder()) {
			if (Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED) true
			else if (Shizuku.shouldShowRequestPermissionRationale()) false
			else {
				Shizuku.requestPermission(SHIZUKU_PERMISSION_REQUEST_CODE)
				false
			}
		} else false
	}

	private val shizukuDeadListener = Shizuku.OnBinderDeadListener {
		Log.e("ShizukuInstaller", "Killed")
	}

	private val permissionListener =
		object : Shizuku.OnRequestPermissionResultListener {
			override fun onRequestPermissionResult(requestCode: Int, grantResult: Int) {
				if (requestCode == SHIZUKU_PERMISSION_REQUEST_CODE) {
					val granted = grantResult == PackageManager.PERMISSION_GRANTED
					viewModelScope.launch {
						userPreferencesRepository.setInstallerType(
							if (granted) InstallerType.SHIZUKU
							else InstallerType.SESSION
						)
					}
					Shizuku.removeRequestPermissionResultListener(this)
				}
			}
		}

	init {
		Shizuku.addBinderDeadListener(shizukuDeadListener)
	}

	override fun onCleared() {
		super.onCleared()
		Shizuku.removeRequestPermissionResultListener(permissionListener)
		Shizuku.removeBinderDeadListener(shizukuDeadListener)
	}

}

private const val SHIZUKU_PERMISSION_REQUEST_CODE = 87263