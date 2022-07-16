package com.looker.core_database.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
data class Apk(
	val packageName: String,
	val size: Long,
	val apkName: String,
	val added: Long,
	val versionCode: Long,
	val versionName: String,
	val hash: String,
	val hashType: String,
	val signature: String,
	val signer: String,
	val srcName: String,
	val minSdk: Int,
	val maxSdk: Int,
	val targetSdk: String,
	val permissions: List<String>
) {
	fun toJson() = Json.encodeToString(this)

	companion object {
		fun fromJson(builder: Json, json: String) = builder.decodeFromString<Apk>(json)
	}
}