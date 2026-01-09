package com.looker.droidify.utility.common

import android.util.Log
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.TimeZone
import kotlinx.datetime.YearMonth
import kotlinx.datetime.minus
import kotlinx.datetime.plusMonth
import kotlinx.datetime.toLocalDateTime
import kotlinx.datetime.yearMonth
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

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

/**
 * Instance        : YYYY-MM-DD; YYYY-MM ; YYYY
 * Returned integer: YYYYMMDD  ; YYYYMM00; YYYY0000
 */
fun String.isoDateToInt(): Int {
    val parts = split("-")
    return when (parts.size) {
        // YYYY-MM-DD
        3    -> (parts[0] + parts[1] + parts[2]).toInt()
        // YYYY-MM
        2    -> (parts[0] + parts[1] + "00").toInt()
        // YYYY
        1    -> (parts[0] + "0000").toInt()
        else -> throw IllegalArgumentException("Invalid date format")
    }
}

/**
 * Returned format: YYYY-MM-01
 */
@OptIn(ExperimentalTime::class)
fun getIsoDateOfMonthsAgo(months: Int): String = Clock.System.now()
    .toLocalDateTime(TimeZone.currentSystemDefault()).date
    .minus(DatePeriod(months = months, days = 1)).toString()

/**
 * Generates list of monthly file names from start date to current month
 * Format: YYYY-MM.json
 */
@OptIn(ExperimentalTime::class)
fun generateMonthlyFileNames(): List<String> {
    val current = Clock.System.now()
        .toLocalDateTime(TimeZone.currentSystemDefault()).date.yearMonth
    val start = YearMonth(2024, 12) // year and month of first report

    val fileNames = mutableListOf<String>()
    var ym = start
    while (ym <= current) {
        fileNames.add("$ym.json")
        ym = ym.plusMonth()
    }
    return fileNames
}
