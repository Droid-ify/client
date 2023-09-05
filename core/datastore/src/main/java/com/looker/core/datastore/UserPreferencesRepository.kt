package com.looker.core.datastore

import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import com.looker.core.common.device.Miui
import com.looker.core.common.extension.updateAsMutable
import com.looker.core.datastore.UserPreferencesRepository.PreferencesKeys.AUTO_SYNC
import com.looker.core.datastore.UserPreferencesRepository.PreferencesKeys.AUTO_UPDATE
import com.looker.core.datastore.UserPreferencesRepository.PreferencesKeys.CLEAN_UP_INTERVAL
import com.looker.core.datastore.UserPreferencesRepository.PreferencesKeys.DYNAMIC_THEME
import com.looker.core.datastore.UserPreferencesRepository.PreferencesKeys.FAVOURITE_APPS
import com.looker.core.datastore.UserPreferencesRepository.PreferencesKeys.INCOMPATIBLE_VERSIONS
import com.looker.core.datastore.UserPreferencesRepository.PreferencesKeys.INSTALLER_TYPE
import com.looker.core.datastore.UserPreferencesRepository.PreferencesKeys.LANGUAGE
import com.looker.core.datastore.UserPreferencesRepository.PreferencesKeys.LAST_CLEAN_UP
import com.looker.core.datastore.UserPreferencesRepository.PreferencesKeys.NOTIFY_UPDATES
import com.looker.core.datastore.UserPreferencesRepository.PreferencesKeys.PROXY_HOST
import com.looker.core.datastore.UserPreferencesRepository.PreferencesKeys.PROXY_PORT
import com.looker.core.datastore.UserPreferencesRepository.PreferencesKeys.PROXY_TYPE
import com.looker.core.datastore.UserPreferencesRepository.PreferencesKeys.SORT_ORDER
import com.looker.core.datastore.UserPreferencesRepository.PreferencesKeys.THEME
import com.looker.core.datastore.UserPreferencesRepository.PreferencesKeys.UNSTABLE_UPDATES
import com.looker.core.datastore.model.*
import kotlinx.coroutines.flow.*
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import java.io.IOException
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours

// TODO: rename to Settings
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
	val proxy: ProxyPreference,
	val cleanUpInterval: Duration,
	val lastCleanup: Instant?,
	val favouriteApps: Set<String>
)

inline fun <T> Flow<UserPreferences>.getProperty(crossinline block: suspend UserPreferences.() -> T): Flow<T> =
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
		val CLEAN_UP_INTERVAL = longPreferencesKey("clean_up_interval")
		val LAST_CLEAN_UP = longPreferencesKey("clean_up_interval")
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

	suspend fun setCleanUpInterval(interval: Duration) =
		CLEAN_UP_INTERVAL.update(interval.inWholeHours)

	suspend fun setCleanupInstant() =
		LAST_CLEAN_UP.update(Clock.System.now().toEpochMilliseconds())

	suspend fun toggleFavourites(packageName: String) {
		dataStore.edit { preference ->
			val currentSet = preference[FAVOURITE_APPS] ?: emptySet()
			val newSet = currentSet.updateAsMutable {
				if (!add(packageName)) remove(packageName)
			}
			preference[FAVOURITE_APPS] = newSet
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
		val type = ProxyType.valueOf(preferences[PROXY_TYPE] ?: ProxyType.DIRECT.name)
		val host = preferences[PROXY_HOST] ?: "localhost"
		val port = preferences[PROXY_PORT] ?: 9050
		val proxy = ProxyPreference(type = type, host = host, port = port)
		val cleanUpInterval = preferences[CLEAN_UP_INTERVAL]?.hours ?: 12L.hours
		val lastCleanup = preferences[LAST_CLEAN_UP]?.let { Instant.fromEpochMilliseconds(it) }
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
			proxy = proxy,
			cleanUpInterval = cleanUpInterval,
			lastCleanup = lastCleanup,
			favouriteApps = favouriteApps
		)
	}
}