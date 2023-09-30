package com.looker.core.model.newer

data class Repo(
	val id: Long,
	val enabled: Boolean,
	val address: String,
	val name: String,
	val description: String,
	val fingerprint: String,
	val authentication: Authentication,
	val versionInfo: VersionInfo,
	val mirrors: List<String>,
	val antiFeatures: List<AntiFeature>,
	val categories: List<Category>
) {
	val shouldAuthenticate =
		authentication.username.isNotEmpty() && authentication.password.isNotEmpty()

	fun update(fingerprint: String, lastModified: Long? = null, etag: String? = null): Repo {
		return copy(
			fingerprint = fingerprint,
			versionInfo = lastModified?.let { VersionInfo(timestamp = it) } ?: versionInfo
		)
	}
}

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
	val timestamp: Long
)