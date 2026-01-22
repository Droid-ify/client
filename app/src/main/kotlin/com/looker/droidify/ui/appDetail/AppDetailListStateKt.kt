package com.looker.droidify.ui.appDetail

import android.content.Context
import android.content.pm.PermissionGroupInfo
import android.content.pm.PermissionInfo
import android.text.SpannableStringBuilder
import android.text.style.RelativeSizeSpan
import android.text.style.TypefaceSpan
import android.view.View.MeasureSpec
import android.widget.TextView
import androidx.core.net.toUri
import com.looker.droidify.data.local.model.RBLogEntity
import com.looker.droidify.data.local.model.toReproducible
import com.looker.droidify.model.InstalledItem
import com.looker.droidify.model.Product
import com.looker.droidify.model.Release
import com.looker.droidify.model.Repository
import com.looker.droidify.ui.appDetail.AppDetailAdapter.ExpandType
import com.looker.droidify.ui.appDetail.AppDetailAdapter.TextType
import com.looker.droidify.ui.appDetail.AppDetailAdapter.TextType.DESCRIPTION
import com.looker.droidify.ui.appDetail.viewHolders.createTextElementLayout
import com.looker.droidify.utility.PackageItemResolver
import com.looker.droidify.utility.common.extension.getWindowManager
import com.looker.droidify.utility.common.nullIfEmpty
import com.looker.droidify.utility.text.formatHtml
import java.util.Locale
import com.looker.droidify.R.string as stringRes

private const val MAX_RELEASE_ITEMS = 5

data class AppDetailListState(
    @JvmField
    val items: List<AppDetailItem>,
)

