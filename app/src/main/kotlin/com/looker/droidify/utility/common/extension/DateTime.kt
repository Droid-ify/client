package com.looker.droidify.utility.common.extension

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

private object DateTime {
    val HTTP_DATE_FORMAT: SimpleDateFormat
        get() = SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("GMT")
        }
}

fun Date.toFormattedString(): String = DateTime.HTTP_DATE_FORMAT.format(this)

fun String.toDate(): Date = DateTime.HTTP_DATE_FORMAT.parse(this)
    ?: throw IllegalStateException("Wrong Date Format")
