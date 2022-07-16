package com.looker.core_database.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "repo_table")
data class Repo(
	@PrimaryKey(autoGenerate = true)
	val repoId: Long,
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
