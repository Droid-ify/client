package com.looker.core.model.newer

data class App(
	val repoId: Long,
	val categories: List<String>,
	val antiFeatures: List<String>,
	val links: Links,
	val metadata: Metadata,
	val author: Author,
	val donation: Set<Donate>,
	val localized: Map<String, Localized>,
	val packages: List<Package>
)

data class Metadata(
	val packageName: PackageName,
	val icon: String,
	val name: String,
	val description: String,
	val summary: String,
	val changelog: String,
	val added: Long,
	val lastUpdated: Long,
	val suggestedVersionName: String,
	val suggestedVersionCode: Int
)

data class Links(
	val issueTracker: String,
	val license: String,
	val sourceCode: String,
	val translation: String,
	val webSite: String
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
	value class OpenCollective(val id: String) : Donate

	data class Liberapay(val id: String, val address: String) : Donate
}

data class AppMinimal(
	val name: String,
	val summary: String,
	val icon: String
)

fun App.minimal(locale: String? = null): AppMinimal = if (locale == null) {
	AppMinimal(
		name = metadata.name,
		summary = metadata.summary,
		icon = metadata.icon
	)
} else {
	val localized = localized[locale]
	AppMinimal(
		name = localized?.name ?: metadata.name,
		summary = localized?.summary ?: metadata.summary,
		icon = localized?.icon ?: metadata.icon
	)
}