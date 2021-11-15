package com.looker.droidify.screen

import android.annotation.SuppressLint
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PermissionGroupInfo
import android.content.pm.PermissionInfo
import android.content.res.Resources
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Parcel
import android.text.SpannableStringBuilder
import android.text.format.DateFormat
import android.text.style.*
import android.text.util.Linkify
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.content.res.AppCompatResources
import androidx.appcompat.widget.LinearLayoutCompat
import androidx.core.content.res.ResourcesCompat
import androidx.core.net.toUri
import androidx.core.text.HtmlCompat
import androidx.core.text.util.LinkifyCompat
import androidx.core.view.doOnPreDraw
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.circularreveal.CircularRevealFrameLayout
import com.google.android.material.divider.MaterialDivider
import com.google.android.material.imageview.ShapeableImageView
import com.google.android.material.progressindicator.LinearProgressIndicator
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.switchmaterial.SwitchMaterial
import com.google.android.material.textview.MaterialTextView
import com.looker.droidify.R
import com.looker.droidify.content.Preferences
import com.looker.droidify.content.ProductPreferences
import com.looker.droidify.entity.*
import com.looker.droidify.graphics.PaddingDrawable
import com.looker.droidify.network.CoilDownloader
import com.looker.droidify.utility.KParcelable
import com.looker.droidify.utility.PackageItemResolver
import com.looker.droidify.utility.Utils
import com.looker.droidify.utility.extension.android.*
import com.looker.droidify.utility.extension.resources.*
import com.looker.droidify.utility.extension.text.*
import com.looker.droidify.widget.ClickableMovementMethod
import com.looker.droidify.widget.StableRecyclerAdapter
import java.lang.ref.WeakReference
import java.util.*
import kotlin.math.*

