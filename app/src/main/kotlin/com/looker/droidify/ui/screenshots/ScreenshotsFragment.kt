package com.looker.droidify.ui.screenshots

import android.app.Dialog
import android.content.Context
import android.graphics.PixelFormat
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.Bundle
import android.view.ViewGroup
import android.view.WindowManager
import androidx.core.graphics.ColorUtils
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.flowWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.MarginPageTransformer
import androidx.viewpager2.widget.ViewPager2
import coil.dispose
import coil.load
import com.jsibbold.zoomage.AutoResetMode.UNDER
import com.jsibbold.zoomage.ZoomageView
import com.looker.core.common.extension.dp
import com.looker.core.common.extension.getColorFromAttr
import com.looker.core.common.extension.getMutatedIcon
import com.looker.core.common.sdkAbove
import com.looker.core.model.Product
import com.looker.core.model.Repository
import com.looker.droidify.database.Database
import com.looker.droidify.graphics.PaddingDrawable
import com.looker.droidify.utility.extension.url
import com.looker.droidify.widget.StableRecyclerAdapter
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import com.google.android.material.R as MaterialR
import com.looker.core.common.R.drawable as drawableRes
import com.looker.core.common.R.style as styleRes

class ScreenshotsFragment() : DialogFragment() {
	companion object {
		private const val EXTRA_PACKAGE_NAME = "packageName"
		private const val EXTRA_REPOSITORY_ID = "repositoryId"
		private const val EXTRA_IDENTIFIER = "identifier"

		private const val STATE_IDENTIFIER = "identifier"
	}

	constructor(packageName: String, repositoryId: Long, identifier: String) : this() {
		arguments = Bundle().apply {
			putString(EXTRA_PACKAGE_NAME, packageName)
			putLong(EXTRA_REPOSITORY_ID, repositoryId)
			putString(EXTRA_IDENTIFIER, identifier)
		}
	}

	fun show(fragmentManager: FragmentManager) {
		show(fragmentManager, this::class.java.name)
	}

	private var viewPager: ViewPager2? = null

	override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
		val packageName = requireArguments().getString(EXTRA_PACKAGE_NAME)!!
		val repositoryId = requireArguments().getLong(EXTRA_REPOSITORY_ID)
		val dialog = Dialog(requireContext(), styleRes.Theme_Main_Dark)

		val window = dialog.window
		val decorView = window?.decorView

		val background = dialog.context.getColorFromAttr(MaterialR.attr.colorSurface).defaultColor
		decorView?.setBackgroundColor(
			ColorUtils.blendARGB(
				0x00FFFFFF and background,
				background,
				0.9f
			)
		)
		decorView?.setPadding(0, 0, 0, 0)
		if (window != null) {
			window.attributes = window.attributes.apply {
				title = ScreenshotsFragment::class.java.name
				format = PixelFormat.TRANSLUCENT
				windowAnimations = run {
					val typedArray = dialog.context.obtainStyledAttributes(
						null, intArrayOf(android.R.attr.windowAnimationStyle),
						android.R.attr.dialogTheme, 0
					)
					try {
						typedArray.getResourceId(0, 0)
					} finally {
						typedArray.recycle()
					}
				}
				sdkAbove(Build.VERSION_CODES.P) {
					layoutInDisplayCutoutMode =
						WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
				}
			}
		}

		if (window != null && decorView != null) {
			WindowInsetsControllerCompat(
				window,
				decorView
			).hide(WindowInsetsCompat.Type.systemBars())
		}

		val viewPager = ViewPager2(dialog.context)
		with(ViewPager2(dialog.context)) {
			adapter = Adapter(packageName)
			setPageTransformer(MarginPageTransformer(8.dp))
			viewTreeObserver.addOnGlobalLayoutListener {
				(adapter as Adapter).size = Pair(width, height)
			}
			dialog.addContentView(
				this, ViewGroup.LayoutParams(
					ViewGroup.LayoutParams.MATCH_PARENT,
					ViewGroup.LayoutParams.MATCH_PARENT
				)
			)
			this@ScreenshotsFragment.viewPager = this

		}

