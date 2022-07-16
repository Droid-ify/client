package com.looker.core_database.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
data class Localized(
	val locale: String,
	val name: String,
	val icon: String,
	val summary: String,
	val description: String,
	val whatsNew: String,
	val promoGraphics: String,
	val featureGraphics: String,
	val tvBanner: String,
	val video: String,
	val phoneScreenshots: List<String>,
	val sevenInchScreenshots: List<String>,
	val tenInchScreenshots: List<String>,
	val tvScreenshots: List<String>,
	val wearScreenshots: List<String>,
) {
	fun toJson() = Json.encodeToString(this)

	companion object {
		fun fromJson(builder: Json, json: String) = builder.decodeFromString<Localized>(json)
	}
}