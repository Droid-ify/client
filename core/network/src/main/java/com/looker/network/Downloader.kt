package com.looker.network

import com.looker.core.common.DataSize
import com.looker.core.common.signature.FileValidator
import com.looker.network.header.HeadersBuilder
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

typealias ProgressListener = suspend (bytesReceived: DataSize, contentLength: DataSize) -> Unit
