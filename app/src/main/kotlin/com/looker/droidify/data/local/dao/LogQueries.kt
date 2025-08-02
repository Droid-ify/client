package com.looker.droidify.data.local.dao

import android.util.Log

fun logQuery(vararg param: Pair<String, Any?>) {
    param.forEach { (key, value) ->
    }
    val message = buildString {
        append("(")
        param.forEach { (key, value) ->
            append("$key: $value, ")
        }
        append(")")
    }
    Log.d("RoomQuery", message)
}
