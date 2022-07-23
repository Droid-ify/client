package com.looker.core_database.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Entity(tableName = "repo_table")
@Serializable
data class Repo(
	@PrimaryKey(autoGenerate = true)
	val repoId: Long = 0L,
	val enabled: Boolean = false,
	val deleted: Boolean = false,
	@SerialName("address") val address: String,
	@SerialName("mirrors") val mirrors: List<String>,
	@SerialName("name") val name: String,
	@SerialName("description") val description: String,
	@SerialName("version") val version: Int,
	@SerialName("finger") val fingerprint: String,
	@SerialName("apkName") val entityTag: String,
	@SerialName("timestamp") val timestamp: Long
)
