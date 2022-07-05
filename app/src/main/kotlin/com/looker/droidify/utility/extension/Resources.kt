@file:Suppress("PackageDirectoryMismatch")

package com.looker.droidify.utility.extension.resources

import android.content.Context
import android.content.res.ColorStateList
import android.content.res.Resources
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.AttrRes
import androidx.annotation.DrawableRes
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import coil.util.CoilUtils
import com.google.android.material.imageview.ShapeableImageView
import com.google.android.material.textview.MaterialTextView
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

fun Context.getColorFromAttr(@AttrRes attrResId: Int): ColorStateList {
	val typedArray = obtainStyledAttributes(intArrayOf(attrResId))
	val (colorStateList, resId) = try {
		Pair(typedArray.getColorStateList(0), typedArray.getResourceId(0, 0))
	} finally {
		typedArray.recycle()
	}
	return colorStateList ?: ContextCompat.getColorStateList(this, resId)!!
}

fun Context.getDrawableFromAttr(attrResId: Int): Drawable {
	val typedArray = obtainStyledAttributes(intArrayOf(attrResId))
	val resId = try {
		typedArray.getResourceId(0, 0)
	} finally {
		typedArray.recycle()
	}
	return getDrawableCompat(resId)
}

fun Context.getDrawableCompat(@DrawableRes resId: Int): Drawable =
	ResourcesCompat.getDrawable(resources, resId, theme) ?: ContextCompat.getDrawable(this, resId)!!


fun Resources.sizeScaled(size: Int): Int {
	return (size * displayMetrics.density).roundToInt()
}

fun MaterialTextView.setTextSizeScaled(size: Int) {
	val realSize = (size * resources.displayMetrics.scaledDensity).roundToInt()
	setTextSize(TypedValue.COMPLEX_UNIT_PX, realSize.toFloat())
}

fun ViewGroup.inflate(layoutResId: Int): View {
	return LayoutInflater.from(context).inflate(layoutResId, this, false)
}

fun ShapeableImageView.clear() {
	CoilUtils.dispose(this)
}