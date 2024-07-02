package com.looker.core.common

import android.net.Uri

interface Exporter<T> {

    suspend fun export(item: T, target: Uri)

    suspend fun import(target: Uri): T

}
