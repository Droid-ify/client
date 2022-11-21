package com.looker.core.model.new

data class Repo(
	val enabled: Boolean = false,
	val address: String,
	val name: String,
	val description: String,
	// Remove
	val version: Int,
	val timestamp: Long,
	val fingerprint: String,
	val entityTag: String,
	// Remove
	val maxAge: Int? = null,
	val mirrors: List<String>
)
