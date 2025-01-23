package com.looker.droidify.utility.common.extension

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

private val HTTP_DATE_FORMAT: SimpleDateFormat
    get() = SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z", Locale.US).apply {
        timeZone = TimeZone.getTimeZone("GMT")
    }

fun Date.toFormattedString(): String = HTTP_DATE_FORMAT.format(this)
