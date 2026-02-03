package com.looker.droidify.datastore

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
import com.looker.droidify.datastore.model.AutoSync
import com.looker.droidify.datastore.model.InstallerType
import com.looker.droidify.datastore.model.LegacyInstallerComponent
import com.looker.droidify.datastore.model.ProxyPreference
import com.looker.droidify.datastore.model.ProxyType
import com.looker.droidify.datastore.model.SortOrder
import com.looker.droidify.datastore.model.Theme
import com.looker.droidify.utility.common.Exporter
import com.looker.droidify.utility.common.extension.updateAsMutable
import java.util.*
import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlin.time.ExperimentalTime
import kotlin.time.Instant
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

@OptIn(ExperimentalTime::class)
class PreferenceSettingsRepository(
    private val dataStore: DataStore<Preferences>,
    private val exporter: Exporter<Settings>,
) : SettingsRepository {
    override val data: Flow<Settings> = dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                Log.e("PreferencesSettingsRepository", "Error reading preferences.", exception)
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

    override suspend fun setLegacyInstallerComponent(component: LegacyInstallerComponent?) {
        when (component) {
            null -> {
                LEGACY_INSTALLER_COMPONENT_TYPE.update("")
                LEGACY_INSTALLER_COMPONENT_CLASS.update("")
                LEGACY_INSTALLER_COMPONENT_ACTIVITY.update("")
            }

            is LegacyInstallerComponent.Component -> {
                LEGACY_INSTALLER_COMPONENT_TYPE.update("component")
                LEGACY_INSTALLER_COMPONENT_CLASS.update(component.clazz)
                LEGACY_INSTALLER_COMPONENT_ACTIVITY.update(component.activity)
            }

            LegacyInstallerComponent.Unspecified -> {
                LEGACY_INSTALLER_COMPONENT_TYPE.update("unspecified")
                LEGACY_INSTALLER_COMPONENT_CLASS.update("")
                LEGACY_INSTALLER_COMPONENT_ACTIVITY.update("")
            }

            LegacyInstallerComponent.AlwaysChoose -> {
                LEGACY_INSTALLER_COMPONENT_TYPE.update("always_choose")
                LEGACY_INSTALLER_COMPONENT_CLASS.update("")
                LEGACY_INSTALLER_COMPONENT_ACTIVITY.update("")
            }
        }
    }

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

    override suspend fun setRbLogLastModified(date: Date) =
        LAST_RB_FETCH.update(date.time)

    override suspend fun updateLastModifiedDownloadStats(date: Date) {
        dataStore.edit { pref ->
            val currentValue = pref[LAST_MODIFIED_DS] ?: 0
            if (date.time > currentValue) pref[LAST_MODIFIED_DS] = date.time
        }
    }

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

    override suspend fun setRepoEnabled(repoId: Int, enabled: Boolean) {
        dataStore.edit { preference ->
            val currentSet = preference[ENABLED_REPO_IDS] ?: emptySet()
            val newSet = currentSet.updateAsMutable {
                if (enabled) add(repoId.toString()) else remove(repoId.toString())
            }
            preference[ENABLED_REPO_IDS] = newSet
        }
    }

    override fun getEnabledRepoIds(): Flow<Set<Int>> {
        return data.map { it.enabledRepoIds }
    }

    override suspend fun isRepoEnabled(repoId: Int): Boolean {
        return repoId in data.first().enabledRepoIds
    }

    override suspend fun setDeleteApkOnInstall(enable: Boolean) =
        DELETE_APK_ON_INSTALL.update(enable)

    private fun mapSettings(preferences: Preferences): Settings {
        val installerType =
            InstallerType.valueOf(preferences[INSTALLER_TYPE] ?: InstallerType.Default.name)
        val legacyInstallerComponent = when (preferences[LEGACY_INSTALLER_COMPONENT_TYPE]) {
            "component" -> {
                preferences[LEGACY_INSTALLER_COMPONENT_CLASS]?.takeIf { it.isNotBlank() }
                    ?.let { cls ->
                        preferences[LEGACY_INSTALLER_COMPONENT_ACTIVITY]?.takeIf { it.isNotBlank() }
                            ?.let { act ->
                                LegacyInstallerComponent.Component(cls, act)
                            }
                    }
            }

            "unspecified" -> LegacyInstallerComponent.Unspecified
            "always_choose" -> LegacyInstallerComponent.AlwaysChoose
            else -> null
        }

        val language = preferences[LANGUAGE] ?: "system"
        val incompatibleVersions = preferences[INCOMPATIBLE_VERSIONS] ?: false
        val notifyUpdate = preferences[NOTIFY_UPDATES] ?: true
        val unstableUpdate = preferences[UNSTABLE_UPDATES] ?: false
        val ignoreSignature = preferences[IGNORE_SIGNATURE] ?: false
        val theme = Theme.valueOf(preferences[THEME] ?: Theme.SYSTEM.name)
        val dynamicTheme = preferences[DYNAMIC_THEME] ?: false
        val autoUpdate = preferences[AUTO_UPDATE] ?: false
        val autoSync = AutoSync.valueOf(preferences[AUTO_SYNC] ?: AutoSync.WIFI_ONLY.name)
        val sortOrder = SortOrder.valueOf(preferences[SORT_ORDER] ?: SortOrder.UPDATED.name)
        val type = ProxyType.valueOf(preferences[PROXY_TYPE] ?: ProxyType.DIRECT.name)
        val host = preferences[PROXY_HOST] ?: "localhost"
        val port = preferences[PROXY_PORT] ?: 9050
        val proxy = ProxyPreference(type = type, host = host, port = port)
        val cleanUpInterval = preferences[CLEAN_UP_INTERVAL]?.hours ?: 12L.hours
        val lastCleanup = preferences[LAST_CLEAN_UP]?.let { Instant.fromEpochMilliseconds(it) }
        val lastRbLogFetch = preferences[LAST_RB_FETCH]?.let { Instant.fromEpochMilliseconds(it) }
        val lastModifiedDownloadStats = preferences[LAST_MODIFIED_DS]?.takeIf { it > 0L }
        val favouriteApps = preferences[FAVOURITE_APPS] ?: emptySet()
        val homeScreenSwiping = preferences[HOME_SCREEN_SWIPING] ?: true
        val enabledRepoIds =
            preferences[ENABLED_REPO_IDS]?.mapNotNull { it.toIntOrNull() }?.toSet() ?: emptySet()
        val deleteApkOnInstall = preferences[DELETE_APK_ON_INSTALL] ?: false

        return Settings(
            language = language,
            incompatibleVersions = incompatibleVersions,
            notifyUpdate = notifyUpdate,
            unstableUpdate = unstableUpdate,
            ignoreSignature = ignoreSignature,
            theme = theme,
            dynamicTheme = dynamicTheme,
            installerType = installerType,
            legacyInstallerComponent = legacyInstallerComponent,
            autoUpdate = autoUpdate,
            autoSync = autoSync,
            sortOrder = sortOrder,
            proxy = proxy,
            cleanUpInterval = cleanUpInterval,
            lastCleanup = lastCleanup,
            lastRbLogFetch = lastRbLogFetch,
            lastModifiedDownloadStats = lastModifiedDownloadStats,
            favouriteApps = favouriteApps,
            homeScreenSwiping = homeScreenSwiping,
            enabledRepoIds = enabledRepoIds,
            deleteApkOnInstall = deleteApkOnInstall,
        )
    }

    private suspend inline fun <T> Preferences.Key<T>.update(newValue: T) {
        dataStore.edit { preferences ->
            preferences[this] = newValue
        }
    }

    companion object PreferencesKeys {
        val LANGUAGE = stringPreferencesKey("key_language")
        val INCOMPATIBLE_VERSIONS = booleanPreferencesKey("key_incompatible_versions")
        val NOTIFY_UPDATES = booleanPreferencesKey("key_notify_updates")
        val UNSTABLE_UPDATES = booleanPreferencesKey("key_unstable_updates")
        val IGNORE_SIGNATURE = booleanPreferencesKey("key_ignore_signature")
        val DYNAMIC_THEME = booleanPreferencesKey("key_dynamic_theme")
        val AUTO_UPDATE = booleanPreferencesKey("key_auto_updates")
        val PROXY_HOST = stringPreferencesKey("key_proxy_host")
        val PROXY_PORT = intPreferencesKey("key_proxy_port")
        val CLEAN_UP_INTERVAL = longPreferencesKey("key_clean_up_interval")
        val LAST_CLEAN_UP = longPreferencesKey("key_last_clean_up_time")
        val LAST_RB_FETCH = longPreferencesKey("key_last_rb_logs_fetch_time")
        val LAST_MODIFIED_DS = longPreferencesKey("key_last_modified_download_stats")
        val FAVOURITE_APPS = stringSetPreferencesKey("key_favourite_apps")
        val HOME_SCREEN_SWIPING = booleanPreferencesKey("key_home_swiping")
        val DELETE_APK_ON_INSTALL = booleanPreferencesKey("key_delete_apk_on_install")
        val LEGACY_INSTALLER_COMPONENT_CLASS =
            stringPreferencesKey("key_legacy_installer_component_class")
        val LEGACY_INSTALLER_COMPONENT_ACTIVITY =
            stringPreferencesKey("key_legacy_installer_component_activity")
        val LEGACY_INSTALLER_COMPONENT_TYPE =
            stringPreferencesKey("key_legacy_installer_component_type")
        val ENABLED_REPO_IDS = stringSetPreferencesKey("key_enabled_repo_ids")

        // Enums
        val THEME = stringPreferencesKey("key_theme")
        val INSTALLER_TYPE = stringPreferencesKey("key_installer_type")
        val AUTO_SYNC = stringPreferencesKey("key_auto_sync")
        val SORT_ORDER = stringPreferencesKey("key_sort_order")
        val PROXY_TYPE = stringPreferencesKey("key_proxy_type")

        fun MutablePreferences.setting(settings: Settings): Preferences {
            set(LANGUAGE, settings.language)
            set(INCOMPATIBLE_VERSIONS, settings.incompatibleVersions)
            set(NOTIFY_UPDATES, settings.notifyUpdate)
            set(UNSTABLE_UPDATES, settings.unstableUpdate)
            set(THEME, settings.theme.name)
            set(DYNAMIC_THEME, settings.dynamicTheme)
            when (settings.legacyInstallerComponent) {
                is LegacyInstallerComponent.Component -> {
                    set(LEGACY_INSTALLER_COMPONENT_TYPE, "component")
                    set(LEGACY_INSTALLER_COMPONENT_CLASS, settings.legacyInstallerComponent.clazz)
                    set(
                        LEGACY_INSTALLER_COMPONENT_ACTIVITY,
                        settings.legacyInstallerComponent.activity,
                    )
                }

                LegacyInstallerComponent.Unspecified -> {
                    set(LEGACY_INSTALLER_COMPONENT_TYPE, "unspecified")
                    set(LEGACY_INSTALLER_COMPONENT_CLASS, "")
                    set(LEGACY_INSTALLER_COMPONENT_ACTIVITY, "")
                }

                LegacyInstallerComponent.AlwaysChoose -> {
                    set(LEGACY_INSTALLER_COMPONENT_TYPE, "always_choose")
                    set(LEGACY_INSTALLER_COMPONENT_CLASS, "")
                    set(LEGACY_INSTALLER_COMPONENT_ACTIVITY, "")
                }

                null -> {
                    set(LEGACY_INSTALLER_COMPONENT_TYPE, "")
                    set(LEGACY_INSTALLER_COMPONENT_CLASS, "")
                    set(LEGACY_INSTALLER_COMPONENT_ACTIVITY, "")
                }
            }
            set(INSTALLER_TYPE, settings.installerType.name)
            set(AUTO_UPDATE, settings.autoUpdate)
            set(AUTO_SYNC, settings.autoSync.name)
            set(SORT_ORDER, settings.sortOrder.name)
            set(PROXY_TYPE, settings.proxy.type.name)
            set(PROXY_HOST, settings.proxy.host)
            set(PROXY_PORT, settings.proxy.port)
            set(CLEAN_UP_INTERVAL, settings.cleanUpInterval.inWholeHours)
            set(LAST_CLEAN_UP, settings.lastCleanup?.toEpochMilliseconds() ?: 0L)
            set(LAST_RB_FETCH, settings.lastRbLogFetch?.toEpochMilliseconds() ?: 0L)
            set(LAST_MODIFIED_DS, settings.lastModifiedDownloadStats ?: 0L)
            set(FAVOURITE_APPS, settings.favouriteApps)
            set(HOME_SCREEN_SWIPING, settings.homeScreenSwiping)
            set(ENABLED_REPO_IDS, settings.enabledRepoIds.map { it.toString() }.toSet())
            set(DELETE_APK_ON_INSTALL, settings.deleteApkOnInstall)
            return this.toPreferences()
        }
    }
}
