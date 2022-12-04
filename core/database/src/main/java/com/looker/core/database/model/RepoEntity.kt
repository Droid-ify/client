package com.looker.core.database.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.looker.core.model.new.Repo

@Entity(tableName = "repos")
data class RepoEntity(
	@PrimaryKey(autoGenerate = true)
	val id: Long = 0L,
	val enabled: Boolean,
	val fingerprint: String,
	val entityTag: String,
	val address: String,
	val mirrors: List<String>,
	val name: String,
	val description: String,
	val version: Int,
	val timestamp: Long,
	val icon: String
)

fun RepoEntity.toExternalModel(): Repo = Repo(
	id = id,
	enabled = enabled,
	address = address,
	name = name,
	description = description,
	version = version,
	timestamp = timestamp,
	fingerprint = fingerprint,
	entityTag = entityTag,
	mirrors = mirrors
)