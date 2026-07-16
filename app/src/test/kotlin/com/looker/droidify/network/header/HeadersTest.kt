package com.looker.droidify.network.header

import okhttp3.Headers
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class HeadersTest {

    private fun build(block: Headers.Builder.() -> Unit): Headers =
        Headers.Builder().apply(block).build()

    @Test
    fun `blank values are not appended`() {
        val headers = build {
            ifModifiedSince("")
            etag("")
            authentication("")
        }
        assertNull(headers["If-Modified-Since"])
        assertNull(headers["ETag"])
        assertNull(headers["Authorization"])
    }

    @Test
    fun `null values are not appended`() {
        val headers = build {
            addIfNotBlank("If-Modified-Since", null)
            addIfNotBlank("ETag", null)
            addIfNotBlank("Authorization", null)
        }
        assertEquals(0, headers.size)
    }

    @Test
    fun `non-blank values are appended`() {
        val headers = build {
            ifModifiedSince("Thu, 02 Jul 2026 05:43:42 GMT")
            etag("\"73ff6c74615547729ecedd240ad9f01d\"")
            authentication("Basic dXNlcjpwYXNz")
        }
        assertEquals("Thu, 02 Jul 2026 05:43:42 GMT", headers["If-Modified-Since"])
        assertEquals("\"73ff6c74615547729ecedd240ad9f01d\"", headers["ETag"])
        assertEquals("Basic dXNlcjpwYXNz", headers["Authorization"])
    }

    @Test
    fun `range header formatting`() {
        assertEquals("bytes=100-", build { inRange(100) }["Range"])
        assertEquals("bytes=100-200", build { inRange(100, 200) }["Range"])
    }
}
