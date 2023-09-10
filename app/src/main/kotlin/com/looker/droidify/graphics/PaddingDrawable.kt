package com.looker.droidify.graphics

import android.graphics.Rect
import android.graphics.drawable.Drawable
import kotlin.math.roundToInt

class PaddingDrawable(
	drawable: Drawable,
	private val horizontalFactor: Float,
	private val aspectRatio: Float = 16f / 9f
) : DrawableWrapper(drawable) {
	override fun getIntrinsicWidth(): Int = (horizontalFactor * super.getIntrinsicWidth()).roundToInt()
	override fun getIntrinsicHeight(): Int = ((horizontalFactor * aspectRatio) * super.getIntrinsicHeight()).roundToInt()

	override fun onBoundsChange(bounds: Rect) {
		val width = (bounds.width() / horizontalFactor).roundToInt()
		val height = (bounds.height() / (horizontalFactor * aspectRatio)).roundToInt()
		val left = (bounds.width() - width) / 2
		val top = (bounds.height() - height) / 2
		drawable.setBounds(
			bounds.left + left, bounds.top + top,
			bounds.left + left + width, bounds.top + top + height
		)
	}
}
