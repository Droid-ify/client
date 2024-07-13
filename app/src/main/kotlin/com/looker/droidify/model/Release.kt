package com.looker.droidify.model

import android.net.Uri

data class Release(
    val selected: Boolean,
    val version: String,
    val versionCode: Long,
    val added: Long,
    val size: Long,
    val minSdkVersion: Int,
    val targetSdkVersion: Int,
    val maxSdkVersion: Int,
    val source: String,
    val release: String,
    val hash: String,
    val hashType: String,
    val signature: String,
    val obbMain: String,
    val obbMainHash: String,
    val obbMainHashType: String,
    val obbPatch: String,
    val obbPatchHash: String,
    val obbPatchHashType: String,
    val permissions: List<String>,
    val features: List<String>,
    val platforms: List<String>,
    val incompatibilities: List<Incompatibility>
) {
    sealed class Incompatibility {
        object MinSdk : Incompatibility()
        object MaxSdk : Incompatibility()
        object Platform : Incompatibility()
        data class Feature(val feature: String) : Incompatibility()
    }

    val identifier: String
        get() = "$versionCode.$hash"

    fun getDownloadUrl(repository: Repository): String {
        return Uri.parse(repository.address).buildUpon().appendPath(release).build().toString()
    }

    val cacheFileName: String
        get() = "${hash.replace('/', '-')}.apk"
}
