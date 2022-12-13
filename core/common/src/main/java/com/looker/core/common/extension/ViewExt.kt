package com.looker.core.common.extension

import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.coordinatorlayout.widget.CoordinatorLayout
import com.google.android.material.appbar.AppBarLayout
import com.looker.core.common.view.CustomCollapsingBehaviour
import kotlin.math.roundToInt

fun TextView.setTextSizeScaled(size: Int) {
	val realSize = (size * resources.displayMetrics.scaledDensity).roundToInt()
	setTextSize(TypedValue.COMPLEX_UNIT_PX, realSize.toFloat())
}

fun ViewGroup.inflate(layoutResId: Int): View {
	return LayoutInflater.from(context).inflate(layoutResId, this, false)
}

fun AppBarLayout.setCollapsable(collapsing: Boolean = false) {
	setExpanded(collapsing, true)
	((layoutParams as CoordinatorLayout.LayoutParams).behavior as CustomCollapsingBehaviour)
		.isShouldScroll = collapsing
}