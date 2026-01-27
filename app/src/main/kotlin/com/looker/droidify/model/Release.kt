package com.looker.droidify.model

import androidx.core.net.toUri

data class Release(
    @JvmField
    val selected: Boolean,
    @JvmField
    val version: String,
    @JvmField
    val versionCode: Long,
    @JvmField
    val added: Long,
    @JvmField
    val size: Long,
    @JvmField
    val minSdkVersion: Int,
    @JvmField
    val targetSdkVersion: Int,
    @JvmField
    val maxSdkVersion: Int,
    @JvmField
    val source: String,
    @JvmField
    val release: String,
    @JvmField
    val hash: String,
    @JvmField
    val hashType: String,
    @JvmField
    val signature: String,
    @JvmField
    val obbMain: String,
    @JvmField
    val obbMainHash: String,
    @JvmField
    val obbMainHashType: String,
    @JvmField
    val obbPatch: String,
    @JvmField
    val obbPatchHash: String,
    @JvmField
    val obbPatchHashType: String,
    @JvmField
    val permissions: List<String>,
    @JvmField
    val features: List<String>,
    @JvmField
    val platforms: List<String>,
    @JvmField
    val incompatibilities: List<Incompatibility>,
) {
    sealed class Incompatibility {
        object MinSdk : Incompatibility()
        object MaxSdk : Incompatibility()
        object Platform : Incompatibility()
        class Feature(val feature: String) : Incompatibility()
    }

    val identifier: String
        get() = "$versionCode.$hash"

    fun getDownloadUrl(repository: Repository): String {
        return repository.address.toUri().buildUpon().appendPath(release).build().toString()
    }

    val cacheFileName: String
        get() = "${hash.replace('/', '-')}.apk"
}
