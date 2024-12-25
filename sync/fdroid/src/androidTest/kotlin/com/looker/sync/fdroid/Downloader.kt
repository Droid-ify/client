package com.looker.sync.fdroid

import com.looker.network.Downloader
import com.looker.network.NetworkResponse
import com.looker.network.ProgressListener
import com.looker.network.header.HeadersBuilder
import com.looker.network.validation.FileValidator
import com.looker.sync.fdroid.common.assets
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.io.File
import java.io.InputStream
import java.net.Proxy

val FakeDownloader = object : Downloader {
    override fun setProxy(proxy: Proxy) {
        TODO("Not yet implemented")
    }

    override suspend fun headCall(
        url: String,
        headers: HeadersBuilder.() -> Unit
    ): NetworkResponse {
        TODO("Not yet implemented")
    }

    override suspend fun downloadToFile(
        url: String,
        target: File,
        validator: FileValidator?,
        headers: HeadersBuilder.() -> Unit,
        block: ProgressListener?
    ): NetworkResponse {
        return if (url.endsWith("fail")) NetworkResponse.Error.Unknown(Exception("You asked for it"))
        else {
            val index = when {
                url.endsWith("index-v1.jar") -> assets("izzy_index_v1.jar")
                url.endsWith("index-v2.json") -> assets("izzy_index_v2.json")
                url.endsWith("entry.jar") -> assets("izzy_entry.jar")
                url.endsWith("/diff/1725731263000.json") -> assets("izzy_diff.json")
                // Just in case we try these in future
                url.endsWith("index-v1.json") -> assets("izzy_index_v1.json")
                url.endsWith("entry.json") -> assets("izzy_entry.json")
                else -> error("Unknown URL: $url")
            }
            index.writeTo(target)
            NetworkResponse.Success(200, null, null)
        }
    }

    suspend infix fun InputStream.writeTo(file: File) = withContext(Dispatchers.IO) {
        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
        var bytesRead = read(buffer)
        file.outputStream().use { output ->
            while (bytesRead != -1) {
                ensureActive()
                output.write(buffer, 0, bytesRead)
                bytesRead = read(buffer)
            }
            output.flush()
        }
    }
}
