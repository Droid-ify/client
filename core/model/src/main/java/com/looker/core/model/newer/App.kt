package com.looker.core.model.newer

data class App(
	val repoId: Long,
	val categories: List<String>,
	val antiFeatures: List<String>,
	val links: Links,
	val license: String,
	val metadata: Metadata,
	val author: Author,
	val screenshots: Screenshots,
	val graphics: Graphics,
	val donation: Set<Donate>,
	val packages: List<Package>
)

data class Author(
	val name: String,
	val email: String,
	val web: String,
	val phone: String
)

data class Graphics(
	val featureGraphic: String = "",
	val promoGraphic: String = "",
	val tvBanner: String = "",
	val video: String = ""
)

data class Links(
	val changelog: String = "",
	val issueTracker: String = "",
	val sourceCode: String = "",
	val translation: String = "",
	val webSite: String = ""
)

data class Metadata(
	val packageName: PackageName,
	val icon: String,
	val name: String,
	val description: String,
	val summary: String,
	val added: Long,
	val lastUpdated: Long,
	val whatsNew: String,
	val suggestedVersionName: String,
	val suggestedVersionCode: Int
)

data class Screenshots(
	val phone: List<String> = emptyList(),
	val sevenInch: List<String> = emptyList(),
	val tenInch: List<String> = emptyList(),
	val tv: List<String> = emptyList(),
	val wear: List<String> = emptyList()
)

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

fun App.minimal(): AppMinimal = AppMinimal(metadata.name, metadata.summary, metadata.icon)