internal fun createAppDetailListState(
    context: Context,
    packageName: String,
    suggestedRepo: String? = null,
    products: List<Pair<Product, Repository>>,
    rbLogs: List<RBLogEntity>,
    downloads: Long,
    installedItem: InstalledItem?,
    isFavourite: Boolean,
    allowIncompatibleVersion: Boolean,
): AppDetailListState {
    val productRepository = products.findSuggested(installedItem) ?: run {
        return AppDetailListState(
            items = listOf(AppDetailItem.EmptyItem(packageName, suggestedRepo)),
        )
    }

    val product = productRepository.first

    val items = ArrayList<AppDetailItem>()

    items += AppDetailItem.AppInfoItem(
        repository = productRepository.second,
        product = product,
        downloads = downloads,
        installedItem = installedItem,
        isFavourite = isFavourite,
    )

    items += AppDetailItem.DownloadStatusItem(AppDetailAdapter.Status.Idle)
    items += AppDetailItem.InstallButtonItem(null)

    val screenshots = product.screenshots
    if (screenshots.isNotEmpty()) {
        val repository = productRepository.second
        val screenshotItems = screenshots.map {
            if (it.type == Product.Screenshot.Type.VIDEO) {
                VideoItem(videoUrl = it.path)
            } else {
                ScreenshotItem(
                    repository = repository,
                    packageName = packageName,
                    screenshot = it
                )
            }
        }

        items += AppDetailItem.ScreenshotItem(screenshotItems)
    }

    if (installedItem != null) {
        items.add(
            AppDetailItem.SwitchItem(
                switchType = SwitchType.IGNORE_ALL_UPDATES,
                packageName = packageName,
                versionCode = product.versionCode,
            ),
        )

        if (product.canUpdate(installedItem)) {
            items.add(
                AppDetailItem.SwitchItem(
                    switchType = SwitchType.IGNORE_THIS_UPDATE,
                    packageName = packageName,
                    versionCode = product.versionCode,
                ),
            )
        }
    }

    val textView by lazy {
        @Suppress("DEPRECATION")
        val displayContext = context.createDisplayContext(context.getWindowManager().defaultDisplay)
        createTextElementLayout(displayContext)
    }

    val description = formatHtml(product.description, true).apply {
        val summary = product.summary
        if (summary.isNotEmpty() && product.name != summary) {
            if (isNotEmpty()) {
                insert(0, "\n\n")
            }
            insert(0, summary)
            if (isNotEmpty()) {
                setSpan(
                    TypefaceSpan("sans-serif-medium"),
                    0,
                    summary.length,
                    SpannableStringBuilder.SPAN_EXCLUSIVE_EXCLUSIVE,
                )
            }
        }
    }
    if (description.isNotEmpty()) {
        val item = AppDetailItem.TextItem(
            textType = DESCRIPTION,
            text = description,
        )

        val cropped = description.lineCropped(
            tempTextView = textView,
            maxLines = 12,
            cropLines = 10,
        )

        if (cropped != null) {
            val croppedItem = AppDetailItem.TextItem(DESCRIPTION, cropped)
            items.add(croppedItem)
            items.add(AppDetailItem.ExpandItem(
                expandType = ExpandType.DESCRIPTION,
                replace = true,
                items = listOf(item, croppedItem)
            ))
        } else {
            items += item
        }
    }

    val antiFeatures = product.antiFeatures
    if (antiFeatures.isNotEmpty()) {
        items += AppDetailItem.SectionItem(SectionType.ANTI_FEATURES)
        items += AppDetailItem.TextItem(
            textType = TextType.ANTI_FEATURES,
            text = formatAntiFeatures(
                context,
                antiFeatures,
            ),
        )
    }

    val changes = formatHtml(product.whatsNew, true)
    if (changes.isNotEmpty()) {
        items += AppDetailItem.SectionItem(SectionType.CHANGES)
        val cropped = changes.lineCropped(
            tempTextView = textView,
            maxLines = 12,
            cropLines = 10,
        )
        val item = AppDetailItem.TextItem(TextType.CHANGES, changes)
        if (cropped != null) {
            val croppedItem = AppDetailItem.TextItem(
                textType = TextType.CHANGES,
                text = cropped,
            )

            items += croppedItem
            items += AppDetailItem.ExpandItem(
                expandType = ExpandType.CHANGES,
                replace = true,
                items = listOf(item, croppedItem)
            )
        } else {
            items += item
        }
    }

    val linkItems = collectLinks(product)

    if (linkItems.isNotEmpty()) {
        items += AppDetailItem.SectionItem(
            sectionType = SectionType.LINKS,
            expandType = ExpandType.LINKS,
            items = linkItems,
            collapseCount = 0,
        )
    }

    val donates = product.donates
    if (donates.isNotEmpty()) {
        items += AppDetailItem.SectionItem(
            sectionType = SectionType.DONATE,
            expandType = ExpandType.DONATES,
            items = donates.map(AppDetailItem.LinkItem::Donate),
            collapseCount = 0,
        )
    }

    val release = product.displayRelease
    if (release != null) {
        val permissions = collectPermissions(context, release)

        if (permissions.isNotEmpty()) {
            val permissionsItems = ArrayList<AppDetailItem>(permissions.size)

            permissions.mapNotNullTo(permissionsItems) {
                if (it.key != null) {
                    val permissionInfos = it.value.flatten()

                    AppDetailItem.PermissionsItem(
                        group = it.key,
                        permissionNames = permissionInfos.map { permissionInfo -> permissionInfo.name },
                        formattedText = formatPermissionsText(context, permissionInfos),
                    )
                } else {
                    null
                }
            }

            permissions[null]?.let {
                val permissionInfos = it.flatten()

                permissionsItems += AppDetailItem.PermissionsItem(
                    group = null,
                    permissionNames = permissionInfos.map { permissionInfo -> permissionInfo.name },
                    formattedText = formatPermissionsText(context, permissionInfos)
                )
            }

            items += AppDetailItem.SectionItem(
                sectionType = SectionType.PERMISSIONS,
                expandType = ExpandType.PERMISSIONS,
                items = permissionsItems,
                collapseCount = 0,
            )
        }
    }

    val releaseItems = collectReleaseItems(
        products = products,
        productRepository = productRepository,
        allowIncompatibleVersion = allowIncompatibleVersion,
        release = release,
        installedItem = installedItem,
        rbLogs = rbLogs,
    )

    if (releaseItems.isNotEmpty()) {
        items += AppDetailItem.SectionItem(SectionType.VERSIONS)
        if (releaseItems.size > MAX_RELEASE_ITEMS) {
            items.addAll(releaseItems.take(MAX_RELEASE_ITEMS))
            items.add(
                AppDetailItem.ExpandItem(
                    expandType = ExpandType.VERSIONS,
                    replace = false,
                    items = releaseItems.takeLast(releaseItems.size - MAX_RELEASE_ITEMS),
                )
            )
        } else {
            items += releaseItems
        }
    }

    return AppDetailListState(
        items = items,
    )
}

