package com.looker.droidify.ui.appDetail.viewHolders

import android.content.res.Resources
import android.view.View

class Measurement<T : Any> {
    private var density = 0f
    private var scaledDensity = 0f
    private lateinit var metric: T

    fun measure(view: View) {
        val spec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
        view.measure(spec, spec)
    }

    @Suppress("DEPRECATION")
    fun invalidate(resources: Resources, callback: () -> T): T {
        val displayMetrics = resources.displayMetrics
        val density = displayMetrics.density
        val scaledDensity = displayMetrics.scaledDensity

        if (this.density != density || this.scaledDensity != scaledDensity) {
            this.density = density
            this.scaledDensity = scaledDensity
            metric = callback()
        }
        return metric
    }
}
