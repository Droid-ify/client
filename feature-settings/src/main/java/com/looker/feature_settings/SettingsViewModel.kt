package com.looker.feature_settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.looker.core_datastore.UserPreferencesRepository
import com.looker.core_datastore.model.AutoSync
import com.looker.core_datastore.model.InstallerType
import com.looker.core_datastore.model.ProxyType
import com.looker.core_datastore.model.Theme
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.time.Duration

@HiltViewModel
class SettingsViewModel
@Inject constructor(
	private val userPreferencesRepository: UserPreferencesRepository
) : ViewModel() {

	val initialSetup = flow {
		emit(userPreferencesRepository.fetchInitialPreferences())
	}

	val userPreferencesFlow = userPreferencesRepository.userPreferencesFlow

	fun setLanguage(language: String) {
		viewModelScope.launch {
			userPreferencesRepository.setLanguage(language)
		}
	}

	fun setTheme(theme: Theme) {
		viewModelScope.launch {
			userPreferencesRepository.setTheme(theme)
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

	fun setDisplayScreenshots(enable: Boolean) {
		viewModelScope.launch {
			userPreferencesRepository.setDisplayScreenshots(enable)
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
			userPreferencesRepository.setInstallerType(installerType)
		}
	}
}