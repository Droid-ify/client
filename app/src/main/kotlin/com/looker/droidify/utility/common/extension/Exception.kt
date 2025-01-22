package com.looker.droidify.utility.common.extension

import kotlinx.coroutines.CancellationException

inline fun Exception.exceptCancellation() {
    printStackTrace()
    if (this is CancellationException) throw this
}
