package com.looker.droidify.database

import android.os.Handler
import java.io.Closeable

interface DbCursor<T> : Closeable {
    val count: Int
    val position: Int
    fun moveToPosition(position: Int): Boolean
    fun moveToNext(): Boolean
    fun readItem(): T
    fun registerOnInvalidatedCallback(handler: Handler, callback: () -> Unit)
}

fun <V, T: DbCursor<V>> T.asItemSequence(): Sequence<V> {
    return generateSequence { if (moveToNext()) this else null }.map { readItem() }
}

fun <V, T: DbCursor<V>> T.readToListAndClose(): List<V> {
    return use {
        it.asItemSequence().toList()
    }
}

fun <V, T: DbCursor<V>> T.firstItemOrNull(): V? {
    return (if (moveToNext()) this else null)?.readItem()
}
