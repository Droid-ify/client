package com.looker.index.model.v1

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class RepoV1(
	val timestamp: Long,
	val version: Int,
	@SerialName("maxage")
	val maxAge: Int? = null,
	val name: String,
	val icon: String,
	val address: String,
	val description: String,
	val mirrors: List<String> = emptyList()
)