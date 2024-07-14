package com.looker.network

import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.respondError
import io.ktor.client.engine.mock.respondOk
import io.ktor.client.plugins.ConnectTimeoutException
import io.ktor.client.plugins.SocketTimeoutException
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertIs

class KtorDownloaderTest {

    private val engine = MockEngine { request ->
        when (request.url.host) {
            "success.com" -> respondOk("success")
            "notfound.com" -> respondError(HttpStatusCode.NotFound)
            "connection.com" -> throw ConnectTimeoutException(request)
            "socket.com" -> throw SocketTimeoutException(request)
            "notmodified.com" -> respond("", HttpStatusCode.NotModified)
            "authenticate.com" -> respondError(HttpStatusCode.Unauthorized)

            else -> TODO("Not implemented for: ${request.url.host}")
        }
    }

    private val dispatcher = StandardTestDispatcher()

    private val downloader = KtorDownloader(engine, dispatcher)

    @Test
    fun `head call success`() = runTest(dispatcher) {
        val response = downloader.headCall("https://success.com")
        assertIs<NetworkResponse.Success>(response)
    }

    @Test
    fun `head call if path not found`() = runTest(dispatcher) {
        val response = downloader.headCall("https://notfound.com")
        assertIs<NetworkResponse.Error.Http>(response)
    }

    @Test
    fun `save text to file success`() = runTest(dispatcher) {
        val file = File.createTempFile("test", "success")
        val response = downloader.downloadToFile("https://success.com", target = file)
        assertIs<NetworkResponse.Success>(response)
        assertEquals("success", file.readText())
    }

    @Test
    fun `save text to read-only file`() = runTest(dispatcher) {
        val file = File.createTempFile("test", "success")
        file.setReadOnly()
        val response = downloader.downloadToFile("https://success.com", target = file)
        assertIs<NetworkResponse.Error.IO>(response)
    }

    @Test
    fun `save text to file with slow connection`() = runTest(dispatcher) {
        val file = File.createTempFile("test", "success")
        val response = downloader.downloadToFile("https://connection.com", target = file)
        assertIs<NetworkResponse.Error.ConnectionTimeout>(response)
    }

    @Test
    fun `save text to file with socket error`() = runTest(dispatcher) {
        val file = File.createTempFile("test", "success")
        val response = downloader.downloadToFile("https://socket.com", target = file)
        assertIs<NetworkResponse.Error.SocketTimeout>(response)
    }

    @Test
    fun `save text to file if not modifier`() = runTest(dispatcher) {
        val file = File.createTempFile("test", "success")
        val response = downloader.downloadToFile(
            "https://notmodified.com",
            target = file,
            headers = {
                ifModifiedSince("")
            }
        )
        assertIs<NetworkResponse.Success>(response)
        assertEquals("", file.readText())
    }

    @Test
    fun `save text to file with wrong authentication`() = runTest(dispatcher) {
        val file = File.createTempFile("test", "success")
        val response = downloader.downloadToFile(
            "https://authenticate.com",
            target = file,
            headers = {
                authentication("iamlooker", "sneakypeaky")
            }
        )
        assertIs<NetworkResponse.Error.Http>(response)
    }
}
