package com.looker.droidify.ui.appDetail

import android.content.Context
import android.graphics.drawable.Drawable
import android.view.Gravity
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.recyclerview.widget.RecyclerView
import coil3.asImage
import coil3.dispose
import coil3.load
import coil3.request.placeholder
import coil3.size.Scale
import com.google.android.material.imageview.ShapeableImageView
import com.looker.droidify.databinding.VideoButtonBinding
import com.looker.droidify.graphics.PaddingDrawable
import com.looker.droidify.model.Product
import com.looker.droidify.model.Repository
import com.looker.droidify.utility.common.extension.aspectRatio
import com.looker.droidify.utility.common.extension.camera
import com.looker.droidify.utility.common.extension.dp
import com.looker.droidify.utility.common.extension.getColorFromAttr
import com.looker.droidify.utility.common.extension.layoutInflater
import com.looker.droidify.utility.common.extension.openLink
import com.looker.droidify.utility.common.extension.selectableBackground
import com.looker.droidify.widget.StableRecyclerAdapter
import com.google.android.material.R as MaterialR
import com.looker.droidify.R.dimen as dimenRes

class ScreenshotsAdapter(private val onClick: (position: Int) -> Unit) :
    StableRecyclerAdapter<ScreenshotsAdapter.ViewType, RecyclerView.ViewHolder>() {
    enum class ViewType { SCREENSHOT, VIDEO }

    private val items = mutableListOf<Item>()

    private inner class VideoViewHolder(
        binding: VideoButtonBinding,
    ) : RecyclerView.ViewHolder(binding.root) {
        val button = binding.videoButton

        init {
            with(button) {
                layoutParams = RecyclerView.LayoutParams(
                    RecyclerView.LayoutParams.WRAP_CONTENT,
                    150.dp,
                )
                setOnClickListener {
                    val item = items[absoluteAdapterPosition] as Item.VideoItem
                    it.context?.openLink(item.videoUrl)
                }
            }
        }
    }


    private inner class ScreenshotViewHolder(
        context: Context,
    ) : RecyclerView.ViewHolder(FrameLayout(context)) {
        val image = ShapeableImageView(context)
        val placeholderColor = context.getColorFromAttr(MaterialR.attr.colorPrimaryContainer)
        val radius = context.resources.getDimension(dimenRes.shape_small_corner)

        val imageShapeModel = image.shapeAppearanceModel.toBuilder()
            .setAllCornerSizes(radius)
            .build()
        val cameraIcon = context.camera.apply { setTintList(placeholderColor) }
        val placeholder: Drawable = PaddingDrawable(cameraIcon, 3f, context.aspectRatio)

        init {
            with(image) {
                layout(0, 0, 0, 0)
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
                setOnClickListener {
                    val position = if (items.any { it.viewType == ViewType.VIDEO }) {
                        absoluteAdapterPosition - 1
                    } else {
                        absoluteAdapterPosition
                    }
                    onClick(position)
                }
            }
        }
    }

    fun setScreenshots(
        repository: Repository,
        packageName: String,
        screenshots: List<Product.Screenshot>,
    ) {
        items.clear()
        items += screenshots.map {
            if (it.type == Product.Screenshot.Type.VIDEO) Item.VideoItem(it.path)
            else Item.ScreenshotItem(repository, packageName, it)
        }
        notifyItemRangeInserted(0, screenshots.size)
    }

    override val viewTypeClass: Class<ViewType> get() = ViewType::class.java
    override fun getItemCount(): Int = items.size
    override fun getItemEnumViewType(position: Int) = items[position].viewType
    override fun getItemDescriptor(position: Int): String = items[position].descriptor

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: ViewType,
    ): RecyclerView.ViewHolder {
        return when (viewType) {
            ViewType.VIDEO -> VideoViewHolder(VideoButtonBinding.inflate(parent.context.layoutInflater))
            ViewType.SCREENSHOT -> ScreenshotViewHolder(parent.context)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (getItemEnumViewType(position)) {
            ViewType.SCREENSHOT -> {
                holder as ScreenshotViewHolder
                val item = items[position] as Item.ScreenshotItem
                with(holder.image) {
                    load(item.screenshot.url(context, item.repository, item.packageName)) {
                        scale(Scale.FILL)
                        placeholder(holder.placeholder)
                        error(holder.placeholder.asImage())
                    }
                }
            }

            ViewType.VIDEO -> {}
        }
    }

    override fun onViewRecycled(holder: RecyclerView.ViewHolder) {
        super.onViewRecycled(holder)
        if (holder is ScreenshotViewHolder) holder.image.dispose()
    }

    private sealed interface Item {

        val descriptor: String
        val viewType: ViewType

        class ScreenshotItem(
            val repository: Repository,
            val packageName: String,
            val screenshot: Product.Screenshot,
        ) : Item {
            override val viewType: ViewType get() = ViewType.SCREENSHOT
            override val descriptor: String
                get() = "screenshot.${repository.id}.${screenshot.identifier}"
        }

        class VideoItem(val videoUrl: String) : Item {
            override val viewType: ViewType get() = ViewType.VIDEO
            override val descriptor: String get() = "video"
        }
    }
}
