package com.looker.core.model.newer

data class Repo(
	val id: Long,
	val enabled: Boolean,
	val address: String,
	val name: String,
	val description: String,
	val fingerprint: String,
	val username: String,
	val password: String,
	val etag: String,
	val version: Int,
	val timestamp: Long,
	val mirrors: List<String>
)