private fun collectLinks(product: Product): List<AppDetailItem.LinkItem> {
    val linkItems = ArrayList<AppDetailItem.LinkItem>()

    product.source.let { source ->
        if (source.isNotEmpty()) {
            linkItems += AppDetailItem.LinkItem.Typed(
                linkType = LinkType.SOURCE,
                text = "",
                uri = source.toUri(),
            )
        }
    }

    product.author.let { author ->
        if (author.name.isNotEmpty() || author.web.isNotEmpty()) {
            linkItems += AppDetailItem.LinkItem.Typed(
                linkType = LinkType.AUTHOR,
                text = author.name,
                uri = author.web.nullIfEmpty()?.toUri(),
            )
        }

        author.email.nullIfEmpty()?.let { email ->
            linkItems += AppDetailItem.LinkItem.Typed(
                linkType = LinkType.EMAIL,
                text = "",
                uri = "mailto:$email".toUri()
            )
        }
    }

    product.licenses.mapTo(linkItems) { licenses ->
        AppDetailItem.LinkItem.Typed(
            linkType = LinkType.LICENSE,
            text = licenses,
            uri = "https://spdx.org/licenses/$licenses.html".toUri(),
        )
    }

    product.tracker.nullIfEmpty()?.let { tracker ->
        linkItems += AppDetailItem.LinkItem.Typed(
            linkType = LinkType.TRACKER,
            text = "",
            uri = tracker.toUri()
        )
    }

    product.changelog.nullIfEmpty()?.let { changelog ->
        linkItems += AppDetailItem.LinkItem.Typed(
            linkType = LinkType.CHANGELOG,
            text = "",
            uri = changelog.toUri(),
        )
    }

    product.web.nullIfEmpty()?.let { web ->
        linkItems += AppDetailItem.LinkItem.Typed(
            linkType = LinkType.WEB,
            text = "",
            uri = web.toUri()
        )
    }

    return linkItems
}

private fun collectPermissions(context: Context, release: Release): Map<PermissionGroupInfo?, List<List<PermissionInfo>>> {
    val packageManager = context.packageManager

    return release.permissions
        .asSequence()
        .mapNotNull {
            try {
                packageManager.getPermissionInfo(it, 0)
            } catch (_: Exception) {
                null
            }
        }
        .groupBy(PackageItemResolver::getPermissionGroup)
        .asSequence()
        .map { (group, permissionInfo) ->
            val permissionGroupInfo = try {
                group?.let { packageManager.getPermissionGroupInfo(it, 0) }
            } catch (_: Exception) {
                null
            }
            Pair(permissionGroupInfo, permissionInfo)
        }
        .groupBy({ it.first }, { it.second })
}

private fun collectReleaseItems(
    products: List<Pair<Product, Repository>>,
    productRepository: Pair<Product, Repository>,
    allowIncompatibleVersion: Boolean,
    release: Release?,
    installedItem: InstalledItem?,
    rbLogs: List<RBLogEntity>,
): List<AppDetailItem.ReleaseItem> {
    val compatibleReleasePairs = products.flatMap { (product, repository) ->
        product.releases.mapNotNull {
            if (allowIncompatibleVersion || it.incompatibilities.isEmpty()) {
                Pair(it, repository)
            } else {
                null
            }
        }
    }

    val versionsWithMultiSignature = compatibleReleasePairs
        .filterNot { release?.signature?.isEmpty() == true }
        .map { (release, _) -> release.versionCode to release.signature }
        .distinct()
        .groupBy { it.first }
        .filter { (_, entry) -> entry.size >= 2 }
        .keys

    return compatibleReleasePairs
        .mapTo(ArrayList()) { (release, repository) ->
            AppDetailItem.ReleaseItem(
                repository = repository,
                release = release,
                selectedRepository = repository.id == productRepository.second.id,
                showSignature = release.versionCode in versionsWithMultiSignature,
                reproducible = rbLogs.find { it.hash == release.hash }.toReproducible(),
                installedItem = installedItem,
            )
        }.apply {
            sortByDescending { it.release.versionCode }
        }
}

private val permissionPrefixes: Array<String> = arrayOf("android.permission.", "com.android.browser.permission.")

private fun formatPermissionsText(
    context: Context,
    permissions: List<PermissionInfo>,
): CharSequence {
    val localCache = PackageItemResolver.LocalCache()
    val labels = permissions.map { permission ->
        val labelFromPackage = PackageItemResolver.loadLabel(
            context = context,
            localCache = localCache,
            packageItemInfo = permission,
        )
        val label = labelFromPackage ?: run {
            permissionPrefixes.find { permission.name.startsWith(it) }?.let { it ->
                val transform = permission.name.substring(it.length)
                if (transform.matches("[A-Z_]+".toRegex())) {
                    transform.split("_")
                        .joinToString(separator = " ") { it.lowercase(Locale.US) }
                } else {
                    null
                }
            }
        }
        if (label == null) {
            Pair(false, permission.name)
        } else {
            Pair(
                true,
                label.first().uppercaseChar() + label.substring(1, label.length),
            )
        }
    }
    val builder = SpannableStringBuilder()
    (
        labels.asSequence().filter { it.first } + labels.asSequence()
            .filter { !it.first }
        ).forEach {
            if (builder.isNotEmpty()) {
                builder.append("\n\n")
                builder.setSpan(
                    RelativeSizeSpan(1f / 3f),
                    builder.length - 2,
                    builder.length,
                    SpannableStringBuilder.SPAN_EXCLUSIVE_EXCLUSIVE,
                )
            }
            builder.append(it.second)
            if (!it.first) {
                // Replace dots with spans to enable word wrap
                it.second.asSequence()
                    .mapIndexedNotNull { index, c -> if (c == '.') index else null }
                    .map { index -> index + builder.length - it.second.length }
                    .forEach { index ->
                        builder.setSpan(
                            DotSpan(),
                            index,
                            index + 1,
                            SpannableStringBuilder.SPAN_EXCLUSIVE_EXCLUSIVE,
                        )
                    }
            }
        }

    return builder
}

