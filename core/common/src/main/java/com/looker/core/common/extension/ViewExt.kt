package com.looker.core.common.extension

import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.marginBottom
import androidx.core.view.marginLeft
import androidx.core.view.marginRight
import androidx.core.view.marginTop
import androidx.core.view.updateLayoutParams
import androidx.core.view.updatePadding
import androidx.core.widget.NestedScrollView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.appbar.AppBarLayout
import com.looker.core.common.Util
import com.looker.core.common.extension.InsetSides.*
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

fun View.systemBarsMargin(
	allowedSides: List<InsetSides> = listOf(LEFT, RIGHT, BOTTOM)
) {
	if (Util.isR) {
		ViewCompat.setOnApplyWindowInsetsListener(this) { view, windowInsets ->
			val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
			view.updateLayoutParams<ViewGroup.MarginLayoutParams> {
				if (TOP in allowedSides) topMargin = insets.top + marginTop
				if (LEFT in allowedSides) leftMargin = insets.left + marginLeft
				if (BOTTOM in allowedSides) bottomMargin = insets.bottom + marginBottom
				if (RIGHT in allowedSides) rightMargin = insets.right + marginRight
			}
			WindowInsetsCompat.CONSUMED
		}
	}
}

fun RecyclerView.systemBarsPadding(
	allowedSides: List<InsetSides> = listOf(LEFT, RIGHT, BOTTOM)
) {
	if (Util.isR) {
		ViewCompat.setOnApplyWindowInsetsListener(this) { view, windowInsets ->
			clipToPadding = false
			val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
			view.updatePadding(
				if (LEFT in allowedSides) insets.left else 0,
				if (TOP in allowedSides) insets.top else 0,
				if (RIGHT in allowedSides) insets.right else 0,
				if (BOTTOM in allowedSides) insets.bottom else 0
			)
			WindowInsetsCompat.CONSUMED
		}
	}
}

fun NestedScrollView.systemBarsPadding(
	allowedSides: List<InsetSides> = listOf(LEFT, RIGHT, BOTTOM)
) {
	if (Util.isR) {
		ViewCompat.setOnApplyWindowInsetsListener(this) { view, windowInsets ->
			clipToPadding = false
			val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
			view.updatePadding(
				if (LEFT in allowedSides) insets.left else 0,
				if (TOP in allowedSides) insets.top else 0,
				if (RIGHT in allowedSides) insets.right else 0,
				if (BOTTOM in allowedSides) insets.bottom else 0
			)
			WindowInsetsCompat.CONSUMED
		}
	}
}

enum class InsetSides {
	LEFT,
	RIGHT,
	TOP,
	BOTTOM
}