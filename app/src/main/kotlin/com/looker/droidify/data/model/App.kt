package com.looker.droidify.data.model

import androidx.compose.runtime.Immutable

@Immutable
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
    val packages: List<Package>?,
)

@Immutable
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

@Immutable
data class Graphics(
    val featureGraphic: FilePath? = null,
    val promoGraphic: FilePath? = null,
    val tvBanner: FilePath? = null,
    val video: FilePath? = null,
)

@Immutable
data class Links(
    val changelog: String? = null,
    val issueTracker: String? = null,
    val sourceCode: String? = null,
    val translation: String? = null,
    val webSite: String? = null,
)

@Immutable
data class Metadata(
    val name: String,
    val packageName: PackageName,
    val added: Long,
    val description: Html,
    val icon: FilePath?,
    val lastUpdated: Long,
    val license: String,
    val suggestedVersionCode: Long,
    val suggestedVersionName: String,
    val summary: String,
)

data class Screenshots(
    val phone: List<FilePath> = emptyList(),
    val sevenInch: List<FilePath> = emptyList(),
    val tenInch: List<FilePath> = emptyList(),
    val tv: List<FilePath> = emptyList(),
    val wear: List<FilePath> = emptyList(),
)

@Immutable
data class AppMinimal(
    val appId: Long,
    val packageName: PackageName,
    val name: String,
    val summary: String?,
    val icon: FilePath?,
    val suggestedVersion: String,
) {
    val fallbackIcon: FilePath? = icon?.path?.let {  current ->
        FilePath(current.substringBeforeLast("/") + "/icon.png")
    }
}

fun App.minimal() = AppMinimal(
    appId = appId,
    packageName = metadata.packageName,
    name = metadata.name,
    summary = metadata.summary,
    icon = metadata.icon,
    suggestedVersion = metadata.suggestedVersionName,
)
