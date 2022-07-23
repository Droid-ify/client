package com.looker.core_database.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
data class Localized(
	@SerialName("name") val name: String,
	@SerialName("icon") val icon: String,
	@SerialName("summary") val summary: String,
	@SerialName("description") val description: String,
	@SerialName("whatsNew") val whatsNew: String,
	@SerialName("promoGraphic") val promoGraphics: String,
	@SerialName("featureGraphic") val featureGraphics: String,
	@SerialName("tvBanner") val tvBanner: String,
	@SerialName("video") val video: String,
	@SerialName("phoneScreenshots") val phoneScreenshots: List<String>,
	@SerialName("sevenInchScreenshots") val sevenInchScreenshots: List<String>,
	@SerialName("tenInchScreenshots") val tenInchScreenshots: List<String>,
	@SerialName("tvScreenshots") val tvScreenshots: List<String>,
	@SerialName("wearScreenshots") val wearScreenshots: List<String>,
) {
	fun toJson() = Json.encodeToString(this)

	companion object {
		fun fromJson(builder: Json, json: String) = builder.decodeFromString<Localized>(json)
	}
}