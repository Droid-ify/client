package com.looker.droidify.data.local.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

data class RBLog(
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

enum class Reproducible { NO_DATA, UNKNOWN, TRUE, FALSE }

fun RBLog?.toReproducible(): Reproducible = when {
    this == null -> Reproducible.NO_DATA
    this.reproducible == true -> Reproducible.TRUE
    this.reproducible == false -> Reproducible.FALSE
    else -> Reproducible.UNKNOWN // this.reproducible == null
}

private fun RBData.toEntity(hash: String): RBLog = RBLog(
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

fun Map<String, List<RBData>>.toLogs(): List<RBLog> {
    return this.flatMap { (hash, data) ->
        data.map { it.toEntity(hash) }
    }
}
