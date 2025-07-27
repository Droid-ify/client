package com.looker.droidify.data.model

data class Repo(
    val id: Int,
    val enabled: Boolean,
    val address: String,
    val name: String,
    val description: String,
    val fingerprint: Fingerprint?,
    val authentication: Authentication?,
    val versionInfo: VersionInfo,
    val mirrors: List<String>,
) {
    val shouldAuthenticate = authentication != null

    fun update(fingerprint: Fingerprint, timestamp: Long? = null, etag: String? = null): Repo {
        return copy(
            fingerprint = fingerprint,
            versionInfo = timestamp?.let { VersionInfo(timestamp = it, etag = etag) }
                ?: versionInfo,
        )
    }
}

data class AntiFeature(
    val id: Long,
    val name: String,
    val icon: String = "",
    val description: String = "",
)

data class Category(
    val id: Long,
    val name: String,
    val icon: String = "",
    val description: String = "",
)

data class Authentication(
    val username: String,
    val password: String,
)

data class VersionInfo(
    val timestamp: Long,
    val etag: String?,
)
