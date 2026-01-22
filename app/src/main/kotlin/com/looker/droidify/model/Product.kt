package com.looker.droidify.model

import android.content.Context
import com.looker.droidify.utility.common.extension.getColorFromAttr
import com.looker.droidify.utility.common.extension.videoPlaceHolder
import com.google.android.material.R as MaterialR

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
        data class Liberapay(val id: String) : Donate()
        data class OpenCollective(val id: String) : Donate()
    }

    class Screenshot(val locale: String, val type: Type, val path: String) {
        enum class Type(val jsonName: String) {
            VIDEO("video"),
            PHONE("phone"),
            SMALL_TABLET("smallTablet"),
            LARGE_TABLET("largeTablet"),
            WEAR("wear"),
            TV("tv")
        }

        val identifier: String
            get() = "$locale.${type.name}.$path"

        fun url(
            context: Context,
            repository: Repository,
            packageName: String
        ): Any {
            if (type == Type.VIDEO) return context.videoPlaceHolder.apply {
                setTintList(context.getColorFromAttr(MaterialR.attr.colorOnSurfaceInverse))
            }
            val phoneType = when (type) {
                Type.PHONE -> "phoneScreenshots"
                Type.SMALL_TABLET -> "sevenInchScreenshots"
                Type.LARGE_TABLET -> "tenInchScreenshots"
                Type.WEAR -> "wearScreenshots"
                Type.TV -> "tvScreenshots"
                else -> error("Should not be here, video url already returned")
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
            repoId = repositoryId,
            packageName = packageName,
            name = name,
            summary = summary,
            icon = icon,
            metadataIcon = metadataIcon,
            version = version,
            installedVersion = "",
            compatible = compatible,
            canUpdate = false,
            matchRank = 0,
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
