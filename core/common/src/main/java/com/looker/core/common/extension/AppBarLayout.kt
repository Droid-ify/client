package com.looker.core.common.extension

import androidx.coordinatorlayout.widget.CoordinatorLayout
import com.google.android.material.appbar.AppBarLayout
import com.looker.core.common.view.CustomCollapsingBehaviour

fun AppBarLayout.setCollapsable(collapsing: Boolean = false) {
	setExpanded(collapsing, true)
	((layoutParams as CoordinatorLayout.LayoutParams).behavior as CustomCollapsingBehaviour)
		.isShouldScroll = collapsing
}
