package com.looker.droidify.ui.app_detail

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
import android.net.Uri
import android.os.Parcel
import android.text.SpannableStringBuilder
import android.text.format.DateFormat
import android.text.method.LinkMovementMethod
import android.text.style.BulletSpan
import android.text.style.ClickableSpan
import android.text.style.RelativeSizeSpan
import android.text.style.ReplacementSpan
import android.text.style.TypefaceSpan
import android.text.style.URLSpan
import android.text.util.Linkify
import android.util.Log
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextSwitcher
import android.widget.TextView
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.core.content.res.ResourcesCompat
import androidx.core.net.toUri
import androidx.core.text.HtmlCompat
import androidx.core.text.bold
import androidx.core.text.buildSpannedString
import androidx.core.text.util.LinkifyCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.imageview.ShapeableImageView
import com.google.android.material.progressindicator.LinearProgressIndicator
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.switchmaterial.SwitchMaterial
import com.looker.core.common.extension.getColorFromAttr
import com.looker.core.common.extension.getDrawableCompat
import com.looker.core.common.extension.inflate
import com.looker.core.common.extension.setTextSizeScaled
import com.looker.core.common.file.KParcelable
import com.looker.core.common.formatSize
import com.looker.core.common.nullIfEmpty
import com.looker.core.datastore.UserPreferences
import com.looker.core.model.InstalledItem
import com.looker.core.model.Product
import com.looker.core.model.ProductPreference
import com.looker.core.model.Release
import com.looker.core.model.Repository
import com.looker.droidify.R
import com.looker.droidify.content.ProductPreferences
import com.looker.droidify.screen.ScreenshotsAdapter
import com.looker.droidify.utility.PackageItemResolver
import com.looker.droidify.utility.extension.android.Android
import com.looker.droidify.utility.extension.icon
import com.looker.droidify.utility.extension.resources.TypefaceExtra
import com.looker.droidify.utility.extension.resources.sizeScaled
import com.looker.droidify.widget.StableRecyclerAdapter
import java.lang.ref.WeakReference
import java.time.Instant
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.*
import kotlin.math.roundToInt
import com.google.android.material.R as MaterialR
import com.looker.core.common.R as CommonR
import com.looker.core.common.R.drawable as drawableRes
import com.looker.core.common.R.string as stringRes

