package com.looker.core.data.fdroid.model

import com.looker.core.database.model.RepoEntity
import kotlinx.serialization.Serializable

@Serializable
data class RepoDto(
	val address: String = "",
	val name: String = "",
	val description: String = "",
	val timestamp: Long = 0L,
	val version: Int = -1,
	val mirrors: List<String> = emptyList()
)

fun RepoDto.toEntity(
	fingerPrint: String,
	etag: String,
	username: String,
	password: String
): RepoEntity = RepoEntity(
	timestamp = timestamp,
	version = version,
	name = name,
	description = description,
	address = address,
	// It is obviously enabled that's why we are syncing
	enabled = true,
	fingerprint = fingerPrint,
	etag = etag,
	username = username,
	password = password,
	mirrors = mirrors
)