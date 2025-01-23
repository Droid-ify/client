package com.looker.droidify.utility.common

import android.util.Log

fun <T : CharSequence> T.nullIfEmpty(): T? {
    return if (isNullOrBlank()) null else this
}

fun Any.log(
    message: Any?,
    tag: String = this::class.java.simpleName + ".DEBUG",
    type: Int = Log.DEBUG
) {
    Log.println(type, tag, message.toString())
}
