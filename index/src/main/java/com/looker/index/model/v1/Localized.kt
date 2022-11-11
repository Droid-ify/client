package com.looker.index.model.v1

import kotlinx.serialization.Serializable

@Serializable
data class Localized(
	val description: String? = null,
	val name: String? = null,
	val icon: String? = null,
	val whatsNew: String? = null,
	val video: String? = null,
	val phoneScreenshots: List<String>? = null,
	val sevenInchScreenshots: List<String>? = null,
	val tenInchScreenshots: List<String>? = null,
	val wearScreenshots: List<String>? = null,
	val tvScreenshots: List<String>? = null,
	val featureGraphic: String? = null,
	val promoGraphic: String? = null,
	val tvBanner: String? = null,
	val summary: String? = null
)