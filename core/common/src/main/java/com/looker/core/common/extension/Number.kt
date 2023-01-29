package com.looker.core.common.extension

infix fun Long.percentBy(denominator: Long?): Int {
	if (denominator == null || denominator < 1) return -1
	return (this * 100 / denominator).toInt()
}
