package com.looker.core.datastore

import androidx.datastore.core.Serializer
import com.looker.core.datastore.model.AutoSync
import com.looker.core.datastore.model.InstallerType
import com.looker.core.datastore.model.ProxyPreference
import com.looker.core.datastore.model.SortOrder
import com.looker.core.datastore.model.Theme
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlinx.datetime.Instant
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import kotlinx.serialization.json.encodeToStream

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
    val homeScreenSwiping: Boolean = true
)

@OptIn(ExperimentalSerializationApi::class)
object SettingsSerializer : Serializer<Settings> {

    private val json = Json { encodeDefaults = true }

    override val defaultValue: Settings = Settings()

    override suspend fun readFrom(input: InputStream): Settings {
        return try {
            json.decodeFromStream(input)
        } catch (e: SerializationException) {
            e.printStackTrace()
            defaultValue
        }
    }

    override suspend fun writeTo(t: Settings, output: OutputStream) {
        try {
            json.encodeToStream(t, output)
        } catch (e: SerializationException) {
            e.printStackTrace()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }
}
