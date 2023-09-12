package com.looker.core.common.extension

import android.app.NotificationManager
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.drawable.Drawable
import androidx.annotation.AttrRes
import androidx.annotation.DrawableRes
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import com.looker.core.common.R

inline val Context.notificationManager: NotificationManager
	get() = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

fun Context.getColorFromAttr(@AttrRes attrResId: Int): ColorStateList {
	val typedArray = obtainStyledAttributes(intArrayOf(attrResId))
	val (colorStateList, resId) = try {
		Pair(typedArray.getColorStateList(0), typedArray.getResourceId(0, 0))
	} finally {
		typedArray.recycle()
	}
	return colorStateList ?: ContextCompat.getColorStateList(this, resId)!!
}

val Context.copy: Drawable
	get() = getDrawableCompat(R.drawable.ic_copy)

val Context.divider: Drawable
	get() = getDrawableFromAttr(android.R.attr.listDivider)

val Context.homeAsUp: Drawable
	get() = getDrawableFromAttr(android.R.attr.homeAsUpIndicator)

val Context.selectableBackground: Drawable
	get() = getDrawableFromAttr(android.R.attr.selectableItemBackground)

val Context.corneredBackground: Drawable
	get() = getDrawableCompat(R.drawable.background_border)

val Context.aspectRatio: Float
	get() = resources.displayMetrics.heightPixels.toFloat() / resources.displayMetrics.widthPixels.toFloat()

fun Context.getMutatedIcon(@DrawableRes id: Int): Drawable = getDrawableCompat(id).mutate()

private fun Context.getDrawableFromAttr(attrResId: Int): Drawable {
	val typedArray = obtainStyledAttributes(intArrayOf(attrResId))
	val resId = try {
		typedArray.getResourceId(0, 0)
	} finally {
		typedArray.recycle()
	}
	return getDrawableCompat(resId)
}

private fun Context.getDrawableCompat(@DrawableRes resId: Int = R.drawable.background_border): Drawable =
	ResourcesCompat.getDrawable(resources, resId, theme) ?: ContextCompat.getDrawable(this, resId)!!