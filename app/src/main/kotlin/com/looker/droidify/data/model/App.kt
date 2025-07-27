package com.looker.droidify.data.model

data class App(
    val repoId: Long,
    val appId: Long,
    val categories: List<String>,
    val links: Links?,
    val metadata: Metadata,
    val author: Author?,
    val screenshots: Screenshots?,
    val graphics: Graphics?,
    val donation: Donation?,
    val preferredSigner: String = "",
    val packages: List<Package>?
)

data class Author(
    val id: Int,
    val name: String?,
    val email: String?,
    val phone: String?,
    val web: String?,
)

data class Donation(
    val regularUrl: List<String>? = null,
    val bitcoinAddress: String? = null,
    val flattrId: String? = null,
    val litecoinAddress: String? = null,
    val openCollectiveId: String? = null,
    val liberapayId: String? = null,
)

data class Graphics(
    val featureGraphic: String? = null,
    val promoGraphic: String? = null,
    val tvBanner: String? = null,
    val video: String? = null,
)

data class Links(
    val changelog: String? = null,
    val issueTracker: String? = null,
    val sourceCode: String? = null,
    val translation: String? = null,
    val webSite: String? = null,
)

data class Metadata(
    val name: String,
    val packageName: PackageName,
    val added: Long,
    val description: String,
    val icon: String,
    val lastUpdated: Long,
    val license: String,
    val suggestedVersionCode: Long,
    val suggestedVersionName: String,
    val summary: String
)

data class Screenshots(
    val phone: List<String> = emptyList(),
    val sevenInch: List<String> = emptyList(),
    val tenInch: List<String> = emptyList(),
    val tv: List<String> = emptyList(),
    val wear: List<String> = emptyList()
)

data class AppMinimal(
    val appId: Long,
    val name: String,
    val summary: String,
    val icon: String,
    val suggestedVersion: String,
)

fun App.minimal() = AppMinimal(
    appId = appId,
    name = metadata.name,
    summary = metadata.summary,
    icon = metadata.icon,
    suggestedVersion = metadata.suggestedVersionName,
)
