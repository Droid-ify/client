package com.looker.droidify.data.local.model

import androidx.room.Entity
import androidx.room.Index
import com.looker.droidify.sync.JsonParser
import java.io.InputStream
import java.util.*
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.decodeFromStream

@Entity(
    tableName = "download_stats",
    primaryKeys = ["packageName", "source", "timestamp"],
    indices = [Index("packageName"), Index("timestamp")]
)
data class DownloadStats(
    val packageName: String,
    val source: String,
    val timestamp: Long,
    val fDroid: Long,
    val fDroidClassic: Long,
    val neoStore: Long,
    val droidify: Long,
    val flicky: Long,
    val unknown: Long,
)

@Serializable
class ClientCounts(
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
class DownloadStatsData(val stats: Map<String, ClientCounts>) {
    fun toDownloadStats(
        timestamp: Long,
        source: String = "izzyOnDroid",
    ): List<DownloadStats> = buildList {
        for ((packageName, clientCounts) in stats) {
            add(
                DownloadStats(
                    packageName = packageName,
                    source = source,
                    timestamp = timestamp,
                    fDroid = clientCounts.fDroid,
                    fDroidClassic = clientCounts.fDroidClassic,
                    neoStore = clientCounts.neoStore,
                    droidify = clientCounts.driodify,
                    flicky = clientCounts.flicky,
                    unknown = clientCounts.unknown,
                )
            )
        }

    }

    companion object {
        @OptIn(ExperimentalSerializationApi::class)
        fun fromStream(inst: InputStream) =
            DownloadStatsData(JsonParser.decodeFromStream<Map<String, ClientCounts>>(inst))

        @OptIn(ExperimentalSerializationApi::class)
        fun byDayFromStream(inst: InputStream) =
            JsonParser.decodeFromStream<Map<String, DownloadStatsData>>(inst)

        fun String.toEpochMillis(): Long {
            val parts = split("-")
            val year = parts[0].toInt()
            val month = parts.getOrNull(1)?.toInt() ?: 0
            val date = parts.getOrNull(2)?.toInt() ?: 0
            val calendar = Calendar.getInstance()
            calendar.set(year, month, date)
            return calendar.timeInMillis
        }
    }
}
