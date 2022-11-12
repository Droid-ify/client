@file:Suppress("PackageDirectoryMismatch")

package com.looker.droidify.utility.extension.resources

import android.content.res.Resources
import android.graphics.Typeface
import android.util.TypedValue
import kotlin.math.roundToInt

object TypefaceExtra {
	val medium = Typeface.create("sans-serif-medium", Typeface.NORMAL)!!
	val light = Typeface.create("sans-serif-light", Typeface.NORMAL)!!
}

val Number.toPx
	get() = TypedValue.applyDimension(
		TypedValue.COMPLEX_UNIT_DIP,
		this.toFloat(),
		Resources.getSystem().displayMetrics
	)

fun Resources.sizeScaled(size: Int): Int {
	return (size * displayMetrics.density).roundToInt()
}