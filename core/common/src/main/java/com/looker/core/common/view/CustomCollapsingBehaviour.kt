package com.looker.core.common.view

import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import androidx.coordinatorlayout.widget.CoordinatorLayout
import com.google.android.material.appbar.AppBarLayout

class CustomCollapsingBehaviour(context: Context, attrs: AttributeSet) :
	AppBarLayout.Behavior(context, attrs) {
	var isShouldScroll = false

	override fun onStartNestedScroll(
		parent: CoordinatorLayout,
		child: AppBarLayout,
		directTargetChild: View,
		target: View,
		nestedScrollAxes: Int,
		type: Int
	) = isShouldScroll

	override fun onTouchEvent(parent: CoordinatorLayout, child: AppBarLayout, ev: MotionEvent) =
		isShouldScroll && super.onTouchEvent(parent, child, ev)

}