package com.looker.network

import com.looker.network.header.HeadersBuilder
import com.looker.network.validation.FileValidator
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

    companion object {
        internal const val CONNECTION_TIMEOUT = 30_000L
        internal const val SOCKET_TIMEOUT = 15_000L
    }
}

typealias ProgressListener = suspend (bytesReceived: DataSize, contentLength: DataSize) -> Unit
