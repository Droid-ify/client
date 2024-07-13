package com.looker.droidify.ui.appDetail

import android.content.Context
import android.graphics.drawable.Drawable
import android.view.Gravity
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import coil.dispose
import coil.load
import coil.size.Dimension
import coil.size.Scale
import com.google.android.material.imageview.ShapeableImageView
import com.looker.core.common.extension.aspectRatio
import com.looker.core.common.extension.authentication
import com.looker.core.common.extension.camera
import com.looker.core.common.extension.dp
import com.looker.core.common.extension.dpToPx
import com.looker.core.common.extension.getColorFromAttr
import com.looker.core.common.extension.selectableBackground
import com.looker.droidify.model.Product
import com.looker.droidify.model.Repository
import com.looker.droidify.graphics.PaddingDrawable
import com.looker.droidify.utility.extension.ImageUtils.url
import com.looker.droidify.widget.StableRecyclerAdapter
import com.google.android.material.R as MaterialR
import com.looker.core.common.R.dimen as dimenRes

class ScreenshotsAdapter(private val onClick: (Product.Screenshot, ImageView) -> Unit) :
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
        val placeholderColor = context.getColorFromAttr(MaterialR.attr.colorPrimaryContainer)
        val radius = context.resources.getDimension(dimenRes.shape_small_corner)

        val imageShapeModel = image.shapeAppearanceModel.toBuilder()
            .setAllCornerSizes(radius)
            .build()
        val cameraIcon = context.camera
            .apply { setTintList(placeholderColor) }
        val placeholder: Drawable = PaddingDrawable(cameraIcon, 3f, context.aspectRatio)

        init {
            with(image) {
                layout(0, 0, 0, 0)
                adjustViewBounds = true
                shapeAppearanceModel = imageShapeModel
                background = context.selectableBackground
                isFocusable = true
            }
            with(itemView as FrameLayout) {
                addView(image)
                layoutParams = RecyclerView.LayoutParams(
                    RecyclerView.LayoutParams.WRAP_CONTENT,
                    150.dp,
                ).apply {
                    marginStart = radius.toInt()
                    marginEnd = radius.toInt()
                }
                foregroundGravity = Gravity.CENTER
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
            image.setOnClickListener {
                onClick(
                    items[absoluteAdapterPosition].screenshot,
                    it as ImageView
                )
            }
        }
    }

    override fun getItemDescriptor(position: Int): String = items[position].descriptor
    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        holder as ViewHolder
        val item = items[position]
        with(holder.image) {
            load(item.screenshot.url(item.repository, item.packageName)) {
                size(Dimension.Undefined, Dimension(150.dp.dpToPx.toInt()))
                scale(Scale.FIT)
                placeholder(holder.placeholder)
                error(holder.placeholder)
                authentication(item.repository.authentication)
            }
        }
    }

    override fun onViewRecycled(holder: RecyclerView.ViewHolder) {
        super.onViewRecycled(holder)
        holder as ViewHolder
        holder.image.dispose()
    }

    private sealed class Item {
        abstract val descriptor: String

        class ScreenshotItem(
            val repository: Repository,
            val packageName: String,
            val screenshot: Product.Screenshot
        ) : Item() {
            override val descriptor: String
                get() = "screenshot.${repository.id}.${screenshot.identifier}"
        }
    }
}
