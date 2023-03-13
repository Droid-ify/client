package com.looker.core.datastore

import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import com.looker.core.common.device.Miui
import com.looker.core.datastore.UserPreferencesRepository.PreferencesKeys.AUTO_SYNC
import com.looker.core.datastore.UserPreferencesRepository.PreferencesKeys.AUTO_UPDATE
import com.looker.core.datastore.UserPreferencesRepository.PreferencesKeys.CLEAN_UP_DURATION
import com.looker.core.datastore.UserPreferencesRepository.PreferencesKeys.DYNAMIC_THEME
import com.looker.core.datastore.UserPreferencesRepository.PreferencesKeys.FAVOURITE_APPS
import com.looker.core.datastore.UserPreferencesRepository.PreferencesKeys.INCOMPATIBLE_VERSIONS
import com.looker.core.datastore.UserPreferencesRepository.PreferencesKeys.INSTALLER_TYPE
import com.looker.core.datastore.UserPreferencesRepository.PreferencesKeys.LANGUAGE
import com.looker.core.datastore.UserPreferencesRepository.PreferencesKeys.NOTIFY_UPDATES
import com.looker.core.datastore.UserPreferencesRepository.PreferencesKeys.PROXY_HOST
import com.looker.core.datastore.UserPreferencesRepository.PreferencesKeys.PROXY_PORT
import com.looker.core.datastore.UserPreferencesRepository.PreferencesKeys.PROXY_TYPE
import com.looker.core.datastore.UserPreferencesRepository.PreferencesKeys.SORT_ORDER
import com.looker.core.datastore.UserPreferencesRepository.PreferencesKeys.THEME
import com.looker.core.datastore.UserPreferencesRepository.PreferencesKeys.UNSTABLE_UPDATES
import com.looker.core.datastore.model.AutoSync
import com.looker.core.datastore.model.InstallerType
import com.looker.core.datastore.model.ProxyType
import com.looker.core.datastore.model.SortOrder
import com.looker.core.datastore.model.Theme
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.io.IOException
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours

data class UserPreferences(
	val language: String,
	val incompatibleVersions: Boolean,
	val notifyUpdate: Boolean,
	val unstableUpdate: Boolean,
	val theme: Theme,
	val dynamicTheme: Boolean,
	val installerType: InstallerType,
	val autoUpdate: Boolean,
	val autoSync: AutoSync,
	val sortOrder: SortOrder,
	val proxyType: ProxyType,
	val proxyHost: String,
	val proxyPort: Int,
	val cleanUpDuration: Duration,
	val favouriteApps: Set<String>
)

inline fun <T> Flow<UserPreferences>.distinctMap(crossinline block: suspend (UserPreferences) -> T): Flow<T> =
	map(block).distinctUntilChanged()

class UserPreferencesRepository(private val dataStore: DataStore<Preferences>) {
	private val tag: String = "UserPreferenceRepo"

	private object PreferencesKeys {
		val LANGUAGE = stringPreferencesKey("key_language")
		val INCOMPATIBLE_VERSIONS = booleanPreferencesKey("key_incompatible_versions")
		val NOTIFY_UPDATES = booleanPreferencesKey("key_notify_updates")
		val UNSTABLE_UPDATES = booleanPreferencesKey("key_unstable_updates")
		val THEME = stringPreferencesKey("key_theme")
		val DYNAMIC_THEME = booleanPreferencesKey("key_dynamic_theme")
		val INSTALLER_TYPE = stringPreferencesKey("key_installer_type")
		val AUTO_UPDATE = booleanPreferencesKey("key_auto_updates")
		val AUTO_SYNC = stringPreferencesKey("key_auto_sync")
		val SORT_ORDER = stringPreferencesKey("key_sort_order")
		val PROXY_TYPE = stringPreferencesKey("key_proxy_type")
		val PROXY_HOST = stringPreferencesKey("key_proxy_host")
		val PROXY_PORT = intPreferencesKey("key_proxy_port")
		val CLEAN_UP_DURATION = longPreferencesKey("clean_up_duration")
		val FAVOURITE_APPS = stringSetPreferencesKey("favourite_apps")
	}

	val userPreferencesFlow: Flow<UserPreferences> = dataStore.data
		.catch { exception ->
			if (exception is IOException) Log.e(tag, "Error reading preferences.", exception)
			else throw exception
		}.map(::mapUserPreferences)

	suspend fun setLanguage(language: String) =
		LANGUAGE.update(language)

