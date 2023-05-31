package com.looker.core.common.extension

import kotlinx.coroutines.CancellationException

@Suppress("RedundantSuspendModifier")
// [suspend] to ensure only be used in coroutine scope
suspend inline fun Exception.exceptCancellation() {
	if (this is CancellationException) throw this
	printStackTrace()
}