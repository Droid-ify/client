package com.looker.core.database.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.looker.core.model.newer.Repo

@Entity(tableName = "repos")
data class RepoEntity(
	@PrimaryKey(autoGenerate = true)
	val id: Long = 0L,
	val enabled: Boolean,
	val fingerprint: String,
	val etag: String,
	val username: String,
	val password: String,
	val address: String,
	val mirrors: List<String>,
	val name: String,
	val description: String,
	val version: Int,
	val timestamp: Long
)

fun RepoEntity.toExternalModel(): Repo = Repo(
	id = id,
	enabled = enabled,
	address = address,
	name = name,
	description = description,
	fingerprint = fingerprint,
	username = username,
	password = password,
	mirrors = mirrors
)