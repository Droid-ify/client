package com.looker.droidify.data.local.model

import androidx.room.Entity
import androidx.room.Index
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

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
    val repository: String,
    val apkUrl: String,
    val packageName: String,
    val versionCode: Int,
    val versionName: String,
    val tag: String,
    val commit: String,
    val timestamp: Long,
    val reproducible: Boolean?,
    val error: String?,
)

@Serializable
class RBData(
    val repository: String,
    val tag: String,
    val commit: String,
    val timestamp: Long,
    val reproducible: Boolean?,
    val error: String?,
    @SerialName("appid")
    val appId: String,
    @SerialName("apk_url")
    val apkUrl: String,
    @SerialName("version_code")
    val versionCode: Int,
    @SerialName("version_name")
    val versionName: String,
)

private fun RBData.toEntity(hash: String): RBLogEntity = RBLogEntity(
    hash = hash,
    repository = repository,
    apkUrl = apkUrl,
    packageName = appId,
    versionCode = versionCode,
    versionName = versionName,
    tag = tag,
    commit = commit,
    timestamp = timestamp,
    reproducible = reproducible,
    error = error,
)

fun Map<String, List<RBData>>.toLogs(): List<RBLogEntity> {
    return this.flatMap { (hash, data) ->
        data.map { it.toEntity(hash) }
    }
}
