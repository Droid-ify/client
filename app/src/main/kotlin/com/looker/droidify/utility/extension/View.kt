package com.looker.droidify.utility.extension

import android.view.View
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.view.ViewCompat
import com.google.android.material.appbar.AppBarLayout

fun View.setCollapsingBar(
	appBar: AppBarLayout,
	collapsable: () -> Boolean = { false }
) {
	val params = appBar.layoutParams as CoordinatorLayout.LayoutParams
	if (params.behavior == null)
		params.behavior = AppBarLayout.Behavior()
	val behaviour = params.behavior as AppBarLayout.Behavior
	behaviour.setDragCallback(object : AppBarLayout.Behavior.DragCallback() {
		override fun canDrag(appBarLayout: AppBarLayout): Boolean {
			return false
		}
	})
	appBar.setExpanded(collapsable(), true)
	ViewCompat.setNestedScrollingEnabled(this, collapsable())
}