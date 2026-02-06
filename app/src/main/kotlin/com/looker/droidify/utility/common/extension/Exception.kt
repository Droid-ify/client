package com.looker.droidify.utility.common.extension

import kotlinx.coroutines.CancellationException

@Suppress("NOTHING_TO_INLINE")
inline fun Exception.exceptCancellation() {
    printStackTrace()
    if (this is CancellationException) throw this
}
