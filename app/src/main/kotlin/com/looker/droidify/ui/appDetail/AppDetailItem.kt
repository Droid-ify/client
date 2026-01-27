package com.looker.droidify.ui.appDetail

import android.content.Context
import android.content.pm.PermissionGroupInfo
import android.net.Uri
import androidx.core.net.toUri
import com.looker.droidify.R
import com.looker.droidify.data.local.model.Reproducible
import com.looker.droidify.model.InstalledItem
import com.looker.droidify.model.Product
import com.looker.droidify.model.Release
import com.looker.droidify.model.Repository
import com.looker.droidify.network.DataSize
import com.looker.droidify.utility.common.nullIfEmpty

sealed interface AppDetailItem {
    val viewType: AppDetailAdapter.AppDetailsViewType
    override fun equals(other: Any?): Boolean

    data class AppInfoItem(
        @JvmField
        val repository: Repository,
        @JvmField
        val product: Product,
        @JvmField
        val downloads: Long,
        @JvmField
        val installedItem: InstalledItem?,
        @JvmField
        val isFavourite: Boolean,
    ) : AppDetailItem {
        override val viewType: AppDetailAdapter.AppDetailsViewType
            get() = AppDetailAdapter.AppDetailsViewType.APP_INFO

        @JvmField
        val sizeStr = DataSize(product.displayRelease?.size ?: 0).toString()
    }

    data class DownloadStatusItem(
        @JvmField
        val status: AppDetailAdapter.Status,
    ) : AppDetailItem {
        override val viewType: AppDetailAdapter.AppDetailsViewType
            get() = AppDetailAdapter.AppDetailsViewType.DOWNLOAD_STATUS
    }

    data class InstallButtonItem(
        @JvmField
        val action: AppDetailAdapter.Action?,
    ) : AppDetailItem {
        override val viewType: AppDetailAdapter.AppDetailsViewType
            get() = AppDetailAdapter.AppDetailsViewType.INSTALL_BUTTON
    }

    data class ScreenshotItem(
        @JvmField
        val screenshotItems: List<ScreenshotsAdapterItem>,
    ) : AppDetailItem {
        override val viewType: AppDetailAdapter.AppDetailsViewType
            get() = AppDetailAdapter.AppDetailsViewType.SCREENSHOT
    }

    data class SwitchItem(
        @JvmField
        val switchType: SwitchType,
        @JvmField
        val packageName: String,
        @JvmField
        val versionCode: Long,
    ) : AppDetailItem {
        override val viewType: AppDetailAdapter.AppDetailsViewType
            get() = AppDetailAdapter.AppDetailsViewType.SWITCH
    }

    data class SectionItem(
        @JvmField
        val sectionType: SectionType,
        @JvmField
        val expandType: AppDetailAdapter.ExpandType,
        @JvmField
        val items: List<AppDetailItem>,
        @JvmField
        val collapseCount: Int,
    ) : AppDetailItem {
        constructor(sectionType: SectionType) : this(
            sectionType,
            AppDetailAdapter.ExpandType.NOTHING,
            emptyList(),
            0,
        )

        override val viewType: AppDetailAdapter.AppDetailsViewType
            get() = AppDetailAdapter.AppDetailsViewType.SECTION
    }

    data class ExpandItem(
        @JvmField
        val expandType: AppDetailAdapter.ExpandType,
        @JvmField
        val replace: Boolean,
        @JvmField
        val items: List<AppDetailItem>,
    ) : AppDetailItem {
        override val viewType: AppDetailAdapter.AppDetailsViewType
            get() = AppDetailAdapter.AppDetailsViewType.EXPAND
    }

    data class TextItem(
        @JvmField
        val textType: AppDetailAdapter.TextType,
        @JvmField
        val text: CharSequence,
    ) : AppDetailItem {
        override val viewType: AppDetailAdapter.AppDetailsViewType
            get() = AppDetailAdapter.AppDetailsViewType.TEXT
    }

