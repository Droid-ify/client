package com.looker.droidify.model

import com.looker.core.domain.model.App
import com.looker.core.domain.model.Donation
import com.looker.core.domain.model.Screenshots

data class Product(
    var repositoryId: Long,
    val packageName: String,
    val name: String,
    val summary: String,
    var description: String,
    val whatsNew: String,
    val icon: String,
    val metadataIcon: String,
    val author: Author,
    val source: String,
    val changelog: String,
    val web: String,
    val tracker: String,
    val added: Long,
    val updated: Long,
    val suggestedVersionCode: Long,
    val categories: List<String>,
    val antiFeatures: List<String>,
    val licenses: List<String>,
    val donates: List<Donate>,
    val screenshots: List<Screenshot>,
    val releases: List<Release>
) {
    data class Author(val name: String, val email: String, val web: String)

    sealed class Donate {
        data class Regular(val url: String) : Donate()
        data class Bitcoin(val address: String) : Donate()
        data class Litecoin(val address: String) : Donate()
        data class Flattr(val id: String) : Donate()
        data class Liberapay(val id: String) : Donate()
        data class OpenCollective(val id: String) : Donate()
    }

    class Screenshot(val locale: String, val type: Type, val path: String) {
        enum class Type(val jsonName: String) {
            PHONE("phone"),
            SMALL_TABLET("smallTablet"),
            LARGE_TABLET("largeTablet")
        }

        val identifier: String
            get() = "$locale.${type.name}.$path"

        fun url(
            repository: Repository,
            packageName: String
        ): String {
            val phoneType = when (type) {
                Type.PHONE -> "phoneScreenshots"
                Type.SMALL_TABLET -> "sevenInchScreenshots"
                Type.LARGE_TABLET -> "tenInchScreenshots"
            }
            return "${repository.address}/$packageName/$locale/$phoneType/$path"
        }
    }

    // Same releases with different signatures
    val selectedReleases: List<Release>
        get() = releases.filter { it.selected }

    val displayRelease: Release?
        get() = selectedReleases.firstOrNull() ?: releases.firstOrNull()

    val version: String
        get() = displayRelease?.version.orEmpty()

    val versionCode: Long
        get() = selectedReleases.firstOrNull()?.versionCode ?: 0L

    val compatible: Boolean
        get() = selectedReleases.firstOrNull()?.incompatibilities?.isEmpty() == true

    val signatures: List<String>
        get() = selectedReleases.mapNotNull { it.signature.ifBlank { null } }.distinct().toList()

    fun item(): ProductItem {
        return ProductItem(
            repositoryId,
            packageName,
            name,
            summary,
            icon,
            metadataIcon,
            version,
            "",
            compatible,
            false,
            0
        )
    }

    fun canUpdate(installedItem: InstalledItem?): Boolean {
        return installedItem != null && compatible && versionCode > installedItem.versionCode &&
            installedItem.signature in signatures
    }
}

fun List<Pair<Product, Repository>>.findSuggested(
    installedItem: InstalledItem?
): Pair<Product, Repository>? = maxWithOrNull(
    compareBy(
        { (product, _) ->
            product.compatible &&
                (installedItem == null || installedItem.signature in product.signatures)
        },
        { (product, _) ->
            product.versionCode
        }
    )
)
//
//fun App.toProduct() = Product(
//    packageName = metadata.packageName.name,
//    name = metadata.name,
//    summary = metadata.summary,
//    description = metadata.description,
//    whatsNew = metadata.whatsNew,
//    icon = metadata.icon,
//    metadataIcon = "",
//    author = Product.Author(
//        name = author.name,
//        email = author.email,
//        web = author.web,
//    ),
//    source = links.sourceCode,
//    changelog = links.changelog,
//    web = links.webSite,
//    tracker = links.issueTracker,
//    added = metadata.added,
//    updated = metadata.lastUpdated,
//    suggestedVersionCode = metadata.suggestedVersionCode,
//    categories = categories,
//    antiFeatures = metadata.antiFeatures,
//    licenses = listOf(metadata.license),
//    donates = donation.toLegacy(),
//    screenshots = screenshots.toLegacy(),
//    releases = packages
//)

fun Donation.toLegacy() = buildList {
    regularUrl?.let { add(Product.Donate.Regular(it)) }
    bitcoinAddress?.let { add(Product.Donate.Bitcoin(it)) }
    flattrId?.let { add(Product.Donate.Flattr(it)) }
    liteCoinAddress?.let { add(Product.Donate.Litecoin(it)) }
    openCollectiveId?.let { add(Product.Donate.OpenCollective(it)) }
    librePayId?.let { add(Product.Donate.Liberapay(it)) }
    librePayAddress?.let { add(Product.Donate.Liberapay(it)) }
}

fun Screenshots.toLegacy() = buildList {
    phone.forEach { add(Product.Screenshot("en-US", Product.Screenshot.Type.PHONE, it)) }
    sevenInch.forEach { add(Product.Screenshot("en-US", Product.Screenshot.Type.SMALL_TABLET, it)) }
    tenInch.forEach { add(Product.Screenshot("en-US", Product.Screenshot.Type.LARGE_TABLET, it)) }
    tv.forEach { add(Product.Screenshot("en-US", Product.Screenshot.Type.PHONE, it)) }
    wear.forEach { add(Product.Screenshot("en-US", Product.Screenshot.Type.PHONE, it)) }
}
