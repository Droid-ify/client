package com.looker.droidify.ui.appDetail

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.drawable.Drawable
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.FrameLayout
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import coil3.asImage
import coil3.dispose
import coil3.load
import coil3.request.placeholder
import coil3.size.Scale
import com.google.android.material.imageview.ShapeableImageView
import com.google.android.material.shape.ShapeAppearanceModel
import com.looker.droidify.databinding.VideoButtonBinding
import com.looker.droidify.graphics.PaddingDrawable
import com.looker.droidify.ui.appDetail.ScreenshotsAdapter.BaseViewHolder
import com.looker.droidify.ui.appDetail.ScreenshotsAdapter.ViewType
import com.looker.droidify.utility.common.extension.aspectRatio
import com.looker.droidify.utility.common.extension.authentication
import com.looker.droidify.utility.common.extension.camera
import com.looker.droidify.utility.common.extension.dp
import com.looker.droidify.utility.common.extension.getColorFromAttr
import com.looker.droidify.utility.common.extension.layoutInflater
import com.looker.droidify.utility.common.extension.openLink
import com.looker.droidify.utility.common.extension.selectableBackground
import com.looker.droidify.widget.EnumRecyclerAdapter
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import com.google.android.material.R as MaterialR
import com.looker.droidify.R.dimen as dimenRes

class ScreenshotsAdapter(
    private val defaultDispatcher: CoroutineDispatcher,
    private val onScreenshotClick: ScreenshotClickListener
) : EnumRecyclerAdapter<ViewType, BaseViewHolder<ScreenshotsAdapterItem>>() {
    enum class ViewType { SCREENSHOT, VIDEO }

    private var items: List<ScreenshotsAdapterItem> = emptyList()

    abstract class BaseViewHolder<T: ScreenshotsAdapterItem>(itemView: View): RecyclerView.ViewHolder(itemView) {

        protected lateinit var currentItem: T
            private set

        fun onBind(item: T) {
            currentItem = item
            onBindImpl(item)
        }

        protected abstract fun onBindImpl(item: T)

        open fun onRecycled() {

        }
    }

    private class VideoViewHolder(
        binding: VideoButtonBinding,
    ) : BaseViewHolder<VideoItem>(binding.root), View.OnClickListener {
        private val button: Button = binding.videoButton

        init {
            with(button) {
                layoutParams = RecyclerView.LayoutParams(
                    RecyclerView.LayoutParams.WRAP_CONTENT,
                    150.dp,
                )

                setOnClickListener(this@VideoViewHolder)
            }
        }

        override fun onClick(v: View?) {
            if (v == itemView) {
                v.context.openLink(currentItem.videoUrl)
            }
        }

        override fun onBindImpl(item: VideoItem) {
            // do nothing
        }
    }

    fun interface ScreenshotClickListener {
        fun onScreenshotClick(item: ScreenshotItem)
    }

    private class ScreenshotViewHolder(
        context: Context,
        private val clickListener: ScreenshotClickListener
    ) : BaseViewHolder<ScreenshotItem>(FrameLayout(context)), View.OnClickListener {
        private val image = ShapeableImageView(context)
        private val placeholderColor: ColorStateList = context.getColorFromAttr(MaterialR.attr.colorPrimaryContainer)
        private val radius = context.resources.getDimension(dimenRes.shape_small_corner)

        private val imageShapeModel: ShapeAppearanceModel = image.shapeAppearanceModel.toBuilder()
            .setAllCornerSizes(radius)
            .build()
        private val cameraIcon: Drawable = context.camera.apply { setTintList(placeholderColor) }
        private val placeholder: Drawable = PaddingDrawable(cameraIcon, 3f, context.aspectRatio)

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
                setOnClickListener(this@ScreenshotViewHolder)
            }
        }

        override fun onClick(v: View?) {
            if (v == itemView) {
                clickListener.onScreenshotClick(currentItem)
            }
        }

        override fun onBindImpl(item: ScreenshotItem) {
            val image = image
            image.left = 0
            image.right = 0
            image.load(item.screenshot.url(
                context = image.context,
                repository = item.repository,
                packageName = item.packageName,
            )) {
                authentication(item.repository.authentication)
                scale(Scale.FILL)
                placeholder(placeholder)
                error(placeholder.asImage())
            }
        }

        override fun onRecycled() {
            image.dispose()
        }
    }

    suspend fun setScreenshots(
        newList: List<ScreenshotsAdapterItem>
    ): Unit = coroutineScope {
        val oldList = items
        val diff = withContext(defaultDispatcher) {
            DiffUtil.calculateDiff(DiffCallback(oldList, newList))
        }
        ensureActive()
        items = newList
        diff.dispatchUpdatesTo(this@ScreenshotsAdapter)
    }

    override val viewTypeClass: Class<ViewType> get() = ViewType::class.java
    override fun getItemCount(): Int = items.size
    override fun getItemEnumViewType(position: Int) = getItem(position).viewType

    private fun getItem(position: Int): ScreenshotsAdapterItem = items[position]

    @Suppress("UNCHECKED_CAST")
    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: ViewType,
    ): BaseViewHolder<ScreenshotsAdapterItem> {
        return when (viewType) {
            ViewType.VIDEO -> VideoViewHolder(VideoButtonBinding.inflate(parent.context.layoutInflater))
            ViewType.SCREENSHOT -> ScreenshotViewHolder(parent.context, onScreenshotClick)
        } as BaseViewHolder<ScreenshotsAdapterItem>
    }

    override fun onBindViewHolder(holder: BaseViewHolder<ScreenshotsAdapterItem>, position: Int) {
        holder.onBind(getItem(position))
    }

    override fun onViewRecycled(holder: BaseViewHolder<ScreenshotsAdapterItem>) {
        super.onViewRecycled(holder)
        holder.onRecycled()
    }

    private class DiffCallback(
        private val oldList: List<ScreenshotsAdapterItem>,
        private val newList: List<ScreenshotsAdapterItem>,
    ): DiffUtil.Callback() {
        override fun getOldListSize(): Int {
            return oldList.size
        }

        override fun getNewListSize(): Int {
            return newList.size
        }

        override fun areItemsTheSame(
            oldItemPosition: Int,
            newItemPosition: Int,
        ): Boolean {
            return oldList[oldItemPosition] == newList[newItemPosition]
        }

        override fun areContentsTheSame(
            oldItemPosition: Int,
            newItemPosition: Int,
        ): Boolean {
            return true
        }
    }
}
