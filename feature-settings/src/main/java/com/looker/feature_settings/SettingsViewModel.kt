package com.looker.feature_settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.looker.core_datastore.UserPreferencesRepository
import com.looker.core_datastore.model.AutoSync
import com.looker.core_datastore.model.InstallerType
import com.looker.core_datastore.model.ProxyType
import com.looker.core_datastore.model.Theme
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch

class SettingsViewModel(
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

	fun setListAnimation(enable: Boolean) {
		viewModelScope.launch {
			userPreferencesRepository.enableListAnimation(enable)
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
			userPreferencesRepository.setInstallerType(installerType)
		}
	}
}

internal class SettingsViewModelFactory(
	private val userPreferencesRepository: UserPreferencesRepository
) : ViewModelProvider.Factory {
	@Suppress("UNCHECKED_CAST")
	override fun <T : ViewModel> create(modelClass: Class<T>): T {
		if (modelClass.isAssignableFrom(SettingsViewModel::class.java))
			return SettingsViewModel(userPreferencesRepository) as T
		throw IllegalArgumentException("Unknown ViewModel Class")
	}
}