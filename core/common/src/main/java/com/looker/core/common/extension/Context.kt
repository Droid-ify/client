package com.looker.core.common.extension

import android.app.NotificationManager
import android.app.job.JobScheduler
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.drawable.Drawable
import android.net.ConnectivityManager
import android.os.PowerManager
import android.view.inputmethod.InputMethodManager
import androidx.annotation.AttrRes
import androidx.annotation.DrawableRes
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.content.ContextCompat
import androidx.core.content.getSystemService
import com.looker.core.common.R

inline val Context.clipboardManager: ClipboardManager?
    get() = getSystemService()

inline val Context.connectivityManager: ConnectivityManager?
    get() = getSystemService()

inline val Context.inputManager: InputMethodManager?
    get() = getSystemService()

inline val Context.jobScheduler: JobScheduler?
    get() = getSystemService()

inline val Context.notificationManager: NotificationManager?
    get() = getSystemService()

inline val Context.powerManager: PowerManager?
    get() = getSystemService()

fun Context.copyToClipboard(clip: String) {
    clipboardManager?.setPrimaryClip(ClipData.newPlainText(null, clip))
}

val Context.corneredBackground: Drawable
    get() = getDrawableCompat(R.drawable.background_border)

val Context.divider: Drawable
    get() = getDrawableFromAttr(android.R.attr.listDivider)

val Context.homeAsUp: Drawable
    get() = getDrawableFromAttr(android.R.attr.homeAsUpIndicator)

val Context.open: Drawable
    get() = getDrawableCompat(R.drawable.ic_launch)

val Context.selectableBackground: Drawable
    get() = getDrawableFromAttr(android.R.attr.selectableItemBackground)

val Context.camera: Drawable
    get() = getDrawableCompat(R.drawable.ic_image)

val Context.aspectRatio: Float
    get() = with(resources.displayMetrics) {
        (heightPixels / widthPixels).toFloat()
    }

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

fun Context.getDrawableCompat(@DrawableRes resId: Int = R.drawable.background_border): Drawable =
    requireNotNull(AppCompatResources.getDrawable(this, resId)) { "Cannot find drawable, ID: $resId" }

fun Context.getColorFromAttr(@AttrRes attrResId: Int): ColorStateList {
    val typedArray = obtainStyledAttributes(intArrayOf(attrResId))
    val (colorStateList, resId) = try {
        Pair(typedArray.getColorStateList(0), typedArray.getResourceId(0, 0))
    } finally {
        typedArray.recycle()
    }
    return colorStateList ?: ContextCompat.getColorStateList(this, resId)!!
}
