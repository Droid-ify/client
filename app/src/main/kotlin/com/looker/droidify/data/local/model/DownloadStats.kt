package com.looker.droidify.data.local.model

import androidx.room.Entity
import androidx.room.Index
import com.looker.droidify.sync.JsonParser
import java.io.InputStream
import java.util.*
import kotlin.time.ExperimentalTime
import kotlinx.serialization.ExperimentalSerializationApi
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
    val downloads: Long,
)

@Serializable
class DownloadStatsData(val stats: Map<String, Long>) {
    fun toDownloadStats(
        timestamp: Long,
        source: String = "IzzyOnDroid",
    ): List<DownloadStats> = stats.map { (packageName, downloads) ->
        DownloadStats(
            packageName = packageName,
            source = source,
            timestamp = timestamp,
            downloads = downloads,
        )
    }

    companion object {
        @OptIn(ExperimentalSerializationApi::class)
        fun fromStream(inst: InputStream) =
            DownloadStatsData(JsonParser.decodeFromStream<Map<String, Long>>(inst))

        @OptIn(ExperimentalTime::class)
        fun String.toEpochMillis(): Long {
            val parts = split("-")
            val year = parts[0].toInt() - 1900
            val month = (parts.getOrNull(1)?.toInt()?.minus(1)) ?: 0
            val date = parts.getOrNull(2)?.toInt() ?: 1
            return Date(year, month, date, 0, 0, 0).time
        }
    }
}
