package com.looker.core.datastore.migration

import androidx.datastore.core.DataMigration
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import com.looker.core.datastore.Settings
import com.looker.core.datastore.model.AutoSync
import com.looker.core.datastore.model.InstallerType
import com.looker.core.datastore.model.ProxyPreference
import com.looker.core.datastore.model.ProxyType
import com.looker.core.datastore.model.SortOrder
import com.looker.core.datastore.model.Theme
import kotlin.time.Duration.Companion.hours
import kotlinx.coroutines.flow.first
import kotlinx.datetime.Instant

class ProtoDataStoreMigration(
    private val oldDataStore: DataStore<Preferences>
) : DataMigration<Settings> {
    override suspend fun cleanUp() {
        oldDataStore.edit { it.clear() }
    }

    override suspend fun shouldMigrate(currentData: Settings): Boolean =
        oldDataStore.data.first().asMap().isNotEmpty()

    override suspend fun migrate(currentData: Settings): Settings {
        return oldDataStore.data.first().mapSettings()
    }

    // TODO: Remove after next update
    private companion object {
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
        val LAST_CLEAN_UP = longPreferencesKey("last_clean_up_time")
        val FAVOURITE_APPS = stringSetPreferencesKey("favourite_apps")
        val HOME_SCREEN_SWIPING = booleanPreferencesKey("home_swiping")

        private fun Preferences.mapSettings(): Settings {
            val defaultSetting = Settings()

            val language = this[LANGUAGE] ?: defaultSetting.language
            val incompatibleVersions = this[INCOMPATIBLE_VERSIONS]
                ?: defaultSetting.incompatibleVersions
            val notifyUpdate = this[NOTIFY_UPDATES] ?: defaultSetting.notifyUpdate
            val unstableUpdate = this[UNSTABLE_UPDATES] ?: defaultSetting.unstableUpdate
            val theme = Theme.valueOf(this[THEME] ?: Theme.SYSTEM.name)
            val dynamicTheme = this[DYNAMIC_THEME] ?: defaultSetting.dynamicTheme
            val installerType =
                InstallerType.valueOf(this[INSTALLER_TYPE] ?: defaultSetting.installerType.name)
            val autoUpdate = this[AUTO_UPDATE] ?: false
            val autoSync = AutoSync.valueOf(this[AUTO_SYNC] ?: defaultSetting.autoSync.name)
            val sortOrder = SortOrder.valueOf(this[SORT_ORDER] ?: defaultSetting.sortOrder.name)
            val type = ProxyType.valueOf(this[PROXY_TYPE] ?: defaultSetting.proxy.type.name)
            val host = this[PROXY_HOST] ?: defaultSetting.proxy.host
            val port = this[PROXY_PORT] ?: defaultSetting.proxy.port
            val proxy = ProxyPreference(type = type, host = host, port = port)
            val cleanUpInterval = this[CLEAN_UP_INTERVAL]?.hours ?: defaultSetting.cleanUpInterval
            val lastCleanup = this[LAST_CLEAN_UP]?.let { Instant.fromEpochMilliseconds(it) }
            val favouriteApps = this[FAVOURITE_APPS] ?: defaultSetting.favouriteApps
            val homeScreenSwiping = this[HOME_SCREEN_SWIPING] ?: defaultSetting.homeScreenSwiping

            return Settings(
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
                favouriteApps = favouriteApps,
                homeScreenSwiping = homeScreenSwiping
            )
        }
    }
}
