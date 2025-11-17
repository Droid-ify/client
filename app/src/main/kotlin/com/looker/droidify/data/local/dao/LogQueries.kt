package com.looker.droidify.data.local.dao

import android.util.Log

fun logQuery(vararg param: Pair<String, Any?>) {
    param.forEach { (key, value) ->
    }
    val message = buildString {
        appendLine("(")
        param.forEachIndexed { index, (key, value) ->
            appendLine("\t$key: $value,")
        }
        appendLine(")")
    }
    Log.d("RoomQuery", message)
}
