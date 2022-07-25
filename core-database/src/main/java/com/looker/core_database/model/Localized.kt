package com.looker.core_database.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Localized(
	@SerialName("name") val name: String = "",
	@SerialName("icon") val icon: String = "",
	@SerialName("summary") val summary: String = "",
	@SerialName("description") val description: String = "",
	@SerialName("whatsNew") val whatsNew: String = "",
	@SerialName("promoGraphic") val promoGraphics: String = "",
	@SerialName("featureGraphic") val featureGraphics: String = "",
	@SerialName("tvBanner") val tvBanner: String = "",
	@SerialName("video") val video: String = "",
	@SerialName("phoneScreenshots") val phoneScreenshots: List<String> = emptyList(),
	@SerialName("sevenInchScreenshots") val sevenInchScreenshots: List<String> = emptyList(),
	@SerialName("tenInchScreenshots") val tenInchScreenshots: List<String> = emptyList(),
	@SerialName("tvScreenshots") val tvScreenshots: List<String> = emptyList(),
	@SerialName("wearScreenshots") val wearScreenshots: List<String> = emptyList()
)