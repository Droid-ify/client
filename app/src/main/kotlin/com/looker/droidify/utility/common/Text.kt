package com.looker.droidify.utility.common

import android.util.Log
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlin.time.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.YearMonth
import kotlinx.datetime.plusMonth
import kotlinx.datetime.toLocalDateTime
import kotlinx.datetime.yearMonth

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

@OptIn(ExperimentalTime::class)
fun generateMonthlyFileNames(since: Long?): List<String> = buildList {
    val current = Clock.System.now()
        .toLocalDateTime(TimeZone.UTC)
        .date.yearMonth

    // Download stats project started on `Dec 2024`
    var ym = if (since == null) {
        YearMonth(2024, 12)
    } else {
        Instant.fromEpochMilliseconds(since).toLocalDateTime(TimeZone.UTC).date.yearMonth
    }
    while (ym <= current) {
        add("$ym.json")
        ym = ym.plusMonth()
    }
}
