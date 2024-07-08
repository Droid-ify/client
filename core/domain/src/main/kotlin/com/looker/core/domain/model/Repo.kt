package com.looker.core.domain.model

data class Repo(
    val id: Long,
    val enabled: Boolean,
    val address: String,
    val name: String,
    val description: String,
    val fingerprint: Fingerprint?,
    val authentication: Authentication,
    val versionInfo: VersionInfo,
    val mirrors: List<String>,
    val antiFeatures: List<AntiFeature>,
    val categories: List<Category>
) {
    val shouldAuthenticate =
        authentication.username.isNotEmpty() && authentication.password.isNotEmpty()

    fun update(fingerprint: Fingerprint, timestamp: Long? = null, etag: String? = null): Repo {
        return copy(
            fingerprint = fingerprint,
            versionInfo = timestamp?.let { VersionInfo(timestamp = it, etag = etag) } ?: versionInfo
        )
    }
}

val String.isOnion: Boolean
    get() = endsWith(".onion")

data class AntiFeature(
    val name: String,
    val icon: String = "",
    val description: String = ""
)

data class Category(
    val name: String,
    val icon: String = "",
    val description: String = ""
)

data class Authentication(
    val username: String,
    val password: String
)

data class VersionInfo(
    val timestamp: Long,
    val etag: String?
)
