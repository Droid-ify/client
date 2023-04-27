package com.looker.core.model.newer

data class App(
	val repoId: Long,
	val categories: List<String>,
	val antiFeatures: Set<AntiFeatures>,
	val links: Links,
	val metadata: Metadata,
	val author: Author,
	val screenshots: Screenshots,
	val graphics: Graphics,
	val donation: Donation,
	val preferredSigner: String = "",
	val packages: List<Package>
)

sealed interface AntiFeatures {
	object Ads : AntiFeatures
	object Debug : AntiFeatures
	object UnsafeSigning : AntiFeatures
	object Vulnerable : AntiFeatures
	object SourceUnavailable : AntiFeatures
	object Promotion : AntiFeatures
	object CopyrightedAssets : AntiFeatures
	object NonFreeLibraries : AntiFeatures
	object NonFreeNetwork : AntiFeatures
	object Tracking : AntiFeatures
	object InAppPurchase : AntiFeatures
	data class Unknown(val id: String) : AntiFeatures
}

data class Author(
	val name: String,
	val email: String,
	val web: String,
	val phone: String
)

data class Donation(
	val regularUrl: String? = null,
	val bitcoinAddress: String? = null,
	val flattrId: String? = null,
	val liteCoinAddress: String? = null,
	val openCollectiveId: String? = null,
	val librePayId: String? = null,
	val librePayAddress: String? = null
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
	val name: String,
	val packageName: PackageName,
	val added: Long,
	val description: String,
	val icon: String,
	val lastUpdated: Long,
	val license: String,
	val suggestedVersionCode: Int,
	val suggestedVersionName: String,
	val summary: String,
	val whatsNew: String
)

data class Screenshots(
	val phone: List<String> = emptyList(),
	val sevenInch: List<String> = emptyList(),
	val tenInch: List<String> = emptyList(),
	val tv: List<String> = emptyList(),
	val wear: List<String> = emptyList()
)

data class AppMinimal(
	val name: String,
	val summary: String,
	val icon: String
)

fun App.minimal(): AppMinimal = AppMinimal(metadata.name, metadata.summary, metadata.icon)