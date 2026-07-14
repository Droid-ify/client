package com.looker.droidify.network

import com.looker.droidify.network.header.authentication
import com.looker.droidify.network.header.ifModifiedSince
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import mockwebserver3.Dispatcher
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import mockwebserver3.RecordedRequest
import okhttp3.OkHttpClient
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.io.File
import java.util.concurrent.TimeUnit
import kotlin.test.assertEquals
import kotlin.test.assertIs

class OkHttpDownloaderTest {

    private val server = MockWebServer()

    private val client = OkHttpClient.Builder()
        .connectTimeout(500, TimeUnit.MILLISECONDS)
        .readTimeout(500, TimeUnit.MILLISECONDS)
        .build()

    private val downloader = OkHttpDownloader(client, Dispatchers.IO)

    @BeforeEach
    fun setup() {
        server.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse =
                when (request.url.encodedPath) {
                    "/success" -> MockResponse(code = 200, body = "success")
                    "/notfound" -> MockResponse(code = 404)
                    "/notmodified" -> MockResponse(code = 304)
                    "/authenticate" -> MockResponse(code = 401)
                    else -> TODO("Not implemented for: ${request.url.encodedPath}")
                }
        }
        server.start()
    }

    @AfterEach
    fun teardown() {
        server.close()
    }

    private fun url(path: String): String = server.url(path).toString()

    @Test
    fun `head call success`() = runTest {
        val response = downloader.headCall(url("/success"))
        assertIs<NetworkResponse.Success>(response)
    }

    @Test
    fun `head call if path not found`() = runTest {
        val response = downloader.headCall(url("/notfound"))
        assertIs<NetworkResponse.Error.Http>(response)
    }

    @Test
    fun `save text to file success`() = runTest {
        val file = File.createTempFile("test", "success")
        val response = downloader.downloadToFile(
            url("/success"),
            target = file,
        )
        assertIs<NetworkResponse.Success>(response)
        assertEquals("success", file.readText())
    }

    @Test
    fun `save text to read-only file`() = runTest {
        val file = File.createTempFile("test", "success")
        file.setReadOnly()
        val response = downloader.downloadToFile(
            url("/success"),
            target = file,
        )
        assertIs<NetworkResponse.Error.IO>(response)
    }

    @Test
    fun `save text to file with refused connection`() = runTest {
        val port = server.port
        server.close()
        val file = File.createTempFile("test", "success")
        val response = downloader.downloadToFile(
            "http://localhost:$port/success",
            target = file,
        )
        assertIs<NetworkResponse.Error.ConnectionTimeout>(response)
    }

    @Test
    fun `save text to file if not modified`() = runTest {
        val file = File.createTempFile("test", "success")
        val response = downloader.downloadToFile(
            url("/notmodified"),
            target = file,
            headers = {
                ifModifiedSince("")
            },
        )
        assertIs<NetworkResponse.Success>(response)
        assertEquals("", file.readText())
    }

    @Test
    fun `save text to file with wrong authentication`() = runTest {
        val file = File.createTempFile("test", "success")
        val response = downloader.downloadToFile(
            url("/authenticate"),
            target = file,
            headers = {
                authentication(
                    "iamlooker",
                    "sneakypeaky",
                )
            },
        )
        assertIs<NetworkResponse.Error.Http>(response)
    }
}
