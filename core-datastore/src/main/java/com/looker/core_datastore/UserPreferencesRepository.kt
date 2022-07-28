package com.looker.core_datastore

import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.looker.core_datastore.model.AutoSync
import com.looker.core_datastore.model.InstallerType
import com.looker.core_datastore.model.ProxyType
import com.looker.core_datastore.model.SortOrder
import com.looker.core_datastore.model.Theme
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.io.IOException
import java.util.*
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours

/**
 * Settings data class
 */
data class UserPreferences(
	val language: String,
	val incompatibleVersions: Boolean,
	val listAnimation: Boolean,
	val notifyUpdate: Boolean,
	val unstableUpdate: Boolean,
	val theme: Theme,
	val installerType: InstallerType,
	val autoSync: AutoSync,
	val sortOrder: SortOrder,
	val proxyType: ProxyType,
	val proxyHost: String,
	val proxyPort: Int,
	val cleanUpDuration: Duration
)

/**
 * This class handles the data storing and retrieval
 */
class UserPreferencesRepository(private val dataStore: DataStore<Preferences>) {
	private val tag: String = "UserPreferenceRepo"

	/**
	 * Keys for the data store
	 */
	private object PreferencesKeys {
		val LANGUAGE = stringPreferencesKey("key_language")
		val INCOMPATIBLE_VERSIONS = booleanPreferencesKey("key_incompatible_versions")
		val LIST_ANIMATION = booleanPreferencesKey("key_list_animation")
		val NOTIFY_UPDATES = booleanPreferencesKey("key_notify_updates")
		val UNSTABLE_UPDATES = booleanPreferencesKey("key_unstable_updates")
		val THEME = stringPreferencesKey("key_theme")
		val INSTALLER_TYPE = stringPreferencesKey("key_installer_type")
		val AUTO_SYNC = stringPreferencesKey("key_auto_sync")
		val SORT_ORDER = stringPreferencesKey("key_sort_order")
		val PROXY_TYPE = stringPreferencesKey("key_proxy_type")
		val PROXY_HOST = stringPreferencesKey("key_proxy_host")
		val PROXY_PORT = intPreferencesKey("key_proxy_port")
		val CLEAN_UP_DURATION = longPreferencesKey("clean_up_duration")
	}

	/**
	 * Provides a flow for the [UserPreferences]
	 */
	val userPreferencesFlow: Flow<UserPreferences> = dataStore.data
		.catch { exception ->
			if (exception is IOException) Log.e(tag, "Error reading preferences.", exception)
			else throw exception
		}
		.map(::mapUserPreferences)

	/**
	 * Set [language]
	 */
	suspend fun setLanguage(language: String) {
		PreferencesKeys.LANGUAGE.update(language)
	}

	/**
	 * Incompatible Version
	 */
	suspend fun enableIncompatibleVersion(enable: Boolean) {
		PreferencesKeys.INCOMPATIBLE_VERSIONS.update(enable)
	}

	/**
	 * Animation for Items in List
	 */
	suspend fun enableListAnimation(enable: Boolean) {
		PreferencesKeys.LIST_ANIMATION.update(enable)
	}

	/**
	 * Get Notification about updates
	 */
	suspend fun enableNotifyUpdates(enable: Boolean) {
		PreferencesKeys.NOTIFY_UPDATES.update(enable)
	}

	/**
	 * Allow unstable updates for installed apps
	 */
	suspend fun enableUnstableUpdates(enable: Boolean) {
		PreferencesKeys.UNSTABLE_UPDATES.update(enable)
	}

	/**
	 * Set new [Theme]
	 */
	suspend fun setTheme(theme: Theme) {
		PreferencesKeys.THEME.update(theme.name)
	}

	/**
	 * Set a new [InstallerType]
	 */
	suspend fun setInstallerType(installerType: InstallerType) {
		PreferencesKeys.INSTALLER_TYPE.update(installerType.name)
	}