	suspend fun enableIncompatibleVersion(enable: Boolean) =
		INCOMPATIBLE_VERSIONS.update(enable)

	suspend fun enableNotifyUpdates(enable: Boolean) =
		NOTIFY_UPDATES.update(enable)

	suspend fun enableUnstableUpdates(enable: Boolean) =
		UNSTABLE_UPDATES.update(enable)

	suspend fun setTheme(theme: Theme) =
		THEME.update(theme.name)

	suspend fun setDynamicTheme(enable: Boolean) =
		DYNAMIC_THEME.update(enable)

	suspend fun setInstallerType(installerType: InstallerType) =
		INSTALLER_TYPE.update(installerType.name)

	suspend fun setAutoUpdate(allow: Boolean) =
		AUTO_UPDATE.update(allow)

	suspend fun setAutoSync(autoSync: AutoSync) =
		AUTO_SYNC.update(autoSync.name)

	suspend fun setSortOrder(sortOrder: SortOrder) =
		SORT_ORDER.update(sortOrder.name)

	suspend fun setProxyType(proxyType: ProxyType) =
		PROXY_TYPE.update(proxyType.name)

	suspend fun setProxyHost(proxyHost: String) =
		PROXY_HOST.update(proxyHost)

	suspend fun setProxyPort(proxyPort: Int) =
		PROXY_PORT.update(proxyPort)

	suspend fun setCleanUpDuration(duration: Duration) =
		CLEAN_UP_DURATION.update(duration.inWholeHours)

	suspend fun toggleFavourites(packageName: String) {
		dataStore.edit { preference ->
			val currentSet = preference[FAVOURITE_APPS] ?: emptySet()
			val newSet = currentSet.toMutableSet()
			if (!newSet.add(packageName)) newSet.remove(packageName)
			preference[FAVOURITE_APPS] = newSet.toSet()
		}
	}

	private suspend inline fun <T> Preferences.Key<T>.update(newValue: T) {
		dataStore.edit { preferences ->
			preferences[this] = newValue
		}
	}

	suspend fun fetchInitialPreferences() =
		mapUserPreferences(dataStore.data.first().toPreferences())

	private fun mapUserPreferences(preferences: Preferences): UserPreferences {
		val installerName = (if (Miui.isMiui) {
			if (Miui.isMiuiOptimizationDisabled()) InstallerType.SESSION else InstallerType.LEGACY
		} else InstallerType.SESSION).name

		val language = preferences[LANGUAGE] ?: "system"
		val incompatibleVersions = preferences[INCOMPATIBLE_VERSIONS] ?: false
		val notifyUpdate = preferences[NOTIFY_UPDATES] ?: true
		val unstableUpdate = preferences[UNSTABLE_UPDATES] ?: false
		val theme = Theme.valueOf(preferences[THEME] ?: Theme.SYSTEM.name)
		val dynamicTheme = preferences[DYNAMIC_THEME] ?: false
		val installerType = InstallerType.valueOf(preferences[INSTALLER_TYPE] ?: installerName)
		val autoUpdate = preferences[AUTO_UPDATE] ?: false
		val autoSync = AutoSync.valueOf(preferences[AUTO_SYNC] ?: AutoSync.WIFI_ONLY.name)
		val sortOrder = SortOrder.valueOf(preferences[SORT_ORDER] ?: SortOrder.UPDATED.name)
		val proxyType = ProxyType.valueOf(preferences[PROXY_TYPE] ?: ProxyType.DIRECT.name)
		val proxyHost = preferences[PROXY_HOST] ?: "localhost"
		val proxyPort = preferences[PROXY_PORT] ?: 9050
		val cleanUpDuration = preferences[CLEAN_UP_DURATION]?.hours ?: 12L.hours
		val favouriteApps = preferences[FAVOURITE_APPS] ?: emptySet()

		return UserPreferences(
			language = language,
			incompatibleVersions = incompatibleVersions,
			notifyUpdate = notifyUpdate,
			unstableUpdate = unstableUpdate,
			theme = theme,
			dynamicTheme = dynamicTheme,
			installerType = installerType,
			autoUpdate = autoUpdate,
			autoSync = autoSync,
			sortOrder = sortOrder,
			proxyType = proxyType,
			proxyHost = proxyHost,
			proxyPort = proxyPort,
			cleanUpDuration = cleanUpDuration,
			favouriteApps = favouriteApps
		)
	}
}