package com.looker.core.common.extension

import android.content.res.Resources
import android.util.TypedValue
import kotlin.math.roundToInt

infix fun Long.percentBy(denominator: Long?): Int {
	if (denominator == null || denominator < 1) return -1
	return (this * 100 / denominator).toInt()
}

val Number.px
	get() = TypedValue.applyDimension(
		TypedValue.COMPLEX_UNIT_DIP,
		this.toFloat(),
		Resources.getSystem().displayMetrics
	)

val Int.dp
	get() = (this * Resources.getSystem().displayMetrics.density).roundToInt()