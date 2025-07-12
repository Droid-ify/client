package com.looker.droidify.data.local.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Entity(
    tableName = "rblog",
    primaryKeys = ["hash", "packageName", "timestamp"],
    indices = [
        Index(value = ["hash", "packageName", "timestamp"], unique = true),
        Index(value = ["packageName", "versionCode", "reproducible"]),
        Index(value = ["packageName", "hash", "reproducible"]),
    ],
)
data class RBLogEntity(
    val hash: String,
    override val repository: String,
    override val apk_url: String,
    @ColumnInfo(name = "packageName")
    override val appid: String,
    @ColumnInfo(name = "versionCode")
    override val version_code: Int,
    @ColumnInfo(name = "versionName")
    override val version_name: String,
    override val tag: String,
    override val commit: String,
    override val timestamp: Long,
    override val reproducible: Boolean?,
    override val error: String?,
) : RBData(
    repository = repository,
    apk_url = apk_url,
    appid = appid,
    version_code = version_code,
    version_name = version_name,
    tag = tag,
    commit = commit,
    timestamp = timestamp,
    reproducible = reproducible,
    error = error,
)

@Serializable
open class RBData(
    open val repository: String,
    open val apk_url: String,
    open val appid: String,
    open val version_code: Int,
    open val version_name: String,
    open val tag: String,
    open val commit: String,
    open val timestamp: Long,
    open val reproducible: Boolean?,
    open val error: String?,
)

@Serializable
class RBLogs {
    companion object {
        private val jsonConfig = Json { ignoreUnknownKeys = true }
        fun fromJson(json: String) = jsonConfig.decodeFromString<Map<String, List<RBData>>>(json)
    }
}
