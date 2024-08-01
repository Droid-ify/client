package com.looker.sync.fdroid.v1.model

import kotlinx.serialization.Serializable

@Serializable
data class Localized(
    val icon: String? = null,
    val name: String? = null,
    val description: String? = null,
    val summary: String? = null,
    val featureGraphic: String? = null,
    val phoneScreenshots: List<String>? = null,
    val promoGraphic: String? = null,
    val sevenInchScreenshots: List<String>? = null,
    val tenInchScreenshots: List<String>? = null,
    val tvBanner: String? = null,
    val tvScreenshots: List<String>? = null,
    val video: String? = null,
    val wearScreenshots: List<String>? = null,
    val whatsNew: String? = null,
)
