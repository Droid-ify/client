package com.looker.index.model.v1

import kotlinx.serialization.Serializable

@Serializable
data class AppV1(
	val categories: List<String> = emptyList(), // missing in wind repo
	val antiFeatures: List<String> = emptyList(),
	val summary: String? = null,
	val description: String? = null,
	val changelog: String? = null,
	val translation: String? = null,
	val issueTracker: String? = null,
	val sourceCode: String? = null,
	val binaries: String? = null,
	val name: String? = null,
	val authorName: String? = null,
	val authorEmail: String? = null,
	val authorWebSite: String? = null,
	val authorPhone: String? = null,
	val donate: String? = null,
	val liberapayID: String? = null,
	val liberapay: String? = null,
	val openCollective: String? = null,
	val bitcoin: String? = null,
	val litecoin: String? = null,
	val flattrID: String? = null,
	val suggestedVersionName: String? = null,
	val suggestedVersionCode: String? = null,
	val license: String,
	val webSite: String? = null,
	val added: Long? = null,
	val icon: String? = null,
	val packageName: String,
	val lastUpdated: Long? = null,
	val localized: Map<String, Localized>? = null,
	val allowedAPKSigningKeys: List<String>? = null
)

fun <T : Any> Map<String, Localized>.localizedData(
	transform: (Map.Entry<String, Localized>) -> T
): Map<String, T> = mapValues {
	transform(it)
}