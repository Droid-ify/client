package com.looker.core.datastore

import android.net.Uri
import com.looker.core.datastore.model.AutoSync
import com.looker.core.datastore.model.InstallerType
import com.looker.core.datastore.model.ProxyType
import com.looker.core.datastore.model.SortOrder
import com.looker.core.datastore.model.Theme
import kotlinx.coroutines.flow.Flow
import kotlin.time.Duration

interface SettingsRepository {
    val data: Flow<Settings>

    suspend fun getInitial(): Settings

    suspend fun setLanguage(language: String)

    suspend fun enableIncompatibleVersion(enable: Boolean)

    suspend fun enableNotifyUpdates(enable: Boolean)

    suspend fun enableUnstableUpdates(enable: Boolean)

    suspend fun setTheme(theme: Theme)

    suspend fun setDynamicTheme(enable: Boolean)

    suspend fun setInstallerType(installerType: InstallerType)

    suspend fun setAutoUpdate(allow: Boolean)

    suspend fun setAutoSync(autoSync: AutoSync)

    suspend fun setSortOrder(sortOrder: SortOrder)

    suspend fun setProxyType(proxyType: ProxyType)

    suspend fun setProxyHost(proxyHost: String)

    suspend fun setProxyPort(proxyPort: Int)

    suspend fun setCleanUpInterval(interval: Duration)

    suspend fun setCleanupInstant()

    suspend fun setHomeScreenSwiping(value: Boolean)

    suspend fun exportSettings(target: Uri)

    suspend fun importSettings(target: Uri)

    suspend fun toggleFavourites(packageName: String)
}

