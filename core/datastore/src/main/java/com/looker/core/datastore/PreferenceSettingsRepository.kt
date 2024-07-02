package com.looker.core.datastore

import android.net.Uri
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.core.IOException
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import com.looker.core.common.Exporter
import com.looker.core.common.extension.updateAsMutable
import com.looker.core.datastore.model.AutoSync
import com.looker.core.datastore.model.InstallerType
import com.looker.core.datastore.model.ProxyPreference
import com.looker.core.datastore.model.ProxyType
import com.looker.core.datastore.model.SortOrder
import com.looker.core.datastore.model.Theme
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours

class PreferenceSettingsRepository(
    private val dataStore: DataStore<Preferences>,
    private val exporter: Exporter<Settings>,
) : SettingsRepository {
    override val data: Flow<Settings> = dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                Log.e("TAG", "Error reading preferences.", exception)
            } else {
                throw exception
            }
        }.map(::mapSettings)

    override suspend fun getInitial(): Settings {
        return data.first()
    }

    override suspend fun export(target: Uri) {
        val currentSettings = getInitial()
        exporter.export(currentSettings, target)
    }

    override suspend fun import(target: Uri) {
        val importedSettings = exporter.import(target)
        val updatedFavorites = importedSettings.favouriteApps +
            getInitial().favouriteApps
        val updatedSettings = importedSettings.copy(favouriteApps = updatedFavorites)
        dataStore.edit {
            it.setting(updatedSettings)
        }
    }

    override suspend fun setLanguage(language: String) =
        LANGUAGE.update(language)

    override suspend fun enableIncompatibleVersion(enable: Boolean) =
        INCOMPATIBLE_VERSIONS.update(enable)

    override suspend fun enableNotifyUpdates(enable: Boolean) =
        NOTIFY_UPDATES.update(enable)

    override suspend fun enableUnstableUpdates(enable: Boolean) =
        UNSTABLE_UPDATES.update(enable)

    override suspend fun setIgnoreSignature(enable: Boolean) =
        IGNORE_SIGNATURE.update(enable)

    override suspend fun setTheme(theme: Theme) =
        THEME.update(theme.name)

    override suspend fun setDynamicTheme(enable: Boolean) =
        DYNAMIC_THEME.update(enable)

    override suspend fun setInstallerType(installerType: InstallerType) =
        INSTALLER_TYPE.update(installerType.name)

    override suspend fun setAutoUpdate(allow: Boolean) =
        AUTO_UPDATE.update(allow)

    override suspend fun setAutoSync(autoSync: AutoSync) =
        AUTO_SYNC.update(autoSync.name)

    override suspend fun setSortOrder(sortOrder: SortOrder) =
        SORT_ORDER.update(sortOrder.name)

    override suspend fun setProxyType(proxyType: ProxyType) =
        PROXY_TYPE.update(proxyType.name)

    override suspend fun setProxyHost(proxyHost: String) =
        PROXY_HOST.update(proxyHost)

    override suspend fun setProxyPort(proxyPort: Int) =
        PROXY_PORT.update(proxyPort)

    override suspend fun setCleanUpInterval(interval: Duration) =
        CLEAN_UP_INTERVAL.update(interval.inWholeHours)

    override suspend fun setCleanupInstant() =
        LAST_CLEAN_UP.update(Clock.System.now().toEpochMilliseconds())

    override suspend fun setHomeScreenSwiping(value: Boolean) =
        HOME_SCREEN_SWIPING.update(value)

    override suspend fun toggleFavourites(packageName: String) {
        dataStore.edit { preference ->
            val currentSet = preference[FAVOURITE_APPS] ?: emptySet()
            val newSet = currentSet.updateAsMutable {
                if (!add(packageName)) remove(packageName)
            }
            preference[FAVOURITE_APPS] = newSet
        }
    }

    private fun mapSettings(preferences: Preferences): Settings {
        val defaultInstallerName = (InstallerType.Default).name

        val language = preferences[LANGUAGE] ?: "system"
        val incompatibleVersions = preferences[INCOMPATIBLE_VERSIONS] ?: false
        val notifyUpdate = preferences[NOTIFY_UPDATES] ?: true
        val unstableUpdate = preferences[UNSTABLE_UPDATES] ?: false
        val ignoreSignature = preferences[IGNORE_SIGNATURE] ?: false
        val theme = Theme.valueOf(preferences[THEME] ?: Theme.SYSTEM.name)
        val dynamicTheme = preferences[DYNAMIC_THEME] ?: false
        val installerType =
            InstallerType.valueOf(preferences[INSTALLER_TYPE] ?: defaultInstallerName)
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
        val homeScreenSwiping = preferences[HOME_SCREEN_SWIPING] ?: true

        return Settings(
            language = language,
            incompatibleVersions = incompatibleVersions,
            notifyUpdate = notifyUpdate,
            unstableUpdate = unstableUpdate,
            ignoreSignature = ignoreSignature,
            theme = theme,
            dynamicTheme = dynamicTheme,
            installerType = installerType,
            autoUpdate = autoUpdate,
            autoSync = autoSync,
            sortOrder = sortOrder,
            proxy = proxy,
            cleanUpInterval = cleanUpInterval,
            lastCleanup = lastCleanup,
            favouriteApps = favouriteApps,
            homeScreenSwiping = homeScreenSwiping,
        )
    }

    private suspend inline fun <T> Preferences.Key<T>.update(newValue: T) {
        dataStore.edit { preferences ->
            preferences[this] = newValue
        }
    }

    companion object PreferencesKeys {
        private val LANGUAGE = stringPreferencesKey("key_language")
        private val INCOMPATIBLE_VERSIONS = booleanPreferencesKey("key_incompatible_versions")
        private val NOTIFY_UPDATES = booleanPreferencesKey("key_notify_updates")
        private val UNSTABLE_UPDATES = booleanPreferencesKey("key_unstable_updates")
        private val IGNORE_SIGNATURE = booleanPreferencesKey("key_ignore_signature")
        private val THEME = stringPreferencesKey("key_theme")
        private val DYNAMIC_THEME = booleanPreferencesKey("key_dynamic_theme")
        private val INSTALLER_TYPE = stringPreferencesKey("key_installer_type")
        private val AUTO_UPDATE = booleanPreferencesKey("key_auto_updates")
        private val AUTO_SYNC = stringPreferencesKey("key_auto_sync")
        private val SORT_ORDER = stringPreferencesKey("key_sort_order")
        private val PROXY_TYPE = stringPreferencesKey("key_proxy_type")
        private val PROXY_HOST = stringPreferencesKey("key_proxy_host")
        private val PROXY_PORT = intPreferencesKey("key_proxy_port")
        private val CLEAN_UP_INTERVAL = longPreferencesKey("key_clean_up_interval")
        private val LAST_CLEAN_UP = longPreferencesKey("key_last_clean_up_time")
        private val FAVOURITE_APPS = stringSetPreferencesKey("key_favourite_apps")
        private val HOME_SCREEN_SWIPING = booleanPreferencesKey("key_home_swiping")

        fun MutablePreferences.setting(settings: Settings): Preferences {
            set(LANGUAGE, settings.language)
            set(INCOMPATIBLE_VERSIONS, settings.incompatibleVersions)
            set(NOTIFY_UPDATES, settings.notifyUpdate)
            set(UNSTABLE_UPDATES, settings.unstableUpdate)
            set(THEME, settings.theme.name)
            set(DYNAMIC_THEME, settings.dynamicTheme)
            set(INSTALLER_TYPE, settings.installerType.name)
            set(AUTO_UPDATE, settings.autoUpdate)
            set(AUTO_SYNC, settings.autoSync.name)
            set(SORT_ORDER, settings.sortOrder.name)
            set(PROXY_TYPE, settings.proxy.type.name)
            set(PROXY_HOST, settings.proxy.host)
            set(PROXY_PORT, settings.proxy.port)
            set(CLEAN_UP_INTERVAL, settings.cleanUpInterval.inWholeHours)
            set(LAST_CLEAN_UP, settings.lastCleanup?.toEpochMilliseconds() ?: 0L)
            set(FAVOURITE_APPS, settings.favouriteApps)
            set(HOME_SCREEN_SWIPING, settings.homeScreenSwiping)
            return this.toPreferences()
        }
    }
}