private fun formatAntiFeatures(context: Context, antiFeatures: List<String>): String {
    val resources = context.resources

    return antiFeatures.joinToString(separator = "\n") {
        val s = when (it) {
            "Ads" -> resources.getString(stringRes.has_advertising)
            "ApplicationDebuggable" -> resources.getString(stringRes.compiled_for_debugging)
            "DisabledAlgorithm" -> resources.getString(stringRes.signed_using_unsafe_algorithm)
            "KnownVuln" -> resources.getString(stringRes.has_security_vulnerabilities)
            "NoSourceSince" -> resources.getString(stringRes.source_code_no_longer_available)
            "NonFreeAdd" -> resources.getString(stringRes.promotes_non_free_software)
            "NonFreeAssets" -> resources.getString(stringRes.contains_non_free_media)
            "NonFreeDep" -> resources.getString(stringRes.has_non_free_dependencies)
            "NonFreeNet" -> resources.getString(stringRes.promotes_non_free_network_services)
            "NSFW" -> resources.getString(stringRes.contains_nsfw)
            "Tracking" -> resources.getString(stringRes.tracks_or_reports_your_activity)
            "UpstreamNonFree" -> resources.getString(stringRes.upstream_source_code_is_not_free)
            // special tag (https://floss.social/@IzzyOnDroid/110815951568369581)
            // apps include non-free libraries
            "NonFreeComp" -> resources.getString(stringRes.has_non_free_components)
            "TetheredNet" -> resources.getString(stringRes.has_tethered_network)
            else -> resources.getString(stringRes.unknown_FORMAT, it)
        }

        "\u2022 $s"
    }
}

private fun CharSequence.lineCropped(
    tempTextView: TextView,
    maxLines: Int,
    cropLines: Int,
): CharSequence? {
    assert(cropLines <= maxLines)
    tempTextView.text = this

    val textViewWidthSpec = MeasureSpec.makeMeasureSpec(
        tempTextView.resources.displayMetrics.widthPixels,
        MeasureSpec.EXACTLY,
    )
    val textViewHeightSpec = MeasureSpec.makeMeasureSpec(
        0,
        MeasureSpec.UNSPECIFIED,
    )
    tempTextView.measure(textViewWidthSpec, textViewHeightSpec)
    tempTextView.layout(
        0,
        0,
        tempTextView.measuredWidth,
        tempTextView.measuredHeight,
    )
    val layout = tempTextView.layout
    val cropLineOffset = if (layout.lineCount <= maxLines) {
        -1
    } else {
        layout.getLineEnd(cropLines - 1)
    }

    val paragraphEndIndex = if (cropLineOffset < 0) {
        -1
    } else {
        indexOf("\n\n", cropLineOffset).let { if (it >= 0) it else length }
    }
    val paragraphEndLine = if (paragraphEndIndex < 0) {
        -1
    } else {
        layout.getLineForOffset(paragraphEndIndex).apply { assert(this >= 0) }
    }
    val end = when {
        cropLineOffset < 0 -> -1
        paragraphEndLine >= 0 && paragraphEndLine - (cropLines - 1) <= 3 ->
            if (paragraphEndIndex < length) paragraphEndIndex else -1

        else -> cropLineOffset
    }
    val length = if (end < 0) {
        -1
    } else {
        take(end).indexOfLast { it != '\n' }.let { if (it >= 0) it + 1 else end }
    }
    return if (length >= 0) subSequence(0, length) else null
}

internal fun List<Pair<Product, Repository>>.findSuggested(
    installedItem: InstalledItem?,
): Pair<Product, Repository>? = maxWithOrNull(
    compareBy(
        { (product, _) ->
            product.compatible &&
                (installedItem == null || installedItem.signature in product.signatures)
        },
        { (product, _) ->
            product.versionCode
        },
    ),
)
