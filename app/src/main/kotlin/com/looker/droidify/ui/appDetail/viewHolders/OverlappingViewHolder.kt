package com.looker.droidify.ui.appDetail.viewHolders

import android.annotation.SuppressLint
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.looker.droidify.ui.appDetail.AppDetailItem
import com.looker.droidify.utility.extension.resources.sizeScaled

@SuppressLint("ClickableViewAccessibility")
abstract class OverlappingViewHolder<T: AppDetailItem>(itemView: View) : BaseViewHolder<T>(itemView) {
    init {
        // Block touch events if touched above negative margin
        itemView.setOnTouchListener { _, event ->
            event.action == MotionEvent.ACTION_DOWN && run {
                val top = (itemView.layoutParams as ViewGroup.MarginLayoutParams).topMargin
                top < 0 && event.y < -top
            }
        }
    }

    private val topMargin: Int = itemView.context.resources.sizeScaled(8)

    var hasTopMargin = false
        set(value) {
            if (field != value) {
                field = value
                val layoutParams = itemView.layoutParams as RecyclerView.LayoutParams
                layoutParams.topMargin = if (value) {
                    -topMargin
                } else {
                    0
                }
            }
        }
}