		var restored = false
		lifecycleScope.launch {
			Database.ProductAdapter
				.getStream(packageName)
				.map { products ->
					val primaryProduct = products.find { it.repositoryId == repositoryId }
					primaryProduct to Database.RepositoryAdapter.get(repositoryId)
				}
				.flowWithLifecycle(lifecycle, Lifecycle.State.RESUMED)
				.collectLatest { (product, repository) ->
					val screenshots = product?.screenshots.orEmpty()
					(viewPager.adapter as Adapter).update(viewPager, repository, screenshots)
					if (!restored) {
						restored = true
						val identifier = savedInstanceState?.getString(STATE_IDENTIFIER)
							?: requireArguments().getString(STATE_IDENTIFIER)
						if (identifier != null) {
							val index = screenshots.indexOfFirst { it.identifier == identifier }
							if (index >= 0) {
								viewPager.setCurrentItem(index, false)
							}
						}
					}
				}
		}
		return dialog
	}

	override fun onDestroyView() {
		super.onDestroyView()

		viewPager = null
	}

	override fun onSaveInstanceState(outState: Bundle) {
		super.onSaveInstanceState(outState)

		val viewPager = viewPager
		if (viewPager != null) {
			val identifier = (viewPager.adapter as Adapter).getCurrentIdentifier(viewPager)
			identifier?.let { outState.putString(STATE_IDENTIFIER, it) }
		}
	}

	private class Adapter(private val packageName: String) :
		StableRecyclerAdapter<Adapter.ViewType, RecyclerView.ViewHolder>() {
		enum class ViewType { SCREENSHOT }

		private class ViewHolder(context: Context) :
			RecyclerView.ViewHolder(ZoomageView(context)) {
			val image: ZoomageView
				get() = itemView as ZoomageView

			val placeholder: Drawable

			init {
				itemView.layoutParams = RecyclerView.LayoutParams(
					RecyclerView.LayoutParams.MATCH_PARENT,
					RecyclerView.LayoutParams.MATCH_PARENT
				)
				image.apply {
					isZoomable = true
					isTranslatable = true
					autoCenter = true
					restrictBounds = false
					setScaleRange(1f, 8f)
					animateOnReset = true
					autoResetMode = UNDER
				}
				val placeholder = itemView.context.getMutatedIcon(drawableRes.ic_photo_camera)
				placeholder.setTint(itemView.context.getColorFromAttr(MaterialR.attr.colorSurface).defaultColor)
				this.placeholder = PaddingDrawable(placeholder, 4f)
			}
		}

		private var repository: Repository? = null
		private var screenshots = emptyList<Product.Screenshot>()

		fun update(
			viewPager: ViewPager2,
			repository: Repository?,
			screenshots: List<Product.Screenshot>,
		) {
			this.repository = repository
			this.screenshots = screenshots
			notifyItemChanged(viewPager.currentItem)
		}

		var size = Pair(0, 0)
			set(value) {
				if (field != value) {
					field = value
				}
			}

		fun getCurrentIdentifier(viewPager: ViewPager2): String? {
			val position = viewPager.currentItem
			return screenshots.getOrNull(position)?.identifier
		}

		override val viewTypeClass: Class<ViewType>
			get() = ViewType::class.java

		override fun getItemCount(): Int = screenshots.size
		override fun getItemDescriptor(position: Int): String = screenshots[position].identifier
		override fun getItemEnumViewType(position: Int): ViewType = ViewType.SCREENSHOT

		override fun onCreateViewHolder(
			parent: ViewGroup,
			viewType: ViewType,
		): RecyclerView.ViewHolder = ViewHolder(parent.context)

		override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
			holder as ViewHolder
			val screenshot = screenshots[position]
			repository?.let {
				holder.image.load(screenshot.url(it, packageName)) {
					placeholder(holder.placeholder)
					error(holder.placeholder)
				}
			}
		}

		override fun onViewDetachedFromWindow(holder: RecyclerView.ViewHolder) {
			super.onViewDetachedFromWindow(holder)
			holder as ViewHolder
			holder.image.dispose()
		}
	}
}
