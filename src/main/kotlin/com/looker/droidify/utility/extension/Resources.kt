@file:Suppress("PackageDirectoryMismatch")

package com.looker.droidify.utility.extension.resources

import android.content.Context
import android.content.res.ColorStateList
import android.content.res.Resources
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import android.graphics.drawable.VectorDrawable
import android.net.Uri
import android.util.TypedValue
import android.util.Xml
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import coil.load
import coil.request.ImageRequest
import coil.util.CoilUtils
import com.looker.droidify.utility.extension.android.Android
import org.xmlpull.v1.XmlPullParser
import kotlin.math.roundToInt

object TypefaceExtra {
    val bold = Typeface.create("sans-serif-medium", Typeface.BOLD)!!
    val medium = Typeface.create("sans-serif-medium", Typeface.NORMAL)!!
    val light = Typeface.create("sans-serif-light", Typeface.NORMAL)!!
}

val Number.toPx
    get() = TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_DIP,
        this.toFloat(),
        Resources.getSystem().displayMetrics
    )

fun Context.getDrawableCompat(resId: Int): Drawable {
    val drawable = if (!Android.sdk(24)) {
        val fileName = TypedValue().apply { resources.getValue(resId, this, true) }.string
        if (fileName.endsWith(".xml")) {
            resources.getXml(resId).use { it ->
                val eventType = generateSequence { it.next() }
                    .find { it == XmlPullParser.START_TAG || it == XmlPullParser.END_DOCUMENT }
                if (eventType == XmlPullParser.START_TAG) {
                    when (it.name) {
                        "vector" -> VectorDrawable.createFromXmlInner(
                            resources,
                            it,
                            Xml.asAttributeSet(it),
                            theme
                        )
                        else -> null
                    }
                } else {
                    null
                }
            }
        } else {
            null
        }
    } else {
        null
    }
    return drawable ?: ContextCompat.getDrawable(this, resId)!!
}

fun Context.getColorFromAttr(attrResId: Int): ColorStateList {
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

fun Resources.sizeScaled(size: Int): Int {
    return (size * displayMetrics.density).roundToInt()
}

fun TextView.setTextSizeScaled(size: Int) {
    val realSize = (size * resources.displayMetrics.scaledDensity).roundToInt()
    setTextSize(TypedValue.COMPLEX_UNIT_PX, realSize.toFloat())
}

fun ViewGroup.inflate(layoutResId: Int): View {
    return LayoutInflater.from(context).inflate(layoutResId, this, false)
}

fun ImageView.load(uri: Uri, builder: ImageRequest.Builder.() -> Unit = {}) {
    this.load(uri = uri, builder = builder)
}

fun ImageView.clear() {
    CoilUtils.clear(this)
}
