package com.looker.core.common

infix fun Int.percentBy(denominator: Int?): Int =
	this * 100 / (denominator ?: -1)

infix fun Long.percentBy(denominator: Long?): Int =
	(toInt() * 100) / (denominator ?: -1L).toInt()
