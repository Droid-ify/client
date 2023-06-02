package com.looker.core.common.extension

import kotlinx.coroutines.CancellationException

inline fun Exception.exceptCancellation() {
	if (this is CancellationException) throw this
	printStackTrace()
}