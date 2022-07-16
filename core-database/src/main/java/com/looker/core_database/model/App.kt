package com.looker.core_database.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Entity(tableName = "app_table")
data class App(
	@PrimaryKey(autoGenerate = true)
	val packageName: String,
	val repoId: Long,
	val icon: String,
	val license: String,
	val suggestedVersionName: String,
	val website: String,
	val sourceCode: String,
	val changelog: String,
	val issueTracker: String,
	val translation: String,
	val added: Long,
	val lastUpdated: Long,
	val suggestedVersionCode: Long,
	val author: Author,
	val categories: List<String>,
	val antiFeatures: List<String>,
	val localized: List<Localized>,
	val donate: List<Donate>,
	val apks: List<Apk>
)

@Serializable
sealed class Donate(val id: String) {
	@Serializable
	data class Regular(val url: String) : Donate(url)

	@Serializable
	data class Bitcoin(val address: String) : Donate(address)

	@Serializable
	data class LiteCoin(val address: String) : Donate(address)

	@Serializable
	data class Flattr(val userId: String) : Donate(userId)

	@Serializable
	data class LiberaPay(val userId: String) : Donate(userId)

	@Serializable
	data class OpenCollective(val userId: String) : Donate(userId)

	fun toJson() = Json.encodeToString(this)

	companion object {
		fun fromJson(builder: Json, json: String) = builder.decodeFromString<Donate>(json)
	}
}

@Serializable
data class Author(
	val name: String,
	val website: String,
	val email: String
) {
	fun toJson() = Json.encodeToString(this)

	companion object {
		fun fromJson(builder: Json, json: String) = builder.decodeFromString<Author>(json)
	}
}