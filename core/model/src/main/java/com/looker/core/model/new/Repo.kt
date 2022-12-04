package com.looker.core.model.new

data class Repo(
	val id: Long,
	val enabled: Boolean = false,
	val address: String,
	val name: String,
	val description: String,
	// Remove
	val version: Int,
	val timestamp: Long,
	val fingerprint: String,
	val entityTag: String,
	val mirrors: List<String>
)