    sealed class LinkItem : AppDetailItem {
        override val viewType: AppDetailAdapter.AppDetailsViewType
            get() = AppDetailAdapter.AppDetailsViewType.LINK

        abstract val iconResId: Int
        abstract fun getTitle(context: Context): String
        abstract val uri: Uri?

        val displayLink: String?
            get() = uri?.schemeSpecificPart?.nullIfEmpty()
                ?.let { if (it.startsWith("//")) null else it } ?: uri?.toString()

        data class Typed(
            @JvmField
            val linkType: LinkType,
            @JvmField
            val text: String,
            override val uri: Uri?,
        ) : LinkItem() {
            override val iconResId: Int
                get() = linkType.iconResId

            override fun getTitle(context: Context): String {
                return text.nullIfEmpty()?.let { linkType.format?.invoke(context, it) ?: it }
                    ?: context.getString(linkType.titleResId)
            }
        }

        data class Donate(
            @JvmField
            val donate: Product.Donate
        ) : LinkItem() {
            override val iconResId: Int
                get() = when (donate) {
                    is Product.Donate.Regular -> R.drawable.ic_donate
                    is Product.Donate.Bitcoin -> R.drawable.ic_donate_bitcoin
                    is Product.Donate.Litecoin -> R.drawable.ic_donate_litecoin
                    is Product.Donate.Liberapay -> R.drawable.ic_donate_liberapay
                    is Product.Donate.OpenCollective -> R.drawable.ic_donate_opencollective
                }

            override fun getTitle(context: Context): String = when (donate) {
                is Product.Donate.Regular -> context.getString(R.string.website)
                is Product.Donate.Bitcoin -> "Bitcoin"
                is Product.Donate.Litecoin -> "Litecoin"
                is Product.Donate.Liberapay -> "Liberapay"
                is Product.Donate.OpenCollective -> "Open Collective"
            }

            override val uri: Uri = when (donate) {
                is Product.Donate.Regular -> donate.url
                is Product.Donate.Bitcoin -> "bitcoin:${donate.address}"
                is Product.Donate.Litecoin -> "litecoin:${donate.address}"
                is Product.Donate.Liberapay -> "https://liberapay.com/${donate.id}"
                is Product.Donate.OpenCollective -> "https://opencollective.com/${donate.id}"
            }.toUri()
        }
    }

    data class PermissionsItem(
        @JvmField
        val group: PermissionGroupInfo?,
        @JvmField
        val permissionNames: List<String>,
        @JvmField
        val formattedText: CharSequence,
    ) : AppDetailItem {
        override val viewType: AppDetailAdapter.AppDetailsViewType
            get() = AppDetailAdapter.AppDetailsViewType.PERMISSIONS
    }

    data class ReleaseItem(
        @JvmField
        val repository: Repository,
        @JvmField
        val release: Release,
        @JvmField
        val selectedRepository: Boolean,
        @JvmField
        val showSignature: Boolean,
        @JvmField
        val reproducible: Reproducible,
        @JvmField
        val installedItem: InstalledItem?,
    ) : AppDetailItem {

        override val viewType: AppDetailAdapter.AppDetailsViewType
            get() = AppDetailAdapter.AppDetailsViewType.RELEASE

        @JvmField
        val incompatibility = release.incompatibilities.firstOrNull()

        @JvmField
        val singlePlatform = if (release.platforms.size == 1) {
            release.platforms.first()
        } else {
            null
        }

        @JvmField
        val installed = installedItem?.versionCode == release.versionCode && installedItem.signature == release.signature

        @JvmField
        val suggested = incompatibility == null && release.selected && selectedRepository
    }

    data class EmptyItem(
        @JvmField
        val packageName: String,
        @JvmField
        val repoAddress: String?,
    ) : AppDetailItem {

        override val viewType: AppDetailAdapter.AppDetailsViewType
            get() = AppDetailAdapter.AppDetailsViewType.EMPTY
    }
}
