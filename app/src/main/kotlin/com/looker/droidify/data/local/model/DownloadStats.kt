package com.looker.droidify.data.local.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import com.looker.droidify.utility.common.isoDateToInt
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import java.io.InputStream

@Entity(
    tableName = "downloadStats",
    primaryKeys = ["packageName", "date", "client", "source"],
    indices = [
        Index(value = ["packageName", "date", "client", "source"], unique = true),
        Index(value = ["packageName", "date"]),
        Index(value = ["packageName"]),
        Index(value = ["client"]),
        Index(value = ["date"]),
    ],
)
data class DownloadStats(
    @ColumnInfo(name = "packageName")
    val packageName: String,
    // formatted from iso as YYYYMMDD
    val date: Int,
    val client: String,
    val source: String,
    val count: Long,
)

@Serializable
data class ClientCounts(
    @SerialName("F-Droid")
    val fDroid: Long = 0,
    @SerialName("F-Droid Classic")
    val fDroidClassic: Long = 0,
    @SerialName("Neo Store")
    val neoStore: Long = 0,
    @SerialName("Droid-ify")
    val driodify: Long = 0,
    @SerialName("Flicky")
    val flicky: Long = 0,
    @SerialName("_total")
    val total: Long = 0,
    @SerialName("_unknown")
    val unknown: Long = 0,
)

@Serializable
class DownloadStatsData {
    fun toJSON() = Json.encodeToString(this)

    companion object {
        private val jsonConfig = Json {
            ignoreUnknownKeys = true
            prettyPrint = true
        }

        fun fromJson(json: String) =
            jsonConfig.decodeFromString<Map<String, Map<String, ClientCounts>>>(json)

        fun fromStream(inst: InputStream) =
            jsonConfig.decodeFromStream<Map<String, Map<String, ClientCounts>>>(inst)
    }
}

fun Map<String, Map<String, ClientCounts>>.toDownloadStats(): Set<DownloadStats> {
    val result = mutableSetOf<DownloadStats>()

    for ((isoDate, sourceMap) in this) {
        for ((packageName, clientCounts) in sourceMap) {
            // Add a row only when the count is > 0
            fun addIfPositive(client: String, count: Long) {
                if (count > 0) {
                    result += DownloadStats(
                        packageName = packageName,
                        date = isoDate.isoDateToInt(),
                        client = client,
                        source = "izzyOnDroid",
                        count = count,
                    )
                }
            }

            // Map each client field to its corresponding name used in the DB
            addIfPositive("F-Droid", clientCounts.fDroid)
            addIfPositive("F-Droid Classic", clientCounts.fDroidClassic)
            addIfPositive("Neo Store", clientCounts.neoStore)
            addIfPositive("Droid-ify", clientCounts.driodify)
            addIfPositive("Flicky", clientCounts.flicky)
            addIfPositive("_total", clientCounts.total)
            addIfPositive("_unknown", clientCounts.unknown)
        }
    }

    return result
}
