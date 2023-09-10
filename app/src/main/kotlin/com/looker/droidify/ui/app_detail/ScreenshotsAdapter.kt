package com.looker.droidify.ui.app_detail

import android.content.Context
import android.graphics.drawable.Drawable
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.recyclerview.widget.RecyclerView
import coil.load
import coil.size.Scale
import com.google.android.material.imageview.ShapeableImageView
import com.looker.core.common.extension.*
import com.looker.core.model.Product
import com.looker.core.model.Repository
import com.looker.droidify.graphics.PaddingDrawable
import com.looker.droidify.utility.extension.url
import com.looker.droidify.widget.StableRecyclerAdapter
import com.google.android.material.R as MaterialR
import com.looker.core.common.R as CommonR
import com.looker.core.common.R.dimen as dimenRes

class ScreenshotsAdapter(private val onClick: (Product.Screenshot) -> Unit) :
	StableRecyclerAdapter<ScreenshotsAdapter.ViewType, RecyclerView.ViewHolder>() {
	enum class ViewType { SCREENSHOT }

	private val items = mutableListOf<Item.ScreenshotItem>()

	private class ViewHolder(context: Context) :
		RecyclerView.ViewHolder(FrameLayout(context)) {
		val image: ShapeableImageView = object : ShapeableImageView(context) {
			override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
				super.onMeasure(widthMeasureSpec, heightMeasureSpec)
				setMeasuredDimension(measuredWidth, measuredHeight)
			}
		}
		val placeholder: Drawable
		val surfaceColor = context.getColorFromAttr(MaterialR.attr.colorPrimaryContainer)

		val radius = context.resources.getDimension(dimenRes.shape_small_corner)
		val imageShapeModel = image.shapeAppearanceModel.toBuilder()
			.setAllCornerSizes(radius)
			.build()
		val imagePlaceholder = context.getMutatedIcon(CommonR.drawable.ic_screenshot_placeholder)

		init {
			with(image) {
				shapeAppearanceModel = imageShapeModel
				background = context.selectableBackground

				imagePlaceholder.setTintList(surfaceColor)
				placeholder = PaddingDrawable(imagePlaceholder, 3f, context.aspectRatio)
			}
			with(itemView as FrameLayout) {
				addView(image)
				layoutParams = RecyclerView.LayoutParams(
					RecyclerView.LayoutParams.WRAP_CONTENT,
					RecyclerView.LayoutParams.MATCH_PARENT
				).apply {
					marginStart = radius.toInt()
					marginEnd = radius.toInt()
				}
			}
		}
	}

	fun setScreenshots(
		repository: Repository,
		packageName: String,
		screenshots: List<Product.Screenshot>
	) {
		items.clear()
		items += screenshots.map { Item.ScreenshotItem(repository, packageName, it) }
		notifyItemRangeInserted(0, screenshots.size)
	}

	override val viewTypeClass: Class<ViewType>
		get() = ViewType::class.java

	override fun getItemEnumViewType(position: Int): ViewType {
		return ViewType.SCREENSHOT
	}

	override fun onCreateViewHolder(
		parent: ViewGroup,
		viewType: ViewType
	): RecyclerView.ViewHolder {
		return ViewHolder(parent.context).apply {
			image.setOnClickListener { onClick(items[absoluteAdapterPosition].screenshot) }
		}
	}

	override fun getItemDescriptor(position: Int): String = items[position].descriptor
	override fun getItemCount(): Int = items.size

	override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
		holder as ViewHolder
		val item = items[position]
		holder.image.load(
			item.screenshot.url(item.repository, item.packageName)
		) {
			scale(Scale.FILL)
			placeholder(holder.placeholder)
			error(holder.placeholder)
		}
	}

	private sealed class Item {
		abstract val descriptor: String

		class ScreenshotItem(
			val repository: Repository,
			val packageName: String,
			val screenshot: Product.Screenshot,
		) : Item() {
			override val descriptor: String
				get() = "screenshot.${repository.id}.${screenshot.identifier}"
		}
	}
}