class AppDetailAdapter(private val callbacks: Callbacks) :
	StableRecyclerAdapter<AppDetailAdapter.ViewType, RecyclerView.ViewHolder>() {

	interface Callbacks {
		fun onActionClick(action: Action)
		fun onFavouriteClicked()
		fun onPreferenceChanged(preference: ProductPreference)
		fun onPermissionsClick(group: String?, permissions: List<String>)
		fun onScreenshotClick(screenshot: Product.Screenshot)
		fun onReleaseClick(release: Release)
		fun onUriClick(uri: Uri, shouldConfirm: Boolean): Boolean
	}

	enum class Action(@StringRes val titleResId: Int, @DrawableRes val iconResId: Int) {
		INSTALL(stringRes.install, drawableRes.ic_download),
		UPDATE(stringRes.update, drawableRes.ic_download),
		LAUNCH(stringRes.launch, drawableRes.ic_launch),
		DETAILS(stringRes.details, drawableRes.ic_tune),
		UNINSTALL(stringRes.uninstall, drawableRes.ic_delete),
		CANCEL(stringRes.cancel, drawableRes.ic_cancel),
		SHARE(stringRes.share, drawableRes.ic_share)
	}

	sealed interface Status {
		object Idle : Status
		object Pending : Status
		object Connecting : Status
		data class Downloading(val read: Long, val total: Long?) : Status
		object PendingInstall : Status
		object Installing : Status
	}

	enum class ViewType { APP_INFO, DOWNLOAD_STATUS, INSTALL_BUTTON, SCREENSHOT, SWITCH, SECTION, EXPAND, TEXT, LINK, PERMISSIONS, RELEASE, EMPTY }

	private enum class SwitchType(val titleResId: Int) {
		IGNORE_ALL_UPDATES(stringRes.ignore_all_updates),
		IGNORE_THIS_UPDATE(stringRes.ignore_this_update)
	}

	private enum class SectionType(
		val titleResId: Int,
		val colorAttrResId: Int = MaterialR.attr.colorPrimary
	) {
		ANTI_FEATURES(stringRes.anti_features, MaterialR.attr.colorError),
		CHANGES(stringRes.changes),
		LINKS(stringRes.links),
		DONATE(stringRes.donate),
		PERMISSIONS(stringRes.permissions),
		VERSIONS(stringRes.versions)
	}

	internal enum class ExpandType {
		NOTHING, DESCRIPTION, CHANGES,
		LINKS, DONATES, PERMISSIONS, VERSIONS
	}

	private enum class TextType { DESCRIPTION, ANTI_FEATURES, CHANGES }

	private enum class LinkType(
		val iconResId: Int, val titleResId: Int,
		val format: ((Context, String) -> String)? = null,
	) {
		SOURCE(drawableRes.ic_code, stringRes.source_code),
		AUTHOR(drawableRes.ic_person, stringRes.author_website),
		EMAIL(drawableRes.ic_email, stringRes.author_email),
		LICENSE(drawableRes.ic_copyright, stringRes.license,
			format = { context, text -> context.getString(stringRes.license_FORMAT, text) }),
		TRACKER(drawableRes.ic_bug_report, stringRes.bug_tracker),
		CHANGELOG(drawableRes.ic_history, stringRes.changelog),
		WEB(drawableRes.ic_public, stringRes.project_website)
	}

	private sealed class Item {
		abstract val descriptor: String
		abstract val viewType: ViewType

		class AppInfoItem(
			val repository: Repository,
			val product: Product
		) : Item() {
			override val descriptor: String
				get() = "app_info.${product.name}"

			override val viewType: ViewType
				get() = ViewType.APP_INFO
		}

		object DownloadStatusItem : Item() {
			override val descriptor: String
				get() = "download_status"
			override val viewType: ViewType
				get() = ViewType.DOWNLOAD_STATUS
		}

		object InstallButtonItem : Item() {
			override val descriptor: String
				get() = "install_button"
			override val viewType: ViewType
				get() = ViewType.INSTALL_BUTTON

		}

		class ScreenshotItem(
			val screenshots: List<Product.Screenshot>,
			val packageName: String,
			val repository: Repository,
		) : Item() {
			override val descriptor: String
				get() = "screenshot.${screenshots.size}"
			override val viewType: ViewType
				get() = ViewType.SCREENSHOT

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
						is Product.Donate.Regular -> drawableRes.ic_donate_regular
						is Product.Donate.Bitcoin -> drawableRes.ic_donate_bitcoin
						is Product.Donate.Litecoin -> drawableRes.ic_donate_litecoin
						is Product.Donate.Flattr -> drawableRes.ic_donate_flattr
						is Product.Donate.Liberapay -> drawableRes.ic_donate_liberapay
						is Product.Donate.OpenCollective -> drawableRes.ic_donate_opencollective
					}

				override fun getTitle(context: Context): String = when (donate) {
					is Product.Donate.Regular -> context.getString(stringRes.website)
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
			val repository: Repository,
			val release: Release,
			val selectedRepository: Boolean,
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

	@Volatile
	private var isFavourite: Boolean = false

	private class AppInfoViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
		val icon = itemView.findViewById<ShapeableImageView>(R.id.app_icon)!!
		val name = itemView.findViewById<TextView>(R.id.app_name)!!
		val authorName = itemView.findViewById<TextView>(R.id.author_name)!!
		val packageName = itemView.findViewById<TextView>(R.id.package_name)!!
		val textSwitcher = itemView.findViewById<TextSwitcher>(R.id.author_package_name)!!

		init {
			textSwitcher.setInAnimation(itemView.context!!, R.anim.slide_right_fade_in)
			textSwitcher.setOutAnimation(itemView.context!!, R.anim.slide_right_fade_out)
		}

		val version = itemView.findViewById<TextView>(R.id.version)!!
		val size = itemView.findViewById<TextView>(R.id.size)!!
		val dev = itemView.findViewById<MaterialCardView>(R.id.dev_block)!!

		val favouriteButton = itemView.findViewById<MaterialButton>(R.id.favourite)!!
	}

	private class DownloadStatusViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
		val statusText = itemView.findViewById<TextView>(R.id.status)!!
		val progress = itemView.findViewById<LinearProgressIndicator>(R.id.progress)!!
	}

	private class InstallButtonViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
		val button = itemView.findViewById<MaterialButton>(R.id.action)!!

		val actionTintNormal = button.context.getColorFromAttr(MaterialR.attr.colorPrimary)
		val actionTintOnNormal = button.context.getColorFromAttr(MaterialR.attr.colorOnPrimary)
		val actionTintCancel = button.context.getColorFromAttr(MaterialR.attr.colorError)
		val actionTintOnCancel = button.context.getColorFromAttr(MaterialR.attr.colorOnError)
		val actionTintDisabled = button.context.getColorFromAttr(MaterialR.attr.colorOutline)
		val actionTintOnDisabled = button.context.getColorFromAttr(android.R.attr.colorBackground)

		init {
			button.height = itemView.resources.sizeScaled(48)
		}

	}

	private class ScreenShotViewHolder(context: Context) :
		RecyclerView.ViewHolder(RecyclerView(context)) {

		val screenshotsRecycler: RecyclerView
			get() = itemView as RecyclerView
	}

	private class SwitchViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
		val enabled = itemView.findViewById<SwitchMaterial>(R.id.update_state_switch)!!

		val statefulViews: Sequence<View>
			get() = sequenceOf(itemView, enabled)
	}

	private class SectionViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
		val title = itemView.findViewById<TextView>(R.id.title)!!
		val icon = itemView.findViewById<ShapeableImageView>(R.id.icon)!!
	}

	private class ExpandViewHolder(context: Context) :
		RecyclerView.ViewHolder(TextView(context)) {
		val text: TextView
			get() = itemView as TextView

		init {
			itemView as TextView
			itemView.typeface = TypefaceExtra.medium
			itemView.setTextSizeScaled(14)
			itemView.background =
				ResourcesCompat.getDrawable(
					itemView.resources,
					drawableRes.background_border,
					context.theme
				)
			itemView.backgroundTintList =
				itemView.context.getColorFromAttr(MaterialR.attr.colorSurface)
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
		RecyclerView.ViewHolder(TextView(context)) {
		val text: TextView
			get() = itemView as TextView

		init {
			itemView as TextView
			itemView.setTextIsSelectable(true)
			itemView.setTextSizeScaled(15)
			itemView.resources.sizeScaled(16).let { itemView.setPadding(it, it, it, it) }
			itemView.movementMethod = LinkMovementMethod()
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
		val text = itemView.findViewById<TextView>(R.id.text)!!
		val link = itemView.findViewById<TextView>(R.id.link)!!

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
		val text = itemView.findViewById<TextView>(R.id.text)!!

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

		val version = itemView.findViewById<TextView>(R.id.version)!!
		val status = itemView.findViewById<TextView>(R.id.installation_status)!!
		val source = itemView.findViewById<TextView>(R.id.source)!!
		val added = itemView.findViewById<TextView>(R.id.added)!!
		val size = itemView.findViewById<TextView>(R.id.size)!!
		val signature = itemView.findViewById<TextView>(R.id.signature)!!
		val compatibility = itemView.findViewById<TextView>(R.id.compatibility)!!

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
	}

	private class EmptyViewHolder(context: Context) :
		RecyclerView.ViewHolder(LinearLayout(context)) {
		val packageName: TextView

		init {
			itemView as LinearLayout
			itemView.orientation = LinearLayout.VERTICAL
			itemView.gravity = Gravity.CENTER
			itemView.resources.sizeScaled(20).let { itemView.setPadding(it, it, it, it) }
			val title = TextView(itemView.context)
			title.gravity = Gravity.CENTER
			title.typeface = TypefaceExtra.light
			title.setTextColor(context.getColorFromAttr(MaterialR.attr.colorPrimary))
			title.setTextSizeScaled(24)
			title.setText(stringRes.application_not_found)
			itemView.addView(
				title,
				LinearLayout.LayoutParams.MATCH_PARENT,
				LinearLayout.LayoutParams.WRAP_CONTENT
			)
			val packageName = TextView(itemView.context)
			packageName.gravity = Gravity.CENTER
			packageName.setTextColor(context.getColorFromAttr(MaterialR.attr.colorPrimary))
			packageName.setTextSizeScaled(18)
			itemView.addView(
				packageName,
				LinearLayout.LayoutParams.MATCH_PARENT,
				LinearLayout.LayoutParams.WRAP_CONTENT
			)
			itemView.layoutParams = RecyclerView.LayoutParams(
				RecyclerView.LayoutParams.MATCH_PARENT,
				RecyclerView.LayoutParams.MATCH_PARENT
			)
			this.packageName = packageName
		}
	}

	private val items = mutableListOf<Item>()
	private val expanded = mutableSetOf<ExpandType>()
	private var product: Product? = null
	private var installedItem: InstalledItem? = null

	fun setProducts(
		context: Context, packageName: String,
		products: List<Pair<Product, Repository>>,
		installedItem: InstalledItem?,
		userPreferences: UserPreferences
	) {
		val productRepository = Product.findSuggested(products, installedItem) { it.first }
		items.clear()

		if (productRepository != null) {
			items += Item.AppInfoItem(
				productRepository.second,
				productRepository.first
			)

			items += Item.DownloadStatusItem
			items += Item.InstallButtonItem


			if (productRepository.first.screenshots.isNotEmpty()) {
				val screenShotItem = mutableListOf<Item>()
				screenShotItem += Item.ScreenshotItem(
					productRepository.first.screenshots,
					packageName,
					productRepository.second
				)
				items += screenShotItem
			}

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
					"Ads" -> context.getString(stringRes.has_advertising)
					"ApplicationDebuggable" -> context.getString(stringRes.compiled_for_debugging)
					"DisabledAlgorithm" -> context.getString(stringRes.signed_using_unsafe_algorithm)
					"KnownVuln" -> context.getString(stringRes.has_security_vulnerabilities)
					"NoSourceSince" -> context.getString(stringRes.source_code_no_longer_available)
					"NonFreeAdd" -> context.getString(stringRes.promotes_non_free_software)
					"NonFreeAssets" -> context.getString(stringRes.contains_non_free_media)
					"NonFreeDep" -> context.getString(stringRes.has_non_free_dependencies)
					"NonFreeNet" -> context.getString(stringRes.promotes_non_free_network_services)
					"Tracking" -> context.getString(stringRes.tracks_or_reports_your_activity)
					"UpstreamNonFree" -> context.getString(stringRes.upstream_source_code_is_not_free)
					else -> context.getString(stringRes.unknown_FORMAT, it)
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
				source.let { link ->
					if (link.isNotEmpty()) {
						linkItems += Item.LinkItem.Typed(
							LinkType.SOURCE,
							"",
							link.toUri()
						)
					}
				}

				if (author.name.isNotEmpty() || author.web.isNotEmpty()) {
					linkItems += Item.LinkItem.Typed(
						LinkType.AUTHOR,
						author.name,
						author.web.nullIfEmpty()?.let(Uri::parse)
					)
				}
				author.email.nullIfEmpty()?.let {
					linkItems += Item.LinkItem.Typed(LinkType.EMAIL, "", Uri.parse("mailto:$it"))
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

		val incompatible = userPreferences.incompatibleVersions
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
		isFavourite = packageName in userPreferences.favouriteApps

		if (items.isEmpty()) items += Item.EmptyItem(packageName)

		this.product = productRepository?.first
		this.installedItem = installedItem
		notifyDataSetChanged()
	}

	private var action: Action? = null

	fun setAction(newAction: Action?) {
		if (action != newAction) {
			action = newAction
			val index = items.indexOf(Item.InstallButtonItem)
			if (index >= 0) {
				notifyItemChanged(index)
			}
		}
	}

	private var status: Status = Status.Idle

	fun setStatus(newStatus: Status) {
		val statusIndex = items.indexOf(Item.DownloadStatusItem)
		if (status != newStatus && statusIndex >= 0) {
			status = newStatus
			when (newStatus) {
				is Status.Downloading -> notifyItemChanged(statusIndex, Payload.STATUS)
				Status.Connecting -> notifyItemChanged(statusIndex, Payload.STATUS)
				Status.Installing -> notifyItemChanged(statusIndex, Payload.STATUS)
				Status.PendingInstall -> notifyItemInserted(statusIndex)
				Status.Pending -> notifyItemInserted(statusIndex)
				Status.Idle -> notifyItemRemoved(statusIndex)
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
			ViewType.APP_INFO -> AppInfoViewHolder(parent.inflate(R.layout.app_detail_header)).apply {
				favouriteButton.setOnClickListener { callbacks.onFavouriteClicked() }
			}
			ViewType.DOWNLOAD_STATUS -> DownloadStatusViewHolder(parent.inflate(R.layout.download_status))
			ViewType.INSTALL_BUTTON -> InstallButtonViewHolder(parent.inflate(R.layout.install_button)).apply {
				button.setOnClickListener { action?.let(callbacks::onActionClick) }
			}
			ViewType.SCREENSHOT -> ScreenShotViewHolder(parent.context)
			ViewType.SWITCH -> SwitchViewHolder(parent.inflate(R.layout.switch_item)).apply {
				itemView.setOnClickListener {
					val switchItem = items[absoluteAdapterPosition] as Item.SwitchItem
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
					callbacks.onPreferenceChanged(productPreference)
				}
			}
			ViewType.SECTION -> SectionViewHolder(parent.inflate(R.layout.section_item)).apply {
				itemView.setOnClickListener {
					val position = absoluteAdapterPosition
					val sectionItem = items[position] as Item.SectionItem
					if (sectionItem.items.isNotEmpty()) {
						expanded += sectionItem.expandType
						items[position] = Item.SectionItem(
							sectionItem.sectionType, sectionItem.expandType, emptyList(),
							sectionItem.items.size + sectionItem.collapseCount
						)
						notifyItemChanged(position, Payload.REFRESH)
						items.addAll(position + 1, sectionItem.items)
						notifyItemRangeInserted(position + 1, sectionItem.items.size)
					} else if (sectionItem.collapseCount > 0) {
						expanded -= sectionItem.expandType
						items[position] = Item.SectionItem(
							sectionItem.sectionType, sectionItem.expandType,
							items.subList(position + 1, position + 1 + sectionItem.collapseCount)
								.toList(), 0
						)
						notifyItemChanged(position, Payload.REFRESH)
						repeat(sectionItem.collapseCount) { items.removeAt(position + 1) }
						notifyItemRangeRemoved(position + 1, sectionItem.collapseCount)
					}
				}
			}
			ViewType.EXPAND -> ExpandViewHolder(parent.context).apply {
				itemView.setOnClickListener {
					val position = absoluteAdapterPosition
					val expandItem = items[position] as Item.ExpandItem
					if (expandItem.expandType !in expanded) {
						expanded += expandItem.expandType
						if (expandItem.replace) {
							items[position - 1] = expandItem.items[0]
							notifyItemRangeChanged(position - 1, 2)
						} else {
							items.addAll(position, expandItem.items)
							if (position > 0) {
								notifyItemRangeInserted(position, expandItem.items.size)
								notifyItemChanged(position + expandItem.items.size)
							}
						}
					} else {
						expanded -= expandItem.expandType
						if (expandItem.replace) {
							items[position - 1] = expandItem.items[1]
							notifyItemRangeChanged(position - 1, 2)
						} else {
							items.removeAll(expandItem.items)
							if (position > 0) {
								notifyItemRangeRemoved(
									position - expandItem.items.size,
									expandItem.items.size
								)
								notifyItemChanged(position - expandItem.items.size)
							}
						}
					}
				}
			}
			ViewType.TEXT -> TextViewHolder(parent.context)
			ViewType.LINK -> LinkViewHolder(parent.inflate(R.layout.link_item)).apply {
				itemView.setOnClickListener {
					val linkItem = items[absoluteAdapterPosition] as Item.LinkItem
					if (linkItem.uri?.let { callbacks.onUriClick(it, false) } != true) {
						linkItem.displayLink?.let { copyLinkToClipboard(itemView, it) }
					}
				}
				itemView.setOnLongClickListener {
					val linkItem = items[absoluteAdapterPosition] as Item.LinkItem
					linkItem.displayLink?.let { copyLinkToClipboard(itemView, it) }
					true
				}
			}
			ViewType.PERMISSIONS -> PermissionsViewHolder(parent.inflate(R.layout.permissions_item)).apply {
				itemView.setOnClickListener {
					val permissionsItem = items[absoluteAdapterPosition] as Item.PermissionsItem
					callbacks.onPermissionsClick(
						permissionsItem.group?.name,
						permissionsItem.permissions.map { it.name })
				}
			}
			ViewType.RELEASE -> ReleaseViewHolder(parent.inflate(R.layout.release_item)).apply {
				itemView.setOnClickListener {
					val releaseItem = items[absoluteAdapterPosition] as Item.ReleaseItem
					callbacks.onReleaseClick(releaseItem.release)
				}
				itemView.setOnLongClickListener {
					val releaseItem = items[absoluteAdapterPosition] as Item.ReleaseItem
					copyLinkToClipboard(
						itemView,
						releaseItem.release.getDownloadUrl(releaseItem.repository)
					)
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
				var showAuthor = item.product.author.name.isNotEmpty()
				if (updateAll) {
					holder.icon.load(
						item.product.packageName.icon(
							view = holder.icon,
							icon = item.product.icon,
							metadataIcon = item.product.metadataIcon,
							repository = item.repository
						)
					)
					val authorText =
						if (showAuthor) buildSpannedString {
							append("by ")
							bold { append(item.product.author.name) }
						} else buildSpannedString { bold { append(item.product.packageName) } }
					holder.authorName.text = authorText
					holder.packageName.text = authorText
					if (item.product.author.name.isNotEmpty()) {
						holder.icon.setOnClickListener {
							showAuthor = !showAuthor
							val newText = if (showAuthor) buildSpannedString {
								append("by ")
								bold { append(item.product.author.name) }
							} else buildSpannedString { bold { append(item.product.packageName) } }
							holder.textSwitcher.setText(newText)
						}
					}
					holder.name.text = item.product.name
				}

				holder.version.apply {
					text = installedItem?.version ?: product?.version
					if (product?.canUpdate(installedItem) == true) {
						if (background == null) {
							background =
								ResourcesCompat.getDrawable(
									holder.itemView.resources,
									CommonR.drawable.background_border,
									context.theme
								)
							resources.sizeScaled(4).let { setPadding(it * 2, it, it * 2, it) }
							backgroundTintList =
								context.getColorFromAttr(MaterialR.attr.colorSecondaryContainer)
							setTextColor(context.getColorFromAttr(MaterialR.attr.colorSecondary))
						}
					} else {
						if (background != null) {
							setPadding(0, 0, 0, 0)
							setTextColor(context.getColorFromAttr(android.R.attr.colorControlNormal))
							background = null
						}
					}
				}
				holder.size.text = product?.displayRelease?.size?.formatSize()

				holder.dev.setOnClickListener {
					product?.source?.let { link ->
						if (link.isNotEmpty()) {
							context.startActivity(Intent(Intent.ACTION_VIEW, link.toUri()))
						}
					}
				}
				holder.dev.setOnLongClickListener {
					product?.source?.let { link ->
						if (link.isNotEmpty()) copyLinkToClipboard(holder.dev, link)
					}
					true
				}
				holder.favouriteButton.isChecked = isFavourite
			}
			ViewType.DOWNLOAD_STATUS -> {
				holder as DownloadStatusViewHolder
				item as Item.DownloadStatusItem
				val status = status
				holder.itemView.isVisible = status != Status.Idle
				holder.statusText.isVisible = status != Status.Idle
				holder.progress.isVisible = status != Status.Idle
				if (status != Status.Idle) {
					when (status) {
						is Status.Pending -> {
							holder.statusText.setText(stringRes.waiting_to_start_download)
							holder.progress.isIndeterminate = true
						}
						is Status.Connecting -> {
							holder.statusText.setText(stringRes.connecting)
							holder.progress.isIndeterminate = true
						}
						is Status.Downloading -> {
							holder.statusText.text = context.getString(
								stringRes.downloading_FORMAT, if (status.total == null)
									status.read.formatSize() else "${status.read.formatSize()} / ${status.total.formatSize()}"
							)
							holder.progress.isIndeterminate = status.total == null
							if (status.total != null) {
								holder.progress.progress =
									(holder.progress.max.toFloat() * status.read / status.total).roundToInt()
							} else Unit
						}
						Status.Installing -> {
							holder.statusText.setText(stringRes.installing)
							holder.progress.isIndeterminate = true
						}
						Status.PendingInstall -> {
							holder.statusText.setText(stringRes.waiting_to_start_installation)
							holder.progress.isIndeterminate = true
						}
						Status.Idle -> {}
					}::class
				}
				Unit
			}
			ViewType.INSTALL_BUTTON -> {
				holder as InstallButtonViewHolder
				item as Item.InstallButtonItem
				val action = action
				holder.button.apply {
					isEnabled = action != null
					if (action != null) {
						icon = context.getDrawable(action.iconResId)
						setText(action.titleResId)
						setTextColor(
							if (action == Action.CANCEL) holder.actionTintOnCancel
							else holder.actionTintOnNormal
						)
						backgroundTintList = if (action == Action.CANCEL)
							holder.actionTintCancel else holder.actionTintNormal
						iconTint = if (action == Action.CANCEL) holder.actionTintOnCancel
						else holder.actionTintOnNormal
					} else {
						icon = context.getDrawable(drawableRes.ic_cancel)
						setText(stringRes.cancel)
						setTextColor(holder.actionTintOnDisabled)
						backgroundTintList = holder.actionTintDisabled
						iconTint = holder.actionTintOnDisabled
					}
				}
				Unit
			}
			ViewType.SCREENSHOT -> {
				holder as ScreenShotViewHolder
				item as Item.ScreenshotItem
				holder.screenshotsRecycler.run {
					isNestedScrollingEnabled = false
					clipToPadding = false
					context.resources.sizeScaled(8).let {
						setPadding(it, it, it, it)
					}
					layoutManager =
						LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
					adapter =
						ScreenshotsAdapter { callbacks.onScreenshotClick(it) }.apply {
							setScreenshots(item.repository, item.packageName, item.screenshots)
						}
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
				holder.icon.isVisible = expandable
				holder.icon.scaleY = if (item.collapseCount > 0) -1f else 1f
				holder.icon.imageTintList = color
			}
			ViewType.EXPAND -> {
				holder as ExpandViewHolder
				item as Item.ExpandItem
				holder.text.text = if (item.expandType !in expanded) {
					when (item.expandType) {
						ExpandType.VERSIONS -> context.getString(stringRes.show_older_versions)
						else -> context.getString(stringRes.show_more)
					}
				} else context.getString(stringRes.show_less)
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
				holder.link.isVisible = item.uri != null
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
						item.group.loadUnbadgedIcon(packageManager)
					} else {
						null
					} ?: context.getDrawableCompat(drawableRes.ic_perm_device_information)
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

				if (suggested) {
					holder.itemView.apply {
						background = ResourcesCompat.getDrawable(
							holder.itemView.resources,
							drawableRes.background_border,
							holder.itemView.context.theme
						)
						backgroundTintList =
							holder.itemView.context.getColorFromAttr(MaterialR.attr.colorSurface)
					}
				} else {
					holder.itemView.background = null
				}
				holder.version.text =
					context.getString(stringRes.version_FORMAT, item.release.version)

				holder.status.apply {
					isVisible = installed || suggested
					setText(
						when {
							installed -> stringRes.installed
							suggested -> stringRes.suggested
							else -> stringRes.unknown
						}
					)
					background =
						ResourcesCompat.getDrawable(
							holder.itemView.resources,
							drawableRes.background_border,
							context.theme
						)
					setPadding(15, 15, 15, 15)
					backgroundTintList =
						context.getColorFromAttr(MaterialR.attr.colorSecondaryContainer)
					setTextColor(context.getColorFromAttr(MaterialR.attr.colorOnSecondaryContainer))
				}
				holder.source.text =
					context.getString(stringRes.provided_by_FORMAT, item.repository.name)
				val dateFormat = try {
					LocalDateTime.ofInstant(
						Instant.ofEpochMilli(item.release.added),
						TimeZone.getDefault().toZoneId()
					).format(DateTimeFormatter.ofLocalizedDate(FormatStyle.SHORT))
				} catch (e: Exception) {
					Log.e(
						"AppDetailAdapter",
						"onBindViewHolder: Date cannot be formatted locally",
						e
					)
					e.printStackTrace()
					holder.dateFormat.format(item.release.added)
				}
				holder.added.text = dateFormat
				holder.size.text = item.release.size.formatSize()
				holder.signature.isVisible =
					item.showSignature && item.release.signature.isNotEmpty()
				if (item.showSignature && item.release.signature.isNotEmpty()) {
					val bytes =
						item.release.signature.uppercase(Locale.US).windowed(2, 2, false).take(8)
					val signature = bytes.joinToString(separator = " ")
					val builder = SpannableStringBuilder(
						context.getString(
							stringRes.signature_FORMAT,
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
				holder.compatibility.isVisible = incompatibility != null || singlePlatform != null
				if (incompatibility != null) {
					holder.compatibility.setTextColor(context.getColorFromAttr(MaterialR.attr.colorError))
					holder.compatibility.text = when (incompatibility) {
						is Release.Incompatibility.MinSdk,
						is Release.Incompatibility.MaxSdk,
						-> context.getString(
							stringRes.incompatible_with_FORMAT,
							Android.name
						)
						is Release.Incompatibility.Platform -> context.getString(
							stringRes.incompatible_with_FORMAT,
							Android.primaryPlatform ?: context.getString(stringRes.unknown)
						)
						is Release.Incompatibility.Feature -> context.getString(
							stringRes.requires_FORMAT,
							incompatibility.feature
						)
					}
				} else if (singlePlatform != null) {
					holder.compatibility.setTextColor(context.getColorFromAttr(android.R.attr.textColorSecondary))
					holder.compatibility.text =
						context.getString(stringRes.only_compatible_with_FORMAT, singlePlatform)
				}
				val enabled = status == Status.Idle
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
		Snackbar.make(view, stringRes.link_copied_to_clipboard, Snackbar.LENGTH_SHORT).show()
	}

	private class LinkSpan(private val url: String, productAdapter: AppDetailAdapter) :
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
