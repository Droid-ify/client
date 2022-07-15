package com.looker.core_model.new_model

data class Repo(
	val address: String,
	val mirrors: List<String>,
	val name: String,
	val description: String,
	val version: Int,
	val enabled: Boolean,
	val fingerprint: String,
	val lastModified: String,
	val entityTag: String,
	val updated: Long,
	val timestamp: Long,
	val authentication: String
)
