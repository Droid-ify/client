package com.looker.droidify.graphics

import android.graphics.Rect
import android.graphics.drawable.Drawable
import kotlin.math.roundToInt

class PaddingDrawable(drawable: Drawable, private val factor: Float) : DrawableWrapper(drawable) {
    override fun getIntrinsicWidth(): Int = (factor * super.getIntrinsicWidth()).roundToInt()
    override fun getIntrinsicHeight(): Int = (factor * super.getIntrinsicHeight()).roundToInt()

    override fun onBoundsChange(bounds: Rect) {
        val width = (bounds.width() / factor).roundToInt()
        val height = (bounds.height() / factor).roundToInt()
        val left = (bounds.width() - width) / 2
        val top = (bounds.height() - height) / 2
        drawable.setBounds(
            bounds.left + left, bounds.top + top,
            bounds.left + left + width, bounds.top + top + height
        )
    }
}
