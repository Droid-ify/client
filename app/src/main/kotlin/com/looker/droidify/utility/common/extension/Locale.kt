package com.looker.droidify.utility.common.extension

import java.util.Locale

fun String.toLocale(): Locale = when {
    contains("-r") -> Locale(
        substring(0, 2),
        substring(4)
    )

    contains("_") -> Locale(
        substring(0, 2),
        substring(3)
    )

    else -> Locale(this)
}
