package com.looker.droidify.network

import java.io.File
import java.util.concurrent.TimeUnit
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer

class OkHttpDownloaderTest {

    private lateinit var mockServer: MockWebServer
    private lateinit var client: OkHttpClient

    @BeforeTest
    fun setUp() {
        mockServer = MockWebServer()
        mockServer.start()
        client = OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .build()
    }

    @AfterTest
    fun tearDown() {
        mockServer.shutdown()
    }

    @Test
    fun `head call success`() = runTest {
        mockServer.enqueue(MockResponse().setResponseCode(200))
        val response = client.head(mockServer.url("/test").toString())
        assertIs<NetworkResponse.Success>(response)
    }

    @Test
    fun `head call if path not found`() = runTest {
        mockServer.enqueue(MockResponse().setResponseCode(404))
        val response = client.head(mockServer.url("/notfound").toString())
        assertIs<NetworkResponse.Error.Http>(response)
        assertEquals(404, response.statusCode)
    }

    @Test
    fun `save text to file success`() = runTest {
        mockServer.enqueue(MockResponse().setBody("success").setResponseCode(200))
        val file = File.createTempFile("test", "success")
        try {
            val response = client.get(
                url = mockServer.url("/download").toString(),
                target = file,
            )
            assertIs<NetworkResponse.Success>(response)
            assertEquals("success", file.readText())
        } finally {
            file.delete()
        }
    }

    @Test
    fun `save text to read-only file`() = runTest {
        mockServer.enqueue(MockResponse().setBody("success").setResponseCode(200))
        val file = File.createTempFile("test", "readonly")
        try {
            file.setReadOnly()
            val response = client.get(
                url = mockServer.url("/download").toString(),
                target = file,
            )
            assertIs<NetworkResponse.Error.IO>(response)
        } finally {
            file.setWritable(true)
            file.delete()
        }
    }

    @Test
    fun `save text to file if not modified`() = runTest {
        mockServer.enqueue(MockResponse().setResponseCode(304))
        val file = File.createTempFile("test", "notmodified")
        try {
            val response = client.get(
                url = mockServer.url("/download").toString(),
                target = file,
                block = {
                    ifModifiedSince("Wed, 21 Oct 2025 07:28:00 GMT")
                },
            )
            assertIs<NetworkResponse.Success>(response)
            assertEquals(304, response.statusCode)
        } finally {
            file.delete()
        }
    }

    @Test
    fun `save text to file with unauthorized response`() = runTest {
        mockServer.enqueue(MockResponse().setResponseCode(401))
        val file = File.createTempFile("test", "unauthorized")
        try {
            val response = client.get(
                url = mockServer.url("/download").toString(),
                target = file,
                block = {
                    authentication("iamlooker", "sneakypeaky")
                },
            )
            assertIs<NetworkResponse.Error.Http>(response)
            assertEquals(401, response.statusCode)
        } finally {
            file.delete()
        }
    }

    @Test
    fun `progress listener receives updates`() = runTest {
        val content = "a".repeat(1024)
        mockServer.enqueue(
            MockResponse()
                .setBody(content)
                .setResponseCode(200)
                .setHeader("Content-Length", content.length)
        )
        val file = File.createTempFile("test", "progress")
        var lastProgress: Long = 0
        try {
            val response = client.get(
                url = mockServer.url("/download").toString(),
                target = file,
                onProgress = { bytesReceived, _ ->
                    lastProgress = bytesReceived.value
                },
            )
            assertIs<NetworkResponse.Success>(response)
            assertEquals(content.length.toLong(), lastProgress)
        } finally {
            file.delete()
        }
    }

    @Test
    fun `head call returns etag`() = runTest {
        mockServer.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setHeader("ETag", "\"abc123\"")
        )
        val response = client.head(mockServer.url("/test").toString())
        assertIs<NetworkResponse.Success>(response)
        assertEquals("\"abc123\"", response.etag)
    }

    @Test
    fun `head call returns last modified`() = runTest {
        mockServer.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setHeader("Last-Modified", "Wed, 21 Oct 2025 07:28:00 GMT")
        )
        val response = client.head(mockServer.url("/test").toString())
        assertIs<NetworkResponse.Success>(response)
        assertTrue(response.lastModified != null)
    }

    @Test
    fun `resume download with existing file`() = runTest {
        // First part of the file (simulating partial download)
        val firstPart = "Hello, "
        val secondPart = "World!"
        val fullContent = firstPart + secondPart

        // Create a file with partial content
        val file = File.createTempFile("test", "resume")
        try {
            file.writeText(firstPart)
            val initialSize = file.length()

            // Server responds with 206 Partial Content for range request
            mockServer.enqueue(
                MockResponse()
                    .setResponseCode(206)
                    .setBody(secondPart)
                    .setHeader("Content-Range", "bytes $initialSize-${fullContent.length - 1}/${fullContent.length}")
                    .setHeader("Content-Length", secondPart.length)
            )

            val response = client.get(
                url = mockServer.url("/download").toString(),
                target = file,
            )

            // Verify the Range header was sent
            val request = mockServer.takeRequest()
            assertEquals("bytes=7-", request.getHeader("Range"))

            // Verify the response and file content
            assertIs<NetworkResponse.Success>(response)
            assertEquals(fullContent, file.readText())
        } finally {
            file.delete()
        }
    }

    @Test
    fun `resume tracks progress from existing file size`() = runTest {
        val existingContent = "existing-"
        val newContent = "new"
        val totalContent = existingContent + newContent

        val file = File.createTempFile("test", "resume-progress")
        try {
            file.writeText(existingContent)
            val initialSize = file.length()

            mockServer.enqueue(
                MockResponse()
                    .setResponseCode(206)
                    .setBody(newContent)
                    .setHeader("Content-Length", newContent.length)
            )

            val progressValues = mutableListOf<Long>()
            val response = client.get(
                url = mockServer.url("/download").toString(),
                target = file,
                onProgress = { bytesReceived, _ ->
                    progressValues.add(bytesReceived.value)
                },
            )

            assertIs<NetworkResponse.Success>(response)
            // Progress should start from initial size and end at total size
            assertTrue(progressValues.isNotEmpty())
            assertEquals(totalContent.length.toLong(), progressValues.last())
            assertEquals(totalContent, file.readText())
        } finally {
            file.delete()
        }
    }

    @Test
    fun `request DSL builds correct headers`() = runTest {
        mockServer.enqueue(MockResponse().setResponseCode(200))

        client.head(
            url = mockServer.url("/test").toString(),
            block = {
                etag("\"test-etag\"")
                ifModifiedSince("Wed, 21 Oct 2025 07:28:00 GMT")
                range(100, 200)
            },
        )

        val request = mockServer.takeRequest()
        assertEquals("\"test-etag\"", request.getHeader("If-None-Match"))
        assertEquals("Wed, 21 Oct 2025 07:28:00 GMT", request.getHeader("If-Modified-Since"))
        assertEquals("bytes=100-200", request.getHeader("Range"))
    }

    @Test
    fun `authentication header is correctly set`() = runTest {
        mockServer.enqueue(MockResponse().setResponseCode(200))

        client.head(
            url = mockServer.url("/test").toString(),
            block = {
                authentication("user", "pass")
            },
        )

        val request = mockServer.takeRequest()
        val authHeader = request.getHeader("Authorization")
        assertEquals(authHeader?.startsWith("Basic "), true)
    }
}
