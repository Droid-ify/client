package com.looker.droidify.utility.common

import android.util.Log
import java.text.DateFormat
import java.util.Calendar
import java.util.Date
import java.util.TimeZone

private val UTC: TimeZone = TimeZone.getTimeZone("UTC")

fun <T : CharSequence> T.nullIfEmpty(): T? {
    return if (isNullOrBlank()) null else this
}

fun Any.log(
    message: Any?,
    tag: String = this::class.java.simpleName + ".DEBUG",
    type: Int = Log.DEBUG,
) {
    Log.println(type, tag, message.toString())
}

fun formatDate(millis: Long): String = DateFormat
    .getDateInstance(DateFormat.SHORT)
    .apply { timeZone = UTC }
    .format(Date(millis))

fun monthlyFileNamesSince(since: Long?): List<String> = buildList {
    val calendar = Calendar.getInstance(UTC)
    val currentYear = calendar.get(Calendar.YEAR)
    val currentMonth = calendar.get(Calendar.MONTH) + 1

    // Download stats project started on `Dec 2024`
    var year = 2024
    var month = 12
    if (since != null) {
        calendar.timeInMillis = since
        year = calendar.get(Calendar.YEAR)
        month = calendar.get(Calendar.MONTH) + 1
    }
    while (year < currentYear || (year == currentYear && month <= currentMonth)) {
        add("%04d-%02d.json".format(year, month))
        if (month == 12) {
            year += 1
            month = 1
        } else {
            month += 1
        }
    }
}
