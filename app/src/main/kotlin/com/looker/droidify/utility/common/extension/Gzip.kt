package com.looker.droidify.utility.common.extension

import java.io.ByteArrayOutputStream
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream

private const val GZIP_MAGIC_FIRST = 0x1f
private const val GZIP_MAGIC_SECOND = 0x8b
private const val BYTE_MASK = 0xff
private const val GZIP_HEADER_SIZE = 2

/**
 * GZIP-compresses [this].
 *
 * Large serialized JSON blobs (e.g. an app that ships hundreds of releases, like the Brave repos)
 * can exceed SQLite's ~2 MB per-row `CursorWindow` limit, which makes the whole row unreadable and
 * throws `SQLiteBlobTooBigException`. Compressing the blob before storing it keeps the row small
 * enough to be read back. JSON typically shrinks ~5-10x, so this raises the practical ceiling well
 * beyond any realistic single entry.
 */
fun ByteArray.gzip(): ByteArray {
    val output = ByteArrayOutputStream()
    GZIPOutputStream(output).use { it.write(this) }
    return output.toByteArray()
}

/**
 * Reverses [gzip]. If [this] is not GZIP data (detected via the magic header), it is returned
 * unchanged, so reads remain backward compatible with rows written before compression was
 * introduced — no database migration or re-sync is required.
 */
fun ByteArray.gunzipIfNeeded(): ByteArray {
    val isGzip = size >= GZIP_HEADER_SIZE &&
        this[0].toInt() and BYTE_MASK == GZIP_MAGIC_FIRST &&
        this[1].toInt() and BYTE_MASK == GZIP_MAGIC_SECOND
    if (!isGzip) return this
    return GZIPInputStream(inputStream()).use { it.readBytes() }
}
