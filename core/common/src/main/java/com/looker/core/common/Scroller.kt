package com.looker.core.common

import android.content.Context
import android.util.DisplayMetrics
import android.view.View
import androidx.recyclerview.widget.LinearSmoothScroller
import androidx.recyclerview.widget.RecyclerView
import kotlin.math.abs

/**
 * A custom LinearSmoothScroller that increases the scrolling speed quadratically
 * based on the distance already scrolled.
 *
 * @param context The context used to access resources.
 */
class Scroller(context: Context) : LinearSmoothScroller(context) {
    private var distanceScrolled = 0

    /**
     * Calculates the speed per pixel based on the display metrics and the distance
     * already scrolled. The speed increases quadratically over time.
     *
     * @param displayMetrics The display metrics used to calculate the speed.
     * @return The speed per pixel.
     */
    override fun calculateSpeedPerPixel(displayMetrics: DisplayMetrics): Float {
        return (10f / displayMetrics.densityDpi) / (1 + 0.001f * distanceScrolled * distanceScrolled)
    }

    /**
     * Called when the target view is found. Resets the distance scrolled.
     *
     * @param targetView The target view.
     * @param state The current state of RecyclerView.
     * @param action The action to be performed.
     */
    override fun onTargetFound(targetView: View, state: RecyclerView.State, action: Action) {
        super.onTargetFound(targetView, state, action)
        distanceScrolled = 0
    }

    /**
     * Called when seeking the target step. Accumulates the distance scrolled.
     *
     * @param dx The amount of horizontal scroll.
     * @param dy The amount of vertical scroll.
     * @param state The current state of RecyclerView.
     * @param action The action to be performed.
     */
    override fun onSeekTargetStep(dx: Int, dy: Int, state: RecyclerView.State, action: Action) {
        super.onSeekTargetStep(dx, dy, state, action)
        distanceScrolled += abs(dy)
    }
}
