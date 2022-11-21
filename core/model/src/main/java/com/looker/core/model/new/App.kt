package com.looker.core.model.new

data class App(
	val categories: List<String>,
	val antiFeatures: List<String>,
	val summary: String,
	val description: String,
	val changelog: String,
	val translation: String,
	val issueTracker: String,
	val sourceCode: String,
	val binaries: String,
	val name: String,
	val suggestedVersionName: String,
	val suggestedVersionCode: String,
	val license: String,
	val webSite: String,
	val added: Long,
	val icon: String,
	val packageName: String,
	val lastUpdated: Long,
	val author: Author,
	val donation: List<Donate>,
	val localized: Map<String, Localized>,
	val allowedAPKSigningKeys: List<String>
)

data class Author(val name: String, val email: String, val web: String, val phone: String)

sealed interface Donate {
	@JvmInline
	value class Regular(val url: String) : Donate

	@JvmInline
	value class Bitcoin(val address: String) : Donate

	@JvmInline
	value class Litecoin(val address: String) : Donate

	@JvmInline
	value class Flattr(val id: String) : Donate

	@JvmInline
	value class Liberapay(val id: String) : Donate

	@JvmInline
	value class OpenCollective(val id: String) : Donate
}

data class AppMinimal(
	val name: String,
	val summary: String,
	val icon: String
)

fun App.minimal(locale: String? = null): AppMinimal = if (locale == null) {
	AppMinimal(
		name = name,
		summary = summary,
		icon = icon
	)
} else {
	val localized = localized[locale]
	AppMinimal(
		name = localized?.name ?: name,
		summary = localized?.summary ?: summary,
		icon = localized?.icon ?: icon
	)
}