class ProductAdapter(private val callbacks: Callbacks, private val columns: Int) :
    StableRecyclerAdapter<ProductAdapter.ViewType, RecyclerView.ViewHolder>() {
    companion object {
        private const val GRID_SPACING_OUTER_DP = 16
        private const val GRID_SPACING_INNER_DP = 8
    }

    interface Callbacks {
        fun onActionClick(action: Action)
        fun onPreferenceChanged(preference: ProductPreference)
        fun onPermissionsClick(group: String?, permissions: List<String>)
        fun onScreenshotClick(screenshot: Product.Screenshot)
        fun onReleaseClick(release: Release)
        fun onUriClick(uri: Uri, shouldConfirm: Boolean): Boolean
    }

    enum class Action(val titleResId: Int) {
        INSTALL(R.string.install),
        UPDATE(R.string.update),
        LAUNCH(R.string.launch),
        DETAILS(R.string.details),
        UNINSTALL(R.string.uninstall),
        CANCEL(R.string.cancel),
        SHARE(R.string.share)
    }

    sealed class Status {
        object Pending : Status()
        object Connecting : Status()
        data class Downloading(val read: Long, val total: Long?) : Status()
    }

    enum class ViewType { APP_INFO, SWITCH, SECTION, EXPAND, TEXT, LINK, PERMISSIONS, RELEASE, EMPTY }

    private enum class SwitchType(val titleResId: Int) {
        IGNORE_ALL_UPDATES(R.string.ignore_all_updates),
        IGNORE_THIS_UPDATE(R.string.ignore_this_update)
    }

    private enum class SectionType(val titleResId: Int, val colorAttrResId: Int) {
        ANTI_FEATURES(R.string.anti_features, R.attr.colorError),
        CHANGES(R.string.changes, android.R.attr.colorAccent),
        LINKS(R.string.links, android.R.attr.colorAccent),
        DONATE(R.string.donate, android.R.attr.colorAccent),
        PERMISSIONS(R.string.permissions, android.R.attr.colorAccent),
        VERSIONS(R.string.versions, android.R.attr.colorAccent)
    }

    internal enum class ExpandType {
        NOTHING, DESCRIPTION, CHANGES,
        LINKS, DONATES, PERMISSIONS, SCREENSHOTS, VERSIONS
    }

    private enum class TextType { DESCRIPTION, ANTI_FEATURES, CHANGES }

    private enum class LinkType(
        val iconResId: Int, val titleResId: Int,
        val format: ((Context, String) -> String)? = null,
    ) {
        AUTHOR(R.drawable.ic_person, R.string.author_website),
        EMAIL(R.drawable.ic_email, R.string.author_email),
        LICENSE(R.drawable.ic_copyright, R.string.license,
            format = { context, text -> context.getString(R.string.license_FORMAT, text) }),
        TRACKER(R.drawable.ic_bug_report, R.string.bug_tracker),
        CHANGELOG(R.drawable.ic_history, R.string.changelog),
        WEB(R.drawable.ic_public, R.string.project_website)
    }

    private sealed class Item {
        abstract val descriptor: String
        abstract val viewType: ViewType

        class AppInfoItem(val repository: Repository, val product: Product) : Item() {
            override val descriptor: String
                get() = "app_info.${product.name}"

            override val viewType: ViewType
                get() = ViewType.APP_INFO
        }

        class SwitchItem(
            val switchType: SwitchType,
            val packageName: String,
            val versionCode: Long,
        ) : Item() {
            override val descriptor: String
                get() = "switch.${switchType.name}"

            override val viewType: ViewType
                get() = ViewType.SWITCH
        }

        class SectionItem(
            val sectionType: SectionType, val expandType: ExpandType,
            val items: List<Item>, val collapseCount: Int,
        ) : Item() {
            constructor(sectionType: SectionType) : this(
                sectionType,
                ExpandType.NOTHING,
                emptyList(),
                0
            )

            override val descriptor: String
                get() = "section.${sectionType.name}"

            override val viewType: ViewType
                get() = ViewType.SECTION
        }

        class ExpandItem(val expandType: ExpandType, val replace: Boolean, val items: List<Item>) :
            Item() {
            override val descriptor: String
                get() = "expand.${expandType.name}"

            override val viewType: ViewType
                get() = ViewType.EXPAND
        }

        class TextItem(val textType: TextType, val text: CharSequence) : Item() {
            override val descriptor: String
                get() = "text.${textType.name}"

            override val viewType: ViewType
                get() = ViewType.TEXT
        }

        sealed class LinkItem : Item() {
            override val viewType: ViewType
                get() = ViewType.LINK

            abstract val iconResId: Int
            abstract fun getTitle(context: Context): String
            abstract val uri: Uri?

            val displayLink: String?
                get() = uri?.schemeSpecificPart?.nullIfEmpty()
                    ?.let { if (it.startsWith("//")) null else it } ?: uri?.toString()

            class Typed(val linkType: LinkType, val text: String, override val uri: Uri?) :
                LinkItem() {
                override val descriptor: String
                    get() = "link.typed.${linkType.name}"

                override val iconResId: Int
                    get() = linkType.iconResId

                override fun getTitle(context: Context): String {
                    return text.nullIfEmpty()?.let { linkType.format?.invoke(context, it) ?: it }
                        ?: context.getString(linkType.titleResId)
                }
            }

            class Donate(val donate: Product.Donate) : LinkItem() {
                override val descriptor: String
                    get() = "link.donate.$donate"

                override val iconResId: Int
                    get() = when (donate) {
                        is Product.Donate.Regular -> R.drawable.ic_donate_regular
                        is Product.Donate.Bitcoin -> R.drawable.ic_donate_bitcoin
                        is Product.Donate.Litecoin -> R.drawable.ic_donate_litecoin
                        is Product.Donate.Flattr -> R.drawable.ic_donate_flattr
                        is Product.Donate.Liberapay -> R.drawable.ic_donate_liberapay
                        is Product.Donate.OpenCollective -> R.drawable.ic_donate_opencollective
                    }

                override fun getTitle(context: Context): String = when (donate) {
                    is Product.Donate.Regular -> context.getString(R.string.website)
                    is Product.Donate.Bitcoin -> "Bitcoin"
                    is Product.Donate.Litecoin -> "Litecoin"
                    is Product.Donate.Flattr -> "Flattr"
                    is Product.Donate.Liberapay -> "Liberapay"
                    is Product.Donate.OpenCollective -> "Open Collective"
                }

                override val uri: Uri? = when (donate) {
                    is Product.Donate.Regular -> Uri.parse(donate.url)
                    is Product.Donate.Bitcoin -> Uri.parse("bitcoin:${donate.address}")
                    is Product.Donate.Litecoin -> Uri.parse("litecoin:${donate.address}")
                    is Product.Donate.Flattr -> Uri.parse("https://flattr.com/thing/${donate.id}")
                    is Product.Donate.Liberapay -> Uri.parse("https://liberapay.com/~${donate.id}")
                    is Product.Donate.OpenCollective -> Uri.parse("https://opencollective.com/${donate.id}")
                }
            }
        }

        class PermissionsItem(
            val group: PermissionGroupInfo?,
            val permissions: List<PermissionInfo>,
        ) : Item() {
            override val descriptor: String
                get() = "permissions.${group?.name}.${permissions.joinToString(separator = ".") { it.name }}"

            override val viewType: ViewType
                get() = ViewType.PERMISSIONS
        }

        class ReleaseItem(
            val repository: Repository, val release: Release, val selectedRepository: Boolean,
            val showSignature: Boolean,
        ) : Item() {
            override val descriptor: String
                get() = "release.${repository.id}.${release.identifier}"

            override val viewType: ViewType
                get() = ViewType.RELEASE
        }

        class EmptyItem(val packageName: String) : Item() {
            override val descriptor: String
                get() = "empty"

            override val viewType: ViewType
                get() = ViewType.EMPTY
        }
    }

    private class Measurement<T : Any> {
        private var density = 0f
        private var scaledDensity = 0f
        private lateinit var metric: T

        fun measure(view: View) {
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
                .let { view.measure(it, it) }
        }

        fun invalidate(resources: Resources, callback: () -> T): T {
            val (density, scaledDensity) = resources.displayMetrics.let {
                Pair(
                    it.density,
                    it.scaledDensity
                )
            }
            if (this.density != density || this.scaledDensity != scaledDensity) {
                this.density = density
                this.scaledDensity = scaledDensity
                metric = callback()
            }
            return metric
        }
    }

    private enum class Payload { REFRESH, STATUS }

    private class AppInfoViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val icon = itemView.findViewById<ShapeableImageView>(R.id.icon)!!
        val name = itemView.findViewById<MaterialTextView>(R.id.name)!!
        val packageName = itemView.findViewById<MaterialTextView>(R.id.package_name)!!
        val action = itemView.findViewById<MaterialButton>(R.id.action)!!
        val statusLayout = itemView.findViewById<View>(R.id.status_layout)!!
        val status = itemView.findViewById<MaterialTextView>(R.id.status)!!
        val progress = itemView.findViewById<LinearProgressIndicator>(R.id.progress)!!

        val progressIcon: Drawable
        val defaultIcon: Drawable

        val actionTintNormal = action.context.getColorFromAttr(R.attr.colorSurface)
        val actionTintCancel = action.context.getColorFromAttr(R.attr.colorError)
        val actionTintOnNormal = action.context.getColorFromAttr(android.R.attr.textColorPrimary)
        val actionTintOnCancel = action.context.getColorFromAttr(R.attr.colorOnError)

        init {
            action.apply {
                setTextColor(action.context.getColorFromAttr(R.attr.colorOnPrimary))
                height = itemView.resources.sizeScaled(48)
            }
            val (progressIcon, defaultIcon) = Utils.getDefaultApplicationIcons(icon.context)
            this.progressIcon = progressIcon
            this.defaultIcon = defaultIcon
        }

        val targetBlock = itemView.findViewById<LinearLayoutCompat>(R.id.sdk_block)!!
        val divider1 = itemView.findViewById<MaterialDivider>(R.id.divider1)!!
        val targetSdk = itemView.findViewById<MaterialTextView>(R.id.sdk)!!
        val version = itemView.findViewById<MaterialTextView>(R.id.version)!!
        val size = itemView.findViewById<MaterialTextView>(R.id.size)!!
        val devName = itemView.findViewById<MaterialTextView>(R.id.dev)!!
        val devIcon = itemView.findViewById<ShapeableImageView>(R.id.dev_icon)!!
        val dev = itemView.findViewById<MaterialCardView>(R.id.dev_block)!!
    }

    private class SwitchViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val enabled = itemView.findViewById<SwitchMaterial>(R.id.enabled)!!

        val statefulViews: Sequence<View>
            get() = sequenceOf(itemView, enabled)
    }

    private class SectionViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val title = itemView.findViewById<MaterialTextView>(R.id.title)!!
        val icon = itemView.findViewById<ShapeableImageView>(R.id.icon)!!
    }

    private class ExpandViewHolder(context: Context) :
        RecyclerView.ViewHolder(MaterialTextView(context)) {
        val text: MaterialTextView
            get() = itemView as MaterialTextView

        init {
            itemView as MaterialTextView
            itemView.typeface = TypefaceExtra.medium
            itemView.setTextSizeScaled(14)
            itemView.background =
                ResourcesCompat.getDrawable(
                    itemView.resources,
                    R.drawable.background_border,
                    context.theme
                )
            itemView.backgroundTintList = itemView.context.getColorFromAttr(R.attr.colorSurface)
            itemView.gravity = Gravity.CENTER
            itemView.isAllCaps = true
            itemView.layoutParams = RecyclerView.LayoutParams(
                RecyclerView.LayoutParams.MATCH_PARENT,
                itemView.resources.sizeScaled(48)
            ).apply {
                topMargin = itemView.resources.sizeScaled(16)
                leftMargin = itemView.resources.sizeScaled(30)
                rightMargin = itemView.resources.sizeScaled(30)
            }
        }
    }

    private class TextViewHolder(context: Context) :
        RecyclerView.ViewHolder(MaterialTextView(context)) {
        val text: TextView
            get() = itemView as MaterialTextView

        init {
            itemView as MaterialTextView
            itemView.setTextSizeScaled(15)
            itemView.resources.sizeScaled(16).let { itemView.setPadding(it, it, it, it) }
            itemView.movementMethod = ClickableMovementMethod
            itemView.layoutParams = RecyclerView.LayoutParams(
                RecyclerView.LayoutParams.MATCH_PARENT,
                RecyclerView.LayoutParams.WRAP_CONTENT
            )
        }
    }

    private open class OverlappingViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        init {
            // Block touch events if touched above negative margin
            itemView.setOnTouchListener { _, event ->
                event.action == MotionEvent.ACTION_DOWN && run {
                    val top = (itemView.layoutParams as ViewGroup.MarginLayoutParams).topMargin
                    top < 0 && event.y < -top
                }
            }
        }
    }

    private class LinkViewHolder(itemView: View) : OverlappingViewHolder(itemView) {
        companion object {
            private val measurement = Measurement<Int>()
        }

        val icon = itemView.findViewById<ShapeableImageView>(R.id.icon)!!
        val text = itemView.findViewById<MaterialTextView>(R.id.text)!!
        val link = itemView.findViewById<MaterialTextView>(R.id.link)!!

        init {
            val margin = measurement.invalidate(itemView.resources) {
                @SuppressLint("SetTextI18n")
                text.text = "measure"
                link.visibility = View.GONE
                measurement.measure(itemView)
                ((itemView.measuredHeight - icon.measuredHeight) / 2f).roundToInt()
            }
            (icon.layoutParams as ViewGroup.MarginLayoutParams).apply {
                topMargin += margin
                bottomMargin += margin
            }
        }
    }

    private class PermissionsViewHolder(itemView: View) : OverlappingViewHolder(itemView) {
        companion object {
            private val measurement = Measurement<Int>()
        }

        val icon = itemView.findViewById<ShapeableImageView>(R.id.icon)!!
        val text = itemView.findViewById<MaterialTextView>(R.id.text)!!

        init {
            val margin = measurement.invalidate(itemView.resources) {
                @SuppressLint("SetTextI18n")
                text.text = "measure"
                measurement.measure(itemView)
                ((itemView.measuredHeight - icon.measuredHeight) / 2f).roundToInt()
            }
            (icon.layoutParams as ViewGroup.MarginLayoutParams).apply {
                topMargin += margin
                bottomMargin += margin
            }
        }
    }

    private class ReleaseViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val dateFormat = DateFormat.getDateFormat(itemView.context)!!

        val version = itemView.findViewById<MaterialTextView>(R.id.version)!!
        val status = itemView.findViewById<MaterialTextView>(R.id.status)!!
        val source = itemView.findViewById<MaterialTextView>(R.id.source)!!
        val added = itemView.findViewById<MaterialTextView>(R.id.added)!!
        val size = itemView.findViewById<MaterialTextView>(R.id.size)!!
        val signature = itemView.findViewById<MaterialTextView>(R.id.signature)!!
        val compatibility = itemView.findViewById<MaterialTextView>(R.id.compatibility)!!

        val statefulViews: Sequence<View>
            get() = sequenceOf(
                itemView,
                version,
                status,
                source,
                added,
                size,
                signature,
                compatibility
            )

        init {
            status.apply {
                background =
                    ResourcesCompat.getDrawable(
                        itemView.resources,
                        R.drawable.background_border,
                        itemView.context.theme
                    )
                backgroundTintList = itemView.context.getColorFromAttr(R.attr.colorSurface)
                typeface = TypefaceExtra.bold
                setPadding(15, 5, 15, 5)
                setTextColor(itemView.context.getColorFromAttr(R.attr.colorPrimary))
            }
        }
    }

    private class EmptyViewHolder(context: Context) :
        RecyclerView.ViewHolder(LinearLayoutCompat(context)) {
        val packageName: MaterialTextView

        init {
            itemView as LinearLayoutCompat
            itemView.orientation = LinearLayoutCompat.VERTICAL
            itemView.gravity = Gravity.CENTER
            itemView.resources.sizeScaled(20).let { itemView.setPadding(it, it, it, it) }
            val title = MaterialTextView(itemView.context)
            title.gravity = Gravity.CENTER
            title.typeface = TypefaceExtra.light
            title.setTextColor(context.getColorFromAttr(R.attr.colorPrimary))
            title.setTextSizeScaled(24)
            title.setText(R.string.application_not_found)
            itemView.addView(
                title,
                LinearLayoutCompat.LayoutParams.MATCH_PARENT,
                LinearLayoutCompat.LayoutParams.WRAP_CONTENT
            )
            val packageName = MaterialTextView(itemView.context)
            packageName.gravity = Gravity.CENTER
            packageName.setTextColor(context.getColorFromAttr(R.attr.colorPrimary))
            packageName.setTextSizeScaled(18)
            itemView.addView(
                packageName,
                LinearLayoutCompat.LayoutParams.MATCH_PARENT,
                LinearLayoutCompat.LayoutParams.WRAP_CONTENT
            )
            itemView.layoutParams = RecyclerView.LayoutParams(
                RecyclerView.LayoutParams.MATCH_PARENT,
                RecyclerView.LayoutParams.MATCH_PARENT
            )
            this.packageName = packageName
        }
    }

    val gridItemDecoration = object : RecyclerView.ItemDecoration() {
        private fun decorateScreenshot(
            outRect: Rect,
            parent: RecyclerView,
            position: Int,
            count: Int,
        ) {
            val column = position % columns
            val rows = (count + columns - 1) / columns
            val row = position / columns
            val outer = parent.resources.sizeScaled(GRID_SPACING_OUTER_DP)
            val inner = parent.resources.sizeScaled(GRID_SPACING_INNER_DP)
            val total = 2 * outer + (columns - 1) * inner
            // Each column should have this spacing
            val average = total / columns
            // Start spacing is in the range from [outer] to [average - outer]
            // Interpolate between these values
            val start =
                (outer - ((2 * outer - average) * (column.toFloat() / (columns - 1)))).roundToInt()
            val end = average - start
            val bottom = if (row + 1 >= rows) outer else 0
            val rtl = parent.layoutDirection == View.LAYOUT_DIRECTION_RTL
            if (rtl) {
                outRect.set(end, inner, start, bottom)
            } else {
                outRect.set(start, inner, end, bottom)
            }
        }

        override fun getItemOffsets(
            outRect: Rect,
            view: View,
            parent: RecyclerView,
            state: RecyclerView.State,
        ) {
            val holder = parent.getChildViewHolder(view)
            if (holder is ScreenshotViewHolder) {
                val position = holder.adapterPosition
                if (position >= 0) {
                    val first =
                        items.subList(0, position).indexOfLast { it !is Item.ScreenshotItem } + 1
                    val gridCount =
                        items.subList(first, items.size).indexOfFirst { it !is Item.ScreenshotItem }
                            .let { if (it < 0) items.size - first else it }
                    val gridPosition = position - first
                    decorateScreenshot(outRect, parent, gridPosition, gridCount)
                    holder.gridPosition = gridPosition
                    holder.gridCount = gridCount
                } else if (position == -1 && holder.gridPosition >= 0 && holder.gridCount >= 0) {
                    decorateScreenshot(outRect, parent, holder.gridPosition, holder.gridCount)
                }
            }
        }
    }

    fun requiresGrid(position: Int): Boolean {
        return items[position] is Item.ScreenshotItem
    }

    private val items = mutableListOf<Item>()
    private val expanded = mutableSetOf<ExpandType>()
    private var product: Product? = null
    private var installedItem: InstalledItem? = null

    fun setProducts(
        context: Context, packageName: String,
        products: List<Pair<Product, Repository>>, installedItem: InstalledItem?,
    ) {
        val productRepository = Product.findSuggested(products, installedItem) { it.first }
        items.clear()

        if (productRepository != null) {
            items += Item.AppInfoItem(productRepository.second, productRepository.first)

            if (installedItem != null) {
                items.add(
                    Item.SwitchItem(
                        SwitchType.IGNORE_ALL_UPDATES,
                        packageName,
                        productRepository.first.versionCode
                    )
                )
                if (productRepository.first.canUpdate(installedItem)) {
                    items.add(
                        Item.SwitchItem(
                            SwitchType.IGNORE_THIS_UPDATE,
                            packageName,
                            productRepository.first.versionCode
                        )
                    )
                }
            }

            val textViewHolder = TextViewHolder(context)
            val textViewWidthSpec = context.resources.displayMetrics.widthPixels
                .let { View.MeasureSpec.makeMeasureSpec(it, View.MeasureSpec.EXACTLY) }
            val textViewHeightSpec =
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)

            fun CharSequence.lineCropped(maxLines: Int, cropLines: Int): CharSequence? {
                assert(cropLines <= maxLines)
                textViewHolder.text.text = this
                textViewHolder.itemView.measure(textViewWidthSpec, textViewHeightSpec)
                textViewHolder.itemView.layout(
                    0, 0, textViewHolder.itemView.measuredWidth,
                    textViewHolder.itemView.measuredHeight
                )
                val layout = textViewHolder.text.layout
                val cropLineOffset =
                    if (layout.lineCount <= maxLines) -1 else layout.getLineEnd(cropLines - 1)
                val paragraphEndIndex = if (cropLineOffset < 0) -1 else
                    indexOf("\n\n", cropLineOffset).let { if (it >= 0) it else length }
                val paragraphEndLine = if (paragraphEndIndex < 0) -1 else
                    layout.getLineForOffset(paragraphEndIndex).apply { assert(this >= 0) }
                val end = when {
                    cropLineOffset < 0 -> -1
                    paragraphEndLine >= 0 && paragraphEndLine - (cropLines - 1) <= 3 ->
                        if (paragraphEndIndex < length) paragraphEndIndex else -1
                    else -> cropLineOffset
                }
                val length = if (end < 0) -1 else asSequence().take(end)
                    .indexOfLast { it != '\n' }.let { if (it >= 0) it + 1 else end }
                return if (length >= 0) subSequence(0, length) else null
            }

            val description = formatHtml(productRepository.first.description).apply {
                if (productRepository.first.let { it.summary.isNotEmpty() && it.name != it.summary }) {
                    if (isNotEmpty()) {
                        insert(0, "\n\n")
                    }
                    insert(0, productRepository.first.summary)
                    if (isNotEmpty()) {
                        setSpan(
                            TypefaceSpan("sans-serif-medium"),
                            0,
                            productRepository.first.summary.length,
                            SpannableStringBuilder.SPAN_EXCLUSIVE_EXCLUSIVE
                        )
                    }
                }
            }
            if (description.isNotEmpty()) {
                val cropped = if (ExpandType.DESCRIPTION !in expanded) description.lineCropped(
                    12,
                    10
                ) else null
                val item = Item.TextItem(TextType.DESCRIPTION, description)
                if (cropped != null) {
                    val croppedItem = Item.TextItem(TextType.DESCRIPTION, cropped)
                    items += listOf(
                        croppedItem,
                        Item.ExpandItem(ExpandType.DESCRIPTION, true, listOf(item, croppedItem))
                    )
                } else {
                    items += item
                }
            }

            val antiFeatures = productRepository.first.antiFeatures.map {
                when (it) {
                    "Ads" -> context.getString(R.string.has_advertising)
                    "ApplicationDebuggable" -> context.getString(R.string.compiled_for_debugging)
                    "DisabledAlgorithm" -> context.getString(R.string.signed_using_unsafe_algorithm)
                    "KnownVuln" -> context.getString(R.string.has_security_vulnerabilities)
                    "NoSourceSince" -> context.getString(R.string.source_code_no_longer_available)
                    "NonFreeAdd" -> context.getString(R.string.promotes_non_free_software)
                    "NonFreeAssets" -> context.getString(R.string.contains_non_free_media)
                    "NonFreeDep" -> context.getString(R.string.has_non_free_dependencies)
                    "NonFreeNet" -> context.getString(R.string.promotes_non_free_network_services)
                    "Tracking" -> context.getString(R.string.tracks_or_reports_your_activity)
                    "UpstreamNonFree" -> context.getString(R.string.upstream_source_code_is_not_free)
                    else -> context.getString(R.string.unknown_FORMAT, it)
                }
            }.joinToString(separator = "\n") { "\u2022 $it" }
            if (antiFeatures.isNotEmpty()) {
                items += Item.SectionItem(SectionType.ANTI_FEATURES)
                items += Item.TextItem(TextType.ANTI_FEATURES, antiFeatures)
            }

            val changes = formatHtml(productRepository.first.whatsNew)
            if (changes.isNotEmpty()) {
                items += Item.SectionItem(SectionType.CHANGES)
                val cropped =
                    if (ExpandType.CHANGES !in expanded) changes.lineCropped(12, 10) else null
                val item = Item.TextItem(TextType.CHANGES, changes)
                if (cropped != null) {
                    val croppedItem = Item.TextItem(TextType.CHANGES, cropped)
                    items += listOf(
                        croppedItem,
                        Item.ExpandItem(ExpandType.CHANGES, true, listOf(item, croppedItem))
                    )
                } else {
                    items += item
                }
            }

            val linkItems = mutableListOf<Item>()
            productRepository.first.apply {
                if (author.name.isNotEmpty() || author.web.isNotEmpty()) {
                    linkItems += Item.LinkItem.Typed(
                        LinkType.AUTHOR,
                        author.name,
                        author.web.nullIfEmpty()?.let(Uri::parse)
                    )
                }
                author.email.nullIfEmpty()?.let {
                    linkItems += Item.LinkItem
                        .Typed(LinkType.EMAIL, "", Uri.parse("mailto:$it"))
                }
                linkItems += licenses.asSequence().map {
                    Item.LinkItem.Typed(
                        LinkType.LICENSE, it,
                        Uri.parse("https://spdx.org/licenses/$it.html")
                    )
                }
                tracker.nullIfEmpty()
                    ?.let { linkItems += Item.LinkItem.Typed(LinkType.TRACKER, "", Uri.parse(it)) }
                changelog.nullIfEmpty()?.let {
                    linkItems += Item.LinkItem.Typed(
                        LinkType.CHANGELOG,
                        "",
                        Uri.parse(it)
                    )
                }
                web.nullIfEmpty()
                    ?.let { linkItems += Item.LinkItem.Typed(LinkType.WEB, "", Uri.parse(it)) }
            }
            if (linkItems.isNotEmpty()) {
                if (ExpandType.LINKS in expanded) {
                    items += Item.SectionItem(
                        SectionType.LINKS,
                        ExpandType.LINKS,
                        emptyList(),
                        linkItems.size
                    )
                    items += linkItems
                } else {
                    items += Item.SectionItem(SectionType.LINKS, ExpandType.LINKS, linkItems, 0)
                }
            }

            val donateItems = productRepository.first.donates.map(Item.LinkItem::Donate)
            if (donateItems.isNotEmpty()) {
                if (ExpandType.DONATES in expanded) {
                    items += Item.SectionItem(
                        SectionType.DONATE,
                        ExpandType.DONATES,
                        emptyList(),
                        donateItems.size
                    )
                    items += donateItems
                } else {
                    items += Item.SectionItem(
                        SectionType.DONATE,
                        ExpandType.DONATES,
                        donateItems,
                        0
                    )
                }
            }

            val release = productRepository.first.displayRelease
            if (release != null) {
                val packageManager = context.packageManager
                val permissions = release.permissions
                    .asSequence().mapNotNull {
                        try {
                            packageManager.getPermissionInfo(it, 0)
                        } catch (e: Exception) {
                            null
                        }
                    }
                    .groupBy(PackageItemResolver::getPermissionGroup)
                    .asSequence().map { (group, permissionInfo) ->
                        val permissionGroupInfo = try {
                            group?.let { packageManager.getPermissionGroupInfo(it, 0) }
                        } catch (e: Exception) {
                            null
                        }
                        Pair(permissionGroupInfo, permissionInfo)
                    }
                    .groupBy({ it.first }, { it.second })
                if (permissions.isNotEmpty()) {
                    val permissionsItems = mutableListOf<Item>()
                    permissionsItems += permissions.asSequence().filter { it.key != null }
                        .map { Item.PermissionsItem(it.key, it.value.flatten()) }
                    permissions.asSequence().find { it.key == null }
                        ?.let { permissionsItems += Item.PermissionsItem(null, it.value.flatten()) }
                    if (ExpandType.PERMISSIONS in expanded) {
                        items += Item.SectionItem(
                            SectionType.PERMISSIONS, ExpandType.PERMISSIONS,
                            emptyList(), permissionsItems.size
                        )
                        items += permissionsItems
                    } else {
                        items += Item.SectionItem(
                            SectionType.PERMISSIONS,
                            ExpandType.PERMISSIONS,
                            permissionsItems,
                            0
                        )
                    }
                }
            }
        }

        val incompatible = Preferences[Preferences.Key.IncompatibleVersions]
        val compatibleReleasePairs = products.asSequence()
            .flatMap { (product, repository) ->
                product.releases.asSequence()
                    .filter { incompatible || it.incompatibilities.isEmpty() }
                    .map { Pair(it, repository) }
            }
            .toList()
        val signaturesForVersionCode = compatibleReleasePairs.asSequence()
            .mapNotNull { (release, _) ->
                if (release.signature.isEmpty()) null else
                    Pair(release.versionCode, release.signature)
            }
            .distinct().groupBy { it.first }.toMap()
        val releaseItems = compatibleReleasePairs.asSequence()
            .map { (release, repository) ->
                Item.ReleaseItem(
                    repository, release,
                    repository.id == productRepository?.second?.id,
                    signaturesForVersionCode[release.versionCode].orEmpty().size >= 2
                )
            }
            .sortedByDescending { it.release.versionCode }
            .toList()
        if (releaseItems.isNotEmpty()) {
            items += Item.SectionItem(SectionType.VERSIONS)
            val maxReleases = 5
            if (releaseItems.size > maxReleases && ExpandType.VERSIONS !in expanded) {
                items += releaseItems.take(maxReleases)
                items += Item.ExpandItem(
                    ExpandType.VERSIONS,
                    false,
                    releaseItems.takeLast(releaseItems.size - maxReleases)
                )
            } else {
                items += releaseItems
            }
        }

        if (items.isEmpty()) {
            items += Item.EmptyItem(packageName)
        }
        this.product = productRepository?.first
        this.installedItem = installedItem
        notifyDataSetChanged()
    }

    private var action: Action? = null

    fun setAction(action: Action?) {
        if (this.action != action) {
            val translate = this.action == null || action == null ||
                    this.action == Action.CANCEL || action == Action.CANCEL
            this.action = action
            val index = items.indexOfFirst { it is Item.AppInfoItem }
            if (index >= 0) {
                if (translate) {
                    notifyItemChanged(index)
                } else {
                    notifyItemChanged(index, Payload.REFRESH)
                }
            }
        }
    }

    private var status: Status? = null

    fun setStatus(status: Status?) {
        val translate = (this.status == null) != (status == null)
        if (this.status != status) {
            this.status = status
            val index = items.indexOfFirst { it is Item.AppInfoItem }
            if (index >= 0) {
                if (translate) {
                    notifyItemChanged(index)
                    val from = items.indexOfFirst { it is Item.ReleaseItem }
                    val to = items.indexOfLast { it is Item.ReleaseItem }
                    if (from in 0..to) {
                        notifyItemRangeChanged(from, to - from + 1)
                    }
                } else {
                    notifyItemChanged(index, Payload.STATUS)
                }
            }
        }
    }

    override val viewTypeClass: Class<ViewType>
        get() = ViewType::class.java

    override fun getItemCount(): Int = items.size
    override fun getItemDescriptor(position: Int): String = items[position].descriptor
    override fun getItemEnumViewType(position: Int): ViewType = items[position].viewType

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: ViewType,
    ): RecyclerView.ViewHolder {
        return when (viewType) {
            ViewType.APP_INFO -> AppInfoViewHolder(parent.inflate(R.layout.item_app_info_x)).apply {
                action.setOnClickListener { this@ProductAdapter.action?.let(callbacks::onActionClick) }
            }
            ViewType.SWITCH -> SwitchViewHolder(parent.inflate(R.layout.switch_item)).apply {
                itemView.setOnClickListener {
                    val switchItem = items[adapterPosition] as Item.SwitchItem
                    val productPreference = when (switchItem.switchType) {
                        SwitchType.IGNORE_ALL_UPDATES -> {
                            ProductPreferences[switchItem.packageName].let { it.copy(ignoreUpdates = !it.ignoreUpdates) }
                        }
                        SwitchType.IGNORE_THIS_UPDATE -> {
                            ProductPreferences[switchItem.packageName].let {
                                it.copy(
                                    ignoreVersionCode =
                                    if (it.ignoreVersionCode == switchItem.versionCode) 0 else switchItem.versionCode
                                )
                            }
                        }
                    }
                    ProductPreferences[switchItem.packageName] = productPreference
                    items.asSequence().mapIndexedNotNull { index, item ->
                        if (item is Item.AppInfoItem ||
                            item is Item.SectionItem
                        ) index else null
                    }.forEach { notifyItemChanged(it, Payload.REFRESH) }
                    callbacks.onPreferenceChanged(productPreference)
                }
            }
            ViewType.SECTION -> SectionViewHolder(parent.inflate(R.layout.section_item)).apply {
                itemView.setOnClickListener {
                    val position = adapterPosition
                    val sectionItem = items[position] as Item.SectionItem
                    if (sectionItem.items.isNotEmpty()) {
                        expanded += sectionItem.expandType
                        items[position] = Item.SectionItem(
                            sectionItem.sectionType, sectionItem.expandType, emptyList(),
                            sectionItem.items.size + sectionItem.collapseCount
                        )
                        notifyItemChanged(adapterPosition, Payload.REFRESH)
                        items.addAll(position + 1, sectionItem.items)
                        notifyItemRangeInserted(position + 1, sectionItem.items.size)
                    } else if (sectionItem.collapseCount > 0) {
                        expanded -= sectionItem.expandType
                        items[position] = Item.SectionItem(
                            sectionItem.sectionType, sectionItem.expandType,
                            items.subList(position + 1, position + 1 + sectionItem.collapseCount)
                                .toList(), 0
                        )
                        notifyItemChanged(adapterPosition, Payload.REFRESH)
                        repeat(sectionItem.collapseCount) { items.removeAt(position + 1) }
                        notifyItemRangeRemoved(position + 1, sectionItem.collapseCount)
                    }
                }
            }
            ViewType.EXPAND -> ExpandViewHolder(parent.context).apply {
                itemView.setOnClickListener {
                    val position = adapterPosition
                    val expandItem = items[position] as Item.ExpandItem
                    if (expandItem.expandType !in expanded) {
                        expanded += expandItem.expandType
                        if (expandItem.replace) {
                            items[position - 1] = expandItem.items[0]
                            notifyItemRangeChanged(
                                position - expandItem.items.size,
                                expandItem.items.size
                            )
                            notifyItemChanged(position, Payload.REFRESH)
                        } else {
                            items.addAll(position, expandItem.items)
                            if (position > 0) {
                                // Update decorations
                                notifyItemChanged(position - 1, Payload.REFRESH)
                            }
                            notifyItemChanged(position, Payload.REFRESH)
                            notifyItemRangeInserted(position, expandItem.items.size)
                        }
                    } else {
                        expanded -= expandItem.expandType
                        if (expandItem.replace) {
                            items[position - 1] = expandItem.items[1]
                            notifyItemRangeChanged(
                                position - expandItem.items.size,
                                expandItem.items.size
                            )
                            notifyItemChanged(position, Payload.REFRESH)
                        } else {
                            items.removeAt(position - 1)
                            if (position > 0) {
                                // Update decorations
                                notifyItemRemoved(position - 1)
                                notifyItemChanged(position - 1, Payload.REFRESH)
                            }
                        }
                    }
                }
            }
            ViewType.TEXT -> TextViewHolder(parent.context)
            ViewType.LINK -> LinkViewHolder(parent.inflate(R.layout.link_item)).apply {
                itemView.setOnClickListener {
                    val linkItem = items[adapterPosition] as Item.LinkItem
                    if (linkItem.uri?.let { callbacks.onUriClick(it, false) } != true) {
                        linkItem.displayLink?.let { copyLinkToClipboard(itemView, it) }
                    }
                }
                itemView.setOnLongClickListener {
                    val linkItem = items[adapterPosition] as Item.LinkItem
                    linkItem.displayLink?.let { copyLinkToClipboard(itemView, it) }
                    true
                }
            }
            ViewType.PERMISSIONS -> PermissionsViewHolder(parent.inflate(R.layout.permissions_item)).apply {
                itemView.setOnClickListener {
                    val permissionsItem = items[adapterPosition] as Item.PermissionsItem
                    callbacks.onPermissionsClick(
                        permissionsItem.group?.name,
                        permissionsItem.permissions.map { it.name })
                }
            }
            ViewType.RELEASE -> ReleaseViewHolder(parent.inflate(R.layout.release_item)).apply {
                itemView.setOnClickListener {
                    val releaseItem = items[adapterPosition] as Item.ReleaseItem
                    callbacks.onReleaseClick(releaseItem.release)
                }
                itemView.setOnLongClickListener {
                    val releaseItem = items[adapterPosition] as Item.ReleaseItem
                    copyLinkToClipboard(itemView,
                        releaseItem.release.getDownloadUrl(releaseItem.repository))
                    true
                }
            }
            ViewType.EMPTY -> EmptyViewHolder(parent.context)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        onBindViewHolder(holder, position, emptyList())
    }

    override fun onBindViewHolder(
        holder: RecyclerView.ViewHolder,
        position: Int,
        payloads: List<Any>,
    ) {
        val context = holder.itemView.context
        val item = items[position]
        when (getItemEnumViewType(position)) {
            ViewType.APP_INFO -> {
                holder as AppInfoViewHolder
                item as Item.AppInfoItem
                val updateStatus = Payload.STATUS in payloads
                val updateAll = !updateStatus
                if (updateAll) {
                    holder.icon.load(
                        CoilDownloader.createIconUri(
                            holder.icon, item.product.packageName,
                            item.product.icon, item.product.metadataIcon, item.repository
                        )
                    ) {
                        placeholder(holder.progressIcon)
                        error(holder.defaultIcon)
                    }
                    holder.name.text = item.product.name
                    holder.packageName.apply {
                        text = item.product.packageName
                    }
                    val action = action
                    holder.action.apply {
                        visibility = if (action == null) View.GONE else View.VISIBLE
                        if (action != null) {
                            setText(action.titleResId)
                            setTextColor(
                                if (action == Action.CANCEL) holder.actionTintOnCancel
                                else holder.actionTintOnNormal
                            )
                        }
                        if (Android.sdk(22)) {
                            backgroundTintList = if (action == Action.CANCEL)
                                holder.actionTintCancel else holder.actionTintNormal
                        }
                        icon =
                            when (action) {
                                Action.CANCEL -> context.getDrawable(R.drawable.ic_cancel)
                                Action.LAUNCH -> context.getDrawable(R.drawable.ic_launch)
                                Action.DETAILS -> context.getDrawable(R.drawable.ic_tune)
                                else -> context.getDrawable(R.drawable.ic_download)
                            }
                        iconTint = if (action == Action.CANCEL) holder.actionTintOnCancel
                        else holder.actionTintOnNormal
                    }
                }
                if (updateAll || updateStatus) {
                    val status = status
                    holder.statusLayout.visibility =
                        if (status != null) View.VISIBLE else View.INVISIBLE
                    if (status != null) {
                        when (status) {
                            is Status.Pending -> {
                                holder.status.setText(R.string.waiting_to_start_download)
                                holder.progress.isIndeterminate = true
                            }
                            is Status.Connecting -> {
                                holder.status.setText(R.string.connecting)
                                holder.progress.isIndeterminate = true
                            }
                            is Status.Downloading -> {
                                holder.status.text = context.getString(
                                    R.string.downloading_FORMAT, if (status.total == null)
                                        status.read.formatSize() else "${status.read.formatSize()} / ${status.total.formatSize()}"
                                )
                                holder.progress.isIndeterminate = status.total == null
                                if (status.total != null) {
                                    holder.progress.progress =
                                        (holder.progress.max.toFloat() * status.read / status.total).roundToInt()
                                }
                                Unit
                            }
                        }::class
                    }
                }

                val imageSource = product?.source?.trimAfter('/', 4).plus(".png").toUri()
                val sdk = product?.displayRelease?.targetSdkVersion

                holder.version.doOnPreDraw {
                    if (it.measuredWidth > 70 || sdk == 0) {
                        holder.targetBlock.visibility = View.GONE
                        holder.divider1.visibility = View.GONE
                    }
                }

                holder.targetSdk.text = sdk.toString()
                holder.version.text = product?.displayRelease?.version
                holder.size.text = product?.displayRelease?.size?.formatSize()
                val author = product?.author?.name?.replaceFirstChar {
                    if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString()
                }
                val devName = product?.source?.trimAfter('/', 4).trimBefore('/', 3)
                holder.devName.text = if (author.isNullOrEmpty()) devName else author
                when {
                    imageSource.toString()
                        .contains("kde.org") -> holder.devIcon.setImageResource(R.drawable.ic_kde)
                    imageSource.toString()
                        .contains("gitlab") -> holder.devIcon.setImageResource(R.drawable.ic_gitlab)
                    imageSource.toString().contains("github") -> holder.devIcon.load(imageSource)
                }
                holder.dev.setOnClickListener {
                    product?.source?.let { link ->
                        if (link.isNotEmpty()) {
                            context.startActivity(Intent(Intent.ACTION_VIEW, link.toUri()))
                        }
                    }
                }
                holder.dev.setOnLongClickListener {
                    product?.source?.let { link ->
                        if (link.isNotEmpty()) {
                            copyLinkToClipboard(holder.dev, link)
                        }
                    }
                    true
                }
            }
            ViewType.SWITCH -> {
                holder as SwitchViewHolder
                item as Item.SwitchItem
                val (checked, enabled) = when (item.switchType) {
                    SwitchType.IGNORE_ALL_UPDATES -> {
                        val productPreference = ProductPreferences[item.packageName]
                        Pair(productPreference.ignoreUpdates, true)
                    }
                    SwitchType.IGNORE_THIS_UPDATE -> {
                        val productPreference = ProductPreferences[item.packageName]
                        Pair(
                            productPreference.ignoreUpdates || productPreference.ignoreVersionCode == item.versionCode,
                            !productPreference.ignoreUpdates
                        )
                    }
                }
                holder.enabled.setText(item.switchType.titleResId)
                holder.enabled.isChecked = checked
                holder.statefulViews.forEach { it.isEnabled = enabled }
            }
            ViewType.SECTION -> {
                holder as SectionViewHolder
                item as Item.SectionItem
                val expandable = item.items.isNotEmpty() || item.collapseCount > 0
                holder.itemView.isEnabled = expandable
                holder.itemView.let {
                    it.setPadding(
                        it.paddingLeft, it.paddingTop, it.paddingRight,
                        if (expandable) it.paddingTop else 0
                    )
                }
                val color = context.getColorFromAttr(item.sectionType.colorAttrResId)
                holder.title.setTextColor(color)
                holder.title.text = context.getString(item.sectionType.titleResId)
                holder.icon.visibility = if (expandable) View.VISIBLE else View.GONE
                holder.icon.scaleY = if (item.collapseCount > 0) -1f else 1f
                holder.icon.imageTintList = color
            }
            ViewType.EXPAND -> {
                holder as ExpandViewHolder
                item as Item.ExpandItem
                holder.text.text = if (item.expandType !in expanded) {
                    when (item.expandType) {
                        ExpandType.VERSIONS -> context.getString(R.string.show_older_versions)
                        else -> context.getString(R.string.show_more)
                    }
                } else {
                    "Show Less"
                }
            }
            ViewType.TEXT -> {
                holder as TextViewHolder
                item as Item.TextItem
                holder.text.text = item.text
            }
            ViewType.LINK -> {
                holder as LinkViewHolder
                item as Item.LinkItem
                val layoutParams = holder.itemView.layoutParams as RecyclerView.LayoutParams
                layoutParams.topMargin = if (position > 0 && items[position - 1] !is Item.LinkItem)
                    -context.resources.sizeScaled(8) else 0
                holder.itemView.isEnabled = item.uri != null
                holder.icon.setImageResource(item.iconResId)
                holder.text.text = item.getTitle(context)
                holder.link.visibility = if (item.uri != null) View.VISIBLE else View.GONE
                holder.link.text = item.displayLink
            }
            ViewType.PERMISSIONS -> {
                holder as PermissionsViewHolder
                item as Item.PermissionsItem
                val layoutParams = holder.itemView.layoutParams as RecyclerView.LayoutParams
                layoutParams.topMargin =
                    if (position > 0 && items[position - 1] !is Item.PermissionsItem)
                        -context.resources.sizeScaled(8) else 0
                val packageManager = context.packageManager
                holder.icon.setImageDrawable(
                    if (item.group != null && item.group.icon != 0) {
                        if (Android.sdk(22)) {
                            item.group.loadUnbadgedIcon(packageManager)
                        } else {
                            item.group.loadIcon(packageManager)
                        }
                    } else {
                        null
                    } ?: context.getDrawableCompat(R.drawable.ic_perm_device_information)
                )
                val localCache = PackageItemResolver.LocalCache()
                val labels = item.permissions.map { permission ->
                    val labelFromPackage =
                        PackageItemResolver.loadLabel(context, localCache, permission)
                    val label = labelFromPackage ?: run {
                        val prefixes =
                            listOf("android.permission.", "com.android.browser.permission.")
                        prefixes.find { permission.name.startsWith(it) }?.let { it ->
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
                        Pair(true, label.first().uppercaseChar() + label.substring(1, label.length))
                    }
                }
                val builder = SpannableStringBuilder()
                (labels.asSequence().filter { it.first } + labels.asSequence()
                    .filter { !it.first }).forEach {
                    if (builder.isNotEmpty()) {
                        builder.append("\n\n")
                        builder.setSpan(
                            RelativeSizeSpan(1f / 3f), builder.length - 2, builder.length,
                            SpannableStringBuilder.SPAN_EXCLUSIVE_EXCLUSIVE
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
                                    DotSpan(), index, index + 1,
                                    SpannableStringBuilder.SPAN_EXCLUSIVE_EXCLUSIVE
                                )
                            }
                    }
                }
                holder.text.text = builder
            }
            ViewType.RELEASE -> {
                holder as ReleaseViewHolder
                item as Item.ReleaseItem
                val incompatibility = item.release.incompatibilities.firstOrNull()
                val singlePlatform =
                    if (item.release.platforms.size == 1) item.release.platforms.first() else null
                val installed = installedItem?.versionCode == item.release.versionCode &&
                        installedItem?.signature == item.release.signature
                val suggested =
                    incompatibility == null && item.release.selected && item.selectedRepository
                val olderOrSignature = installedItem?.let {
                    it.versionCode > item.release.versionCode ||
                            it.signature != item.release.signature
                } == true
                val grayOut = incompatibility != null || olderOrSignature
                val primarySecondaryColor = context.getColorFromAttr(
                    if (grayOut)
                        android.R.attr.textColorSecondary else android.R.attr.textColor
                )
                holder.version.text =
                    context.getString(R.string.version_FORMAT, item.release.version)
                holder.version.setTextColor(primarySecondaryColor)
                holder.status.visibility = if (installed || suggested) View.VISIBLE else View.GONE
                holder.status.setText(
                    when {
                        installed -> R.string.installed
                        suggested -> R.string.suggested
                        else -> R.string.unknown
                    }
                )
                holder.source.text =
                    context.getString(R.string.provided_by_FORMAT, item.repository.name)
                holder.added.text = holder.dateFormat.format(item.release.added)
                holder.added.setTextColor(primarySecondaryColor)
                holder.size.text = item.release.size.formatSize()
                holder.signature.visibility =
                    if (item.showSignature && item.release.signature.isNotEmpty())
                        View.VISIBLE else View.GONE
                if (item.showSignature && item.release.signature.isNotEmpty()) {
                    val bytes =
                        item.release.signature.uppercase(Locale.US).windowed(2, 2, false).take(8)
                    val signature = bytes.joinToString(separator = " ")
                    val builder = SpannableStringBuilder(
                        context.getString(
                            R.string.signature_FORMAT,
                            signature
                        )
                    )
                    val index = builder.indexOf(signature)
                    if (index >= 0) {
                        bytes.forEachIndexed { i, _ ->
                            builder.setSpan(
                                TypefaceSpan("monospace"),
                                index + 3 * i,
                                index + 3 * i + 2,
                                SpannableStringBuilder.SPAN_EXCLUSIVE_EXCLUSIVE
                            )
                        }
                    }
                    holder.signature.text = builder
                }
                holder.compatibility.visibility =
                    if (incompatibility != null || singlePlatform != null)
                        View.VISIBLE else View.GONE
                if (incompatibility != null) {
                    holder.compatibility.setTextColor(context.getColorFromAttr(R.attr.colorError))
                    holder.compatibility.text = when (incompatibility) {
                        is Release.Incompatibility.MinSdk,
                        is Release.Incompatibility.MaxSdk,
                        -> context.getString(
                            R.string.incompatible_with_FORMAT,
                            Android.name
                        )
                        is Release.Incompatibility.Platform -> context.getString(
                            R.string.incompatible_with_FORMAT,
                            Android.primaryPlatform ?: context.getString(R.string.unknown)
                        )
                        is Release.Incompatibility.Feature -> context.getString(
                            R.string.requires_FORMAT,
                            incompatibility.feature
                        )
                    }
                } else if (singlePlatform != null) {
                    holder.compatibility.setTextColor(context.getColorFromAttr(android.R.attr.textColorSecondary))
                    holder.compatibility.text =
                        context.getString(R.string.only_compatible_with_FORMAT, singlePlatform)
                }
                val enabled = status == null
                holder.statefulViews.forEach { it.isEnabled = enabled }
            }
            ViewType.EMPTY -> {
                holder as EmptyViewHolder
                item as Item.EmptyItem
                holder.packageName.text = item.packageName
            }
        }::class
    }

    private fun formatHtml(text: String): SpannableStringBuilder {
        val html = HtmlCompat.fromHtml(text, HtmlCompat.FROM_HTML_MODE_LEGACY)
        val builder = run {
            val builder = SpannableStringBuilder(html)
            val last = builder.indexOfLast { it != '\n' }
            val first = builder.indexOfFirst { it != '\n' }
            if (last >= 0) {
                builder.delete(last + 1, builder.length)
            }
            if (first in 1 until last) {
                builder.delete(0, first - 1)
            }
            generateSequence(builder) {
                val index = it.indexOf("\n\n\n")
                if (index >= 0) it.delete(index, index + 1) else null
            }.last()
        }
        LinkifyCompat.addLinks(builder, Linkify.WEB_URLS or Linkify.EMAIL_ADDRESSES)
        val urlSpans = builder.getSpans(0, builder.length, URLSpan::class.java).orEmpty()
        for (span in urlSpans) {
            val start = builder.getSpanStart(span)
            val end = builder.getSpanEnd(span)
            val flags = builder.getSpanFlags(span)
            builder.removeSpan(span)
            builder.setSpan(LinkSpan(span.url, this), start, end, flags)
        }
        val bulletSpans = builder.getSpans(0, builder.length, BulletSpan::class.java).orEmpty()
            .asSequence().map { Pair(it, builder.getSpanStart(it)) }
            .sortedByDescending { it.second }
        for (spanPair in bulletSpans) {
            val (span, start) = spanPair
            builder.removeSpan(span)
            builder.insert(start, "\u2022 ")
        }
        return builder
    }

    private fun copyLinkToClipboard(view: View, link: String) {
        val clipboardManager =
            view.context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboardManager.setPrimaryClip(ClipData.newPlainText(null, link))
        Snackbar.make(view, R.string.link_copied_to_clipboard, Snackbar.LENGTH_SHORT).show()
    }

    private class LinkSpan(private val url: String, productAdapter: ProductAdapter) :
        ClickableSpan() {
        private val productAdapterReference = WeakReference(productAdapter)

        override fun onClick(view: View) {
            val productAdapter = productAdapterReference.get()
            val uri = try {
                Uri.parse(url)
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
            if (productAdapter != null && uri != null) {
                productAdapter.callbacks.onUriClick(uri, true)
            }
        }
    }

    private class DotSpan : ReplacementSpan() {
        override fun getSize(
            paint: Paint,
            text: CharSequence?,
            start: Int,
            end: Int,
            fm: Paint.FontMetricsInt?,
        ): Int {
            return paint.measureText(".").roundToInt()
        }

        override fun draw(
            canvas: Canvas, text: CharSequence?, start: Int, end: Int,
            x: Float, top: Int, y: Int, bottom: Int, paint: Paint,
        ) {
            canvas.drawText(".", x, y.toFloat(), paint)
        }
    }

    class SavedState internal constructor(internal val expanded: Set<ExpandType>) : KParcelable {
        override fun writeToParcel(dest: Parcel, flags: Int) {
            dest.writeStringList(expanded.map { it.name }.toList())
        }

        companion object {
            @Suppress("unused")
            @JvmField
            val CREATOR = KParcelable.creator {
                val expanded = it.createStringArrayList()!!.map(ExpandType::valueOf).toSet()
                SavedState(expanded)
            }
        }
    }

    fun saveState(): SavedState? {
        return if (expanded.isNotEmpty()) {
            SavedState(expanded)
        } else {
            null
        }
    }

    fun restoreState(savedState: SavedState) {
        expanded.clear()
        expanded += savedState.expanded
    }
}
