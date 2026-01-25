package com.looker.droidify.utility.common.extension

import android.os.Bundle

private fun Boolean.toByte(): Byte = if (this) 1.toByte() else 0.toByte()

fun Bundle.compatPutBoolean(key: String, value: Boolean) {
    putByte(key, value.toByte())
}

fun Bundle.compatReadBoolean(key: String, defaultValue: Boolean): Boolean {
    return getByte(key, defaultValue.toByte()) == 1.toByte()
}
