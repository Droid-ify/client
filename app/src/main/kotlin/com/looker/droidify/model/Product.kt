package com.looker.droidify.model

import android.content.Context
import com.looker.droidify.utility.common.extension.getColorFromAttr
import com.looker.droidify.utility.common.extension.videoPlaceHolder
import com.google.android.material.R as MaterialR

data class Product(
    @JvmField
    var repositoryId: Long,
    @JvmField
    val packageName: String,
    @JvmField
    val name: String,
    @JvmField
    val summary: String,
    @JvmField
    var description: String,
    @JvmField
    val whatsNew: String,
    @JvmField
    val icon: String,
    @JvmField
    val metadataIcon: String,
    @JvmField
    val author: Author,
    @JvmField
    val source: String,
    @JvmField
    val changelog: String,
    @JvmField
    val web: String,
    @JvmField
    val tracker: String,
    @JvmField
    val added: Long,
    @JvmField
    val updated: Long,
    @JvmField
    val suggestedVersionCode: Long,
    @JvmField
    val categories: List<String>,
    @JvmField
    val antiFeatures: List<String>,
    @JvmField
    val licenses: List<String>,
    @JvmField
    val donates: List<Donate>,
    @JvmField
    val screenshots: List<Screenshot>,
    @JvmField
    val releases: List<Release>
) {
    data class Author(
        @JvmField
        val name: String,
        @JvmField
        val email: String,
        @JvmField
        val web: String,
    )

    sealed class Donate {
        data class Regular(
            @JvmField
            val url: String
        ) : Donate()

        data class Bitcoin(
            @JvmField
            val address: String
        ) : Donate()

        data class Litecoin(
            @JvmField
            val address: String
        ) : Donate()

        data class Liberapay(
            @JvmField
            val id: String
        ) : Donate()

        data class OpenCollective(
            @JvmField
            val id: String
        ) : Donate()
    }

    data class Screenshot(
        @JvmField
        val locale: String,
        @JvmField
        val type: Type,
        @JvmField
        val path: String,
    ) {
        enum class Type(val jsonName: String) {
            VIDEO("video"),
            PHONE("phone"),
            SMALL_TABLET("smallTablet"),
            LARGE_TABLET("largeTablet"),
            WEAR("wear"),
            TV("tv")
        }

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
