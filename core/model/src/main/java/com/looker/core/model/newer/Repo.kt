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
	val mirrors: List<String>
)
