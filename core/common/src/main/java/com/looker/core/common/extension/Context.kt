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

val Context.ERROR_APP_ICON: Drawable
	get() = getDrawableCompat(R.drawable.ic_application_default).mutate()
		.apply { setTintList(getColorFromAttr(R.attr.colorErrorContainer)) }

val Context.PLACEHOLDER_APP_ICON: Drawable
	get() = getDrawableCompat(R.drawable.ic_application_default).mutate()
		.apply { setTintList(getColorFromAttr(R.attr.colorOutline)) }

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