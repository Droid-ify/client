package com.looker.core.datastore

import android.util.Log
import androidx.datastore.core.DataStore
import com.looker.core.common.extension.updateAsMutable
import com.looker.core.datastore.model.AutoSync
import com.looker.core.datastore.model.InstallerType
import com.looker.core.datastore.model.ProxyType
import com.looker.core.datastore.model.SortOrder
import com.looker.core.datastore.model.Theme
import java.io.IOException
import kotlin.time.Duration
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.datetime.Clock

class SettingsRepository(private val dataStore: DataStore<Settings>) {
    private companion object {
        const val TAG: String = "SettingsRepository"
    }

    val settingsFlow: Flow<Settings> = dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                Log.e(TAG, "Error reading preferences.", exception)
            } else {
                throw exception
            }
        }

    inline fun <T> get(crossinline block: suspend Settings.() -> T): Flow<T> =
        settingsFlow.map(block).distinctUntilChanged()

    suspend fun fetchInitialPreferences() =
        dataStore.data.first()

    suspend fun setLanguage(language: String) {
        dataStore.updateData { settings ->
            settings.copy(language = language)
        }
    }

    suspend fun enableIncompatibleVersion(enable: Boolean) {
        dataStore.updateData { settings ->
            settings.copy(incompatibleVersions = enable)
        }
    }

    suspend fun enableNotifyUpdates(enable: Boolean) {
        dataStore.updateData { settings ->
            settings.copy(notifyUpdate = enable)
        }
    }

    suspend fun enableUnstableUpdates(enable: Boolean) {
        dataStore.updateData { settings ->
            settings.copy(unstableUpdate = enable)
        }
    }

    suspend fun setTheme(theme: Theme) {
        dataStore.updateData { settings ->
            settings.copy(theme = theme)
        }
    }

    suspend fun setDynamicTheme(enable: Boolean) {
        dataStore.updateData { settings ->
            settings.copy(dynamicTheme = enable)
        }
    }

    suspend fun setInstallerType(installerType: InstallerType) {
        dataStore.updateData { settings ->
            settings.copy(installerType = installerType)
        }
    }

    suspend fun setAutoUpdate(allow: Boolean) {
        dataStore.updateData { settings ->
            settings.copy(autoUpdate = allow)
        }
    }

    suspend fun setAutoSync(autoSync: AutoSync) {
        dataStore.updateData { settings ->
            settings.copy(autoSync = autoSync)
        }
    }

    suspend fun setSortOrder(sortOrder: SortOrder) {
        dataStore.updateData { settings ->
            settings.copy(sortOrder = sortOrder)
        }
    }

    suspend fun setProxyType(proxyType: ProxyType) {
        dataStore.updateData { settings ->
            settings.copy(proxy = settings.proxy.update(newType = proxyType))
        }
    }

    suspend fun setProxyHost(proxyHost: String) {
        dataStore.updateData { settings ->
            settings.copy(proxy = settings.proxy.update(newHost = proxyHost))
        }
    }

    suspend fun setProxyPort(proxyPort: Int) {
        dataStore.updateData { settings ->
            settings.copy(proxy = settings.proxy.update(newPort = proxyPort))
        }
    }

    suspend fun setCleanUpInterval(interval: Duration) {
        dataStore.updateData { settings ->
            settings.copy(cleanUpInterval = interval)
        }
    }

    suspend fun setCleanupInstant() {
        dataStore.updateData { settings ->
            settings.copy(lastCleanup = Clock.System.now())
        }
    }

    suspend fun setHomeScreenSwiping(value: Boolean) {
        dataStore.updateData { settings ->
            settings.copy(homeScreenSwiping = value)
        }
    }

    suspend fun toggleFavourites(packageName: String) {
        dataStore.updateData { settings ->
            val newSet = settings.favouriteApps.updateAsMutable {
                if (!add(packageName)) remove(packageName)
            }
            settings.copy(favouriteApps = newSet)
        }
    }
}
