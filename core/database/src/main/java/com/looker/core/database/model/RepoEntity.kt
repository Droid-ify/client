package com.looker.core.database.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.looker.core.model.newer.Authentication
import com.looker.core.model.newer.Repo
import com.looker.core.model.newer.VersionInfo

@Entity(tableName = "repos")
data class RepoEntity(
	@PrimaryKey(autoGenerate = true)
	val id: Long? = null,
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

fun Repo.toEntity(): RepoEntity = RepoEntity(
	id = id,
	enabled = enabled,
	address = address,
	name = name,
	description = description,
	fingerprint = fingerprint,
	username = authentication.username,
	password = authentication.password,
	etag = versionInfo.etag,
	version = versionInfo.version,
	timestamp = versionInfo.timestamp,
	mirrors = mirrors
)

fun RepoEntity.toExternalModel(): Repo = Repo(
	id = id!!,
	enabled = enabled,
	address = address,
	name = name,
	description = description,
	fingerprint = fingerprint,
	authentication = Authentication(username, password),
	versionInfo = VersionInfo(etag = etag, version = version, timestamp = timestamp),
	mirrors = mirrors
)