package com.looker.droidify.utility.common.extension

import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class GzipTest {

    @Test
    fun `gzip then gunzip round-trips the original bytes`() {
        val original = """{"packageName":"com.example","releases":[1,2,3]}""".toByteArray()
        val restored = original.gzip().gunzipIfNeeded()
        assertArrayEquals(original, restored)
    }

    @Test
    fun `round-trips a large payload that would overflow a CursorWindow`() {
        // ~5 MB of JSON-ish data, larger than SQLite's ~2 MB per-row window.
        val original = ("""{"v":"1.0","sig":"abc"},""".repeat(220_000)).toByteArray()
        val restored = original.gzip().gunzipIfNeeded()
        assertArrayEquals(original, restored)
    }

    @Test
    fun `gunzipIfNeeded leaves uncompressed JSON untouched for backward compatibility`() {
        // Rows written before compression are plain JSON (start with '{'); they must still read.
        val legacy = """{"packageName":"com.legacy"}""".toByteArray()
        assertArrayEquals(legacy, legacy.gunzipIfNeeded())
    }

    @Test
    fun `gunzipIfNeeded leaves short or empty arrays untouched`() {
        assertArrayEquals(ByteArray(0), ByteArray(0).gunzipIfNeeded())
        val oneByte = byteArrayOf(0x1f)
        assertArrayEquals(oneByte, oneByte.gunzipIfNeeded())
    }

    @Test
    fun `gzip actually shrinks compressible data`() {
        val repetitive = "a".repeat(100_000).toByteArray()
        assertTrue(repetitive.gzip().size < repetitive.size)
    }
}
