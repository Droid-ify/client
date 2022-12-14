package com.looker.core.model.new

data class Repo(
	val id: Long,
	val enabled: Boolean,
	val address: String,
	val name: String,
	val description: String,
	val mirrors: List<String>
)
