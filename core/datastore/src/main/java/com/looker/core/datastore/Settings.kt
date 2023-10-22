package com.looker.core.datastore

import com.looker.core.datastore.model.AutoSync
import com.looker.core.datastore.model.InstallerType
import com.looker.core.datastore.model.ProxyPreference
import com.looker.core.datastore.model.SortOrder
import com.looker.core.datastore.model.Theme
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours

@Serializable
data class Settings(
    val language: String = "system",
    val incompatibleVersions: Boolean = false,
    val notifyUpdate: Boolean = true,
    val unstableUpdate: Boolean = false,
    val theme: Theme = Theme.SYSTEM,
    val dynamicTheme: Boolean = false,
    val installerType: InstallerType = InstallerType.Default,
    val autoUpdate: Boolean = false,
    val autoSync: AutoSync = AutoSync.WIFI_ONLY,
    val sortOrder: SortOrder = SortOrder.UPDATED,
    val proxy: ProxyPreference = ProxyPreference(),
    val cleanUpInterval: Duration = 12.hours,
    val lastCleanup: Instant? = null,
    val favouriteApps: Set<String> = emptySet(),
    val homeScreenSwiping: Boolean = true,
)
