package com.looker.core_model.new_model

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
)
