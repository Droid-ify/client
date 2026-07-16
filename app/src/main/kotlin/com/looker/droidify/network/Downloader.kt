package com.looker.droidify.network

import okhttp3.Headers
import java.io.File

interface Downloader {

    suspend fun headCall(
        url: String,
        headers: Headers.Builder.() -> Unit = {},
    ): NetworkResponse

    suspend fun downloadToFile(
        url: String,
        target: File,
        headers: Headers.Builder.() -> Unit = {},
        block: ProgressListener? = null,
    ): NetworkResponse
}

typealias ProgressListener = suspend (bytesReceived: DataSize, contentLength: DataSize?) -> Unit
