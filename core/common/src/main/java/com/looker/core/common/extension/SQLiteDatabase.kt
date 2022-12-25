package com.looker.core.common.extension

import android.database.sqlite.SQLiteDatabase

fun SQLiteDatabase.execWithResult(sql: String) {
	rawQuery(sql, null).use { it.count }
}