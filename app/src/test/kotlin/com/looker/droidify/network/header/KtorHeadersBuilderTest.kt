package com.looker.droidify.network.header

import io.ktor.http.HeadersBuilder
import io.ktor.http.HttpHeaders
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class KtorHeadersBuilderTest {

    private fun build(block: KtorHeadersBuilder.() -> Unit): io.ktor.http.Headers {
        val builder = HeadersBuilder()
        KtorHeadersBuilder(builder).block()
        return builder.build()
    }

    @Test
    fun `blank values are not appended`() {
        val headers = build {
            ifModifiedSince("")
            etag("")
            authentication("")
        }
        assertFalse(headers.contains(HttpHeaders.IfModifiedSince))
        assertFalse(headers.contains(HttpHeaders.ETag))
        assertFalse(headers.contains(HttpHeaders.Authorization))
    }

    @Test
    fun `null values are not appended`() {
        val headers = build {
            HttpHeaders.IfModifiedSince headsWith null
            HttpHeaders.ETag headsWith null
            HttpHeaders.Authorization headsWith null
        }
        assertTrue(headers.isEmpty())
    }

    @Test
    fun `non-blank values are appended`() {
        val headers = build {
            ifModifiedSince("Thu, 02 Jul 2026 05:43:42 GMT")
            etag("\"73ff6c74615547729ecedd240ad9f01d\"")
            authentication("Basic dXNlcjpwYXNz")
        }
        assertEquals("Thu, 02 Jul 2026 05:43:42 GMT", headers[HttpHeaders.IfModifiedSince])
        assertEquals("\"73ff6c74615547729ecedd240ad9f01d\"", headers[HttpHeaders.ETag])
        assertEquals("Basic dXNlcjpwYXNz", headers[HttpHeaders.Authorization])
    }
}
