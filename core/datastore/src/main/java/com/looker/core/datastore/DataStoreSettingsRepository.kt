package com.looker.core.datastore

import android.net.Uri
import android.util.Log
import androidx.datastore.core.DataStore
import com.looker.core.common.Exporter
import com.looker.core.common.extension.updateAsMutable
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
import kotlinx.datetime.Clock
import java.io.IOException
import kotlin.time.Duration

class DataStoreSettingsRepository(
    private val dataStore: DataStore<Settings>,
    private val exporter: Exporter<Settings>
) : SettingsRepository {
    private companion object {
        const val TAG: String = "SettingsRepository"
    }

    override val data: Flow<Settings> = dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                Log.e(TAG, "Error reading preferences.", exception)
            } else {
                throw exception
            }
        }

    inline fun <T> get(crossinline block: suspend Settings.() -> T): Flow<T> =
        data.map(block).distinctUntilChanged()

    override suspend fun getInitial() =
        dataStore.data.first()

    override suspend fun setLanguage(language: String) {
        dataStore.updateData { settings ->
            settings.copy(language = language)
        }
    }

    override suspend fun enableIncompatibleVersion(enable: Boolean) {
        dataStore.updateData { settings ->
            settings.copy(incompatibleVersions = enable)
        }
    }

    override suspend fun enableNotifyUpdates(enable: Boolean) {
        dataStore.updateData { settings ->
            settings.copy(notifyUpdate = enable)
        }
    }

    override suspend fun enableUnstableUpdates(enable: Boolean) {
        dataStore.updateData { settings ->
            settings.copy(unstableUpdate = enable)
        }
    }

    override suspend fun setTheme(theme: Theme) {
        dataStore.updateData { settings ->
            settings.copy(theme = theme)
        }
    }

    override suspend fun setDynamicTheme(enable: Boolean) {
        dataStore.updateData { settings ->
            settings.copy(dynamicTheme = enable)
        }
    }

    override suspend fun setInstallerType(installerType: InstallerType) {
        dataStore.updateData { settings ->
            settings.copy(installerType = installerType)
        }
    }

    override suspend fun setAutoUpdate(allow: Boolean) {
        dataStore.updateData { settings ->
            settings.copy(autoUpdate = allow)
        }
    }

    override suspend fun setAutoSync(autoSync: AutoSync) {
        dataStore.updateData { settings ->
            settings.copy(autoSync = autoSync)
        }
    }

    override suspend fun setSortOrder(sortOrder: SortOrder) {
        dataStore.updateData { settings ->
            settings.copy(sortOrder = sortOrder)
        }
    }

    override suspend fun setProxyType(proxyType: ProxyType) {
        dataStore.updateData { settings ->
            settings.copy(proxy = settings.proxy.update(newType = proxyType))
        }
    }

    override suspend fun setProxyHost(proxyHost: String) {
        dataStore.updateData { settings ->
            settings.copy(proxy = settings.proxy.update(newHost = proxyHost))
        }
    }

    override suspend fun setProxyPort(proxyPort: Int) {
        dataStore.updateData { settings ->
            settings.copy(proxy = settings.proxy.update(newPort = proxyPort))
        }
    }

    override suspend fun setCleanUpInterval(interval: Duration) {
        dataStore.updateData { settings ->
            settings.copy(cleanUpInterval = interval)
        }
    }

    override suspend fun setCleanupInstant() {
        dataStore.updateData { settings ->
            settings.copy(lastCleanup = Clock.System.now())
        }
    }

    override suspend fun setHomeScreenSwiping(value: Boolean) {
        dataStore.updateData { settings ->
            settings.copy(homeScreenSwiping = value)
        }
    }

    override suspend fun exportSettings(target: Uri) {
        val currentSettings = getInitial()
        exporter.export(currentSettings, target)
    }

    override suspend fun importSettings(target: Uri) {
        val importedSettings = exporter.import(target)
        val updatedFavorites = importedSettings.favouriteApps +
            getInitial().favouriteApps
        val updatedSettings = importedSettings.copy(favouriteApps = updatedFavorites)
        dataStore.updateData { updatedSettings }
    }

    override suspend fun toggleFavourites(packageName: String) {
        dataStore.updateData { settings ->
            val newSet = settings.favouriteApps.updateAsMutable {
                if (!add(packageName)) remove(packageName)
            }
            settings.copy(favouriteApps = newSet)
        }
    }
}
