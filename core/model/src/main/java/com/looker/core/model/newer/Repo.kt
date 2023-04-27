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
	val mirrors: List<String>
)

data class Authentication(
	val username: String,
	val password: String
)

data class VersionInfo(
	val etag: String,
	val version: Int,
	val timestamp: Long
)