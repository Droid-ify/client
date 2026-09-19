package com.looker.droidify.utility.common.cache

import android.content.ContentProvider
import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri
import android.os.Build
import android.os.ParcelFileDescriptor
import android.provider.OpenableColumns
import android.system.Os
import com.looker.droidify.BuildConfig
import com.looker.droidify.utility.common.sdkAbove
import java.io.File
import java.util.*
import kotlin.math.min
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object Cache {

    private const val RELEASE_DIR = "releases"
    private const val PARTIAL_DIR = "partial"
    private const val IMAGES_DIR = "images"
    private const val INDEX_DIR = "index"
    private const val TEMP_DIR = "temporary"

    private fun ensureCacheDir(context: Context, name: String): File {
        return File(
            context.cacheDir,
            name,
        ).apply { isDirectory || mkdirs() || throw RuntimeException() }
    }

    private fun applyOrMode(file: File, mode: Int) {
        // Only take first 12 significant digits
        val oldMode = Os.stat(file.path).st_mode and 0b111111111111
        // Merge old permission and new permission
        val newMode = oldMode or mode
        if (newMode != oldMode) {
            Os.chmod(file.path, newMode)
        }
    }

    fun getEmptySpace(context: Context): Long {
        val dir = context.cacheDir
        return min(dir.usableSpace, dir.freeSpace)
    }

    fun getImagesDir(context: Context): File {
        return ensureCacheDir(context, IMAGES_DIR)
    }

    fun getIndexFile(context: Context, indexName: String): File {
        return File(ensureCacheDir(context, INDEX_DIR), indexName)
    }

    fun getPartialReleaseFile(context: Context, cacheFileName: String): File {
        return File(ensureCacheDir(context, PARTIAL_DIR), cacheFileName)
    }

    fun getReleaseFile(context: Context, cacheFileName: String): File {
        return File(ensureCacheDir(context, RELEASE_DIR), cacheFileName).apply {
            sdkAbove(Build.VERSION_CODES.N) {
                // Make readable for package installer
                val cacheDir = context.cacheDir.parentFile!!.parentFile!!
                generateSequence(this) { it.parentFile!! }.takeWhile { it != cacheDir }.forEach {
                    when {
                        it.isDirectory -> applyOrMode(it, 0b001001001)
                        it.isFile -> applyOrMode(it, 0b100100100)
                    }
                }
            }
        }
    }

    fun getTemporaryFile(context: Context): File {
        return File(ensureCacheDir(context, TEMP_DIR), UUID.randomUUID().toString())
    }

    private val DirectoryExpiration = mapOf(
        IMAGES_DIR to 7.days,
        PARTIAL_DIR to 1.days,
        RELEASE_DIR to 1.days,
        TEMP_DIR to 1.hours,
    )

    suspend fun cleanup(
        context: Context,
        dirHours: Map<String, Duration> = DirectoryExpiration,
    ) = withContext(Dispatchers.IO) {
        context.cacheDir
            .listFiles { it.name !in dirHours.keys }
            ?.forEach(File::deleteRecursively)

        dirHours.forEach { (name, duration) ->
            val dir = File(context.cacheDir, name)
            if (dir.exists()) {
                require(dir.isDirectory) { "Not a directory: ${dir.path}" }
                dir.deleteOlderThan(duration)
            }
        }
    }

    private fun File.deleteOlderThan(duration: Duration) {
        val olderThan = System.currentTimeMillis() - duration.inWholeMilliseconds
        listFiles()?.forEach { file ->
            if (file.isDirectory) {
                file.deleteOlderThan(duration)
                file.delete()
            } else {
                val lastModified = file.lastModified()
                if (lastModified != 0L && lastModified < olderThan) file.delete()
            }
        }
    }

    class Provider : ContentProvider() {
        companion object {
            const val AUTHORITY = "${BuildConfig.APPLICATION_ID}.provider.cache"
            private val defaultColumns = arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE)
        }

        private fun getFileAndTypeForUri(uri: Uri): Pair<File, String> {
            return when (uri.pathSegments?.firstOrNull()) {
                RELEASE_DIR -> Pair(
                    File(context!!.cacheDir, uri.encodedPath!!),
                    "application/vnd.android.package-archive",
                )

                else -> throw SecurityException()
            }
        }

        override fun onCreate(): Boolean = true

        override fun query(
            uri: Uri,
            projection: Array<String>?,
            selection: String?,
            selectionArgs: Array<out String>?,
            sortOrder: String?,
        ): Cursor {
            val file = getFileAndTypeForUri(uri).first
            val columns = (projection ?: defaultColumns).mapNotNull {
                when (it) {
                    OpenableColumns.DISPLAY_NAME -> Pair(it, file.name)
                    OpenableColumns.SIZE -> Pair(it, file.length())
                    else -> null
                }
            }.unzip()
            return MatrixCursor(columns.first.toTypedArray()).apply {
                addRow(
                    columns.second.toTypedArray(),
                )
            }
        }

        override fun getType(uri: Uri): String = getFileAndTypeForUri(uri).second

        private val unsupported: Nothing
            get() = throw UnsupportedOperationException()

        override fun insert(uri: Uri, contentValues: ContentValues?): Uri = unsupported
        override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int =
            unsupported

        override fun update(
            uri: Uri,
            contentValues: ContentValues?,
            selection: String?,
            selectionArgs: Array<out String>?,
        ): Int = unsupported

        override fun openFile(uri: Uri, mode: String): ParcelFileDescriptor? {
            val openMode = when (mode) {
                "r" -> ParcelFileDescriptor.MODE_READ_ONLY
                "w", "wt" ->
                    ParcelFileDescriptor.MODE_WRITE_ONLY or ParcelFileDescriptor.MODE_CREATE or
                        ParcelFileDescriptor.MODE_TRUNCATE

                "wa" ->
                    ParcelFileDescriptor.MODE_WRITE_ONLY or ParcelFileDescriptor.MODE_CREATE or
                        ParcelFileDescriptor.MODE_APPEND

                "rw" -> ParcelFileDescriptor.MODE_READ_WRITE or ParcelFileDescriptor.MODE_CREATE
                "rwt" ->
                    ParcelFileDescriptor.MODE_READ_WRITE or ParcelFileDescriptor.MODE_CREATE or
                        ParcelFileDescriptor.MODE_TRUNCATE

                else -> throw IllegalArgumentException()
            }
            val file = getFileAndTypeForUri(uri).first
            return ParcelFileDescriptor.open(file, openMode)
        }
    }
}
