package com.looker.core.common.extension

import android.database.Cursor

fun Cursor.asSequence(): Sequence<Cursor> {
	return generateSequence { if (moveToNext()) this else null }
}

fun Cursor.firstOrNull(): Cursor? {
	return if (moveToFirst()) this else null
}