	/**
	 * Set [AutoSync] mode
	 */
	suspend fun setAutoSync(autoSync: AutoSync) {
		PreferencesKeys.AUTO_SYNC.update(autoSync.name)
	}

	/**
	 * Set a new [SortOrder] for list in home page
	 */
	suspend fun setSortOrder(sortOrder: SortOrder) {
		PreferencesKeys.SORT_ORDER.update(sortOrder.name)
	}

	/**
	 * Set a [ProxyType] for network connection
	 */
	suspend fun setProxyType(proxyType: ProxyType) {
		PreferencesKeys.PROXY_TYPE.update(proxyType.name)
	}

	/**
	 * [proxyHost] sets a host for Proxy
	 */
	suspend fun setProxyHost(proxyHost: String) {
		PreferencesKeys.PROXY_HOST.update(proxyHost)
	}

	/**
	 * [proxyPort] sets a port for Proxy
	 */
	suspend fun setProxyPort(proxyPort: Int) {
		PreferencesKeys.PROXY_PORT.update(proxyPort)
	}

	suspend fun setCleanUpDuration(duration: Duration) {
		PreferencesKeys.CLEAN_UP_DURATION.update(duration.inWholeHours)
	}

	/**
	 * Simple function to reduce boiler plate
	 */
	private suspend inline fun <T> Preferences.Key<T>.update(newValue: T) {
		dataStore.edit { preferences ->
			preferences[this] = newValue
		}
	}

	/**
	 * Fetches the initial value of [UserPreferences]
	 */
	suspend fun fetchInitialPreferences() =
		mapUserPreferences(dataStore.data.first().toPreferences())

	/**
	 * Maps [Preferences] to [UserPreferences]
	 */
	private fun mapUserPreferences(preferences: Preferences): UserPreferences {
		val language = preferences[PreferencesKeys.LANGUAGE] ?: Locale.getDefault().toString()
		val incompatibleVersions = preferences[PreferencesKeys.INCOMPATIBLE_VERSIONS] ?: false
		val listAnimation = preferences[PreferencesKeys.LIST_ANIMATION] ?: false
		val notifyUpdate = preferences[PreferencesKeys.NOTIFY_UPDATES] ?: true
		val unstableUpdate = preferences[PreferencesKeys.UNSTABLE_UPDATES] ?: false
		val theme = Theme.valueOf(
			preferences[PreferencesKeys.THEME] ?: Theme.SYSTEM.name
		)
		val installerType = InstallerType.valueOf(
			preferences[PreferencesKeys.INSTALLER_TYPE] ?: InstallerType.SESSION.name
		)
		val autoSync = AutoSync.valueOf(
			preferences[PreferencesKeys.AUTO_SYNC] ?: AutoSync.ALWAYS.name
		)
		val sortOrder = SortOrder.valueOf(
			preferences[PreferencesKeys.SORT_ORDER] ?: SortOrder.UPDATED.name
		)
		val proxyType = ProxyType.valueOf(
			preferences[PreferencesKeys.PROXY_TYPE] ?: ProxyType.DIRECT.name
		)
		val proxyHost = preferences[PreferencesKeys.PROXY_HOST] ?: "localhost"
		val proxyPort = preferences[PreferencesKeys.PROXY_PORT] ?: 9050

		val cleanUpDuration = preferences[PreferencesKeys.CLEAN_UP_DURATION]?.hours ?: 12L.hours

		return UserPreferences(
			language = language,
			incompatibleVersions = incompatibleVersions,
			listAnimation = listAnimation,
			notifyUpdate = notifyUpdate,
			unstableUpdate = unstableUpdate,
			theme = theme,
			installerType = installerType,
			autoSync = autoSync,
			sortOrder = sortOrder,
			proxyType = proxyType,
			proxyHost = proxyHost,
			proxyPort = proxyPort,
			cleanUpDuration = cleanUpDuration
		)
	}
}