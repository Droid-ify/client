package com.looker.droidify.graphics

import android.graphics.Canvas
import android.graphics.ColorFilter
import android.graphics.Rect
import android.graphics.drawable.Drawable

open class DrawableWrapper(val drawable: Drawable) : Drawable() {
    init {
        drawable.callback = object : Callback {
            override fun invalidateDrawable(who: Drawable) {
                callback?.invalidateDrawable(who)
            }

            override fun scheduleDrawable(who: Drawable, what: Runnable, `when`: Long) {
                callback?.scheduleDrawable(who, what, `when`)
            }

            override fun unscheduleDrawable(who: Drawable, what: Runnable) {
                callback?.unscheduleDrawable(who, what)
            }
        }
    }

    override fun onBoundsChange(bounds: Rect) {
        drawable.bounds = bounds
    }

    override fun getIntrinsicWidth(): Int = drawable.intrinsicWidth
    override fun getIntrinsicHeight(): Int = drawable.intrinsicHeight
    override fun getMinimumWidth(): Int = drawable.minimumWidth
    override fun getMinimumHeight(): Int = drawable.minimumHeight

    override fun draw(canvas: Canvas) {
        drawable.draw(canvas)
    }

    override fun getAlpha(): Int {
        return drawable.alpha
    }

    override fun setAlpha(alpha: Int) {
        drawable.alpha = alpha
    }

    override fun getColorFilter(): ColorFilter? {
        return drawable.colorFilter
    }

    override fun setColorFilter(colorFilter: ColorFilter?) {
        drawable.colorFilter = colorFilter
    }

    @Suppress("DEPRECATION")
    override fun getOpacity(): Int = drawable.opacity
}
