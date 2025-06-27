package com.looker.droidify.network

import com.looker.droidify.network.header.HeadersBuilder
import com.looker.droidify.network.validation.FileValidator
import java.io.File
import java.net.Proxy

interface Downloader {

    fun setProxy(proxy: Proxy)

    suspend fun headCall(
        url: String,
        headers: HeadersBuilder.() -> Unit = {}
    ): NetworkResponse

    suspend fun downloadToFile(
        url: String,
        target: File,
        validator: FileValidator? = null,
        headers: HeadersBuilder.() -> Unit = {},
        block: ProgressListener? = null
    ): NetworkResponse
}

typealias ProgressListener = suspend (bytesReceived: DataSize, contentLength: DataSize?) -> Unit
