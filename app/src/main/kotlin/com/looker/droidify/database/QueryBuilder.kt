package com.looker.droidify.database

import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.os.CancellationSignal
import com.looker.droidify.BuildConfig
import com.looker.droidify.utility.common.extension.asSequence
import com.looker.droidify.utility.common.log

class QueryBuilder {

    private val builder = StringBuilder(256)
    private val arguments = mutableListOf<String>()

    operator fun plusAssign(query: String) {
        if (builder.isNotEmpty()) {
            builder.append(" ")
        }
        builder.trimAndJoin(query)
    }

    operator fun remAssign(argument: String) {
        this.arguments += argument
    }

    operator fun remAssign(arguments: List<String>) {
        this.arguments += arguments
    }

    fun query(db: SQLiteDatabase, signal: CancellationSignal?): Cursor {
        val query = builder.toString()
        val arguments = arguments.toTypedArray()
        if (BuildConfig.DEBUG) {
            synchronized(QueryBuilder::class.java) {
                log(query)
                db.rawQuery("EXPLAIN QUERY PLAN $query", arguments).use {
                    it.asSequence()
                        .forEach { log(":: ${it.getString(it.getColumnIndex("detail"))}") }
                }
            }
        }
        return db.rawQuery(query, arguments, signal)
    }
}

fun StringBuilder.trimAndJoin(
    input: String,
) {
    var isFirstLine = true
    var startOfLine = 0
    for (i in input.indices) {
        val char = input[i]
        when {
            char == '\n' -> {
                trimAndAppendLine(input, startOfLine, i, this, isFirstLine)
                isFirstLine = false
                startOfLine = i + 1
            }

            else -> {
                if (i == input.lastIndex) {
                    trimAndAppendLine(input, startOfLine, i + 1, this, isFirstLine)
                }
            }
        }
    }
}

private fun trimAndAppendLine(
    input: String,
    start: Int,
    end: Int,
    builder: StringBuilder,
    isFirstLine: Boolean,
) {
    var lineStart = start
    var lineEnd = end - 1

    while (lineStart <= lineEnd && input[lineStart].isWhitespace()) {
        lineStart++
    }

    while (lineEnd >= lineStart && input[lineEnd].isWhitespace()) {
        lineEnd--
    }

    if (lineStart <= lineEnd) {
        if (!isFirstLine) {
            builder.append(' ')
        }
        builder.append(input, lineStart, lineEnd + 1)
    }
}

