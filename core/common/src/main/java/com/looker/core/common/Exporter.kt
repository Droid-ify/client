package com.looker.core.common

import android.net.Uri

interface Exporter<T> {

    suspend fun saveToFile(item: T, target: Uri)

    suspend fun readFromFile(target: Uri): T

}
