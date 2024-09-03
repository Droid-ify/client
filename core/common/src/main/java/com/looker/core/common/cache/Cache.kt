package com.looker.core.common.cache

import android.content.ContentProvider
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri
import android.os.Build
import android.os.ParcelFileDescriptor
import android.provider.OpenableColumns
import android.system.Os
import com.looker.core.common.SdkCheck
import com.looker.core.common.sdkAbove
import java.io.File
import java.util.UUID
import kotlin.concurrent.thread
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours

object Cache {

    private const val RELEASE_DIR = "releases"
    private const val PARTIAL_DIR = "partial"
    private const val IMAGES_DIR = "images"
    private const val INDEX_DIR = "index"
    private const val TEMP_DIR = "temporary"

    private fun ensureCacheDir(context: Context, name: String): File {
        return File(
            context.cacheDir,
            name
        ).apply { isDirectory || mkdirs() || throw RuntimeException() }
    }

    private fun applyOrMode(file: File, mode: Int) {
        val oldMode = Os.stat(file.path).st_mode and 0b111111111111
        val newMode = oldMode or mode
        if (newMode != oldMode) {
            Os.chmod(file.path, newMode)
        }
    }

    private fun subPath(dir: File, file: File): String {
        val dirPath = "${dir.path}/"
        val filePath = file.path
        filePath.startsWith(dirPath) || throw RuntimeException()
        return filePath.substring(dirPath.length)
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

    fun getReleaseUri(context: Context, cacheFileName: String): Uri {
        val file = getReleaseFile(context, cacheFileName)
        val packageInfo =
            try {
                if (SdkCheck.isTiramisu) {
                    context.packageManager.getPackageInfo(
                        context.packageName,
                        PackageManager.PackageInfoFlags.of(PackageManager.GET_PROVIDERS.toLong())
                    )
                } else {
                    @Suppress("DEPRECATION")
                    context.packageManager.getPackageInfo(
                        context.packageName,
                        PackageManager.GET_PROVIDERS
                    )
                }
            } catch (e: Exception) {
                null
            }
        val authority =
            packageInfo?.providers?.find { it.name == Provider::class.java.name }!!.authority
        return Uri.Builder()
            .scheme("content")
            .authority(authority)
            .encodedPath(file.path.drop(context.cacheDir.path.length))
            .build()
    }

    fun getTemporaryFile(context: Context): File {
        return File(ensureCacheDir(context, TEMP_DIR), UUID.randomUUID().toString())
    }

    fun cleanup(context: Context) {
        thread {
            cleanup(
                context,
                Pair(IMAGES_DIR, Duration.INFINITE),
                Pair(INDEX_DIR, Duration.INFINITE),
                Pair(PARTIAL_DIR, 24.hours),
                Pair(RELEASE_DIR, 24.hours),
                Pair(TEMP_DIR, 1.hours),
            )
        }
    }

    private fun cleanup(context: Context, vararg dirHours: Pair<String, Duration>) {
        val knownNames = dirHours.asSequence().map { it.first }.toSet()
        val files = context.cacheDir.listFiles().orEmpty()
        files.asSequence().filter { it.name !in knownNames }.forEach {
            if (it.isDirectory) {
                cleanupDir(it, Duration.ZERO)
                it.delete()
            } else {
                it.delete()
            }
        }
        dirHours.forEach { (name, duration) ->
            val file = File(context.cacheDir, name)
            if (file.exists()) {
                if (file.isDirectory) {
                    cleanupDir(file, duration)
                } else {
                    file.delete()
                }
            }
        }
    }

    private fun cleanupDir(dir: File, duration: Duration) {
        dir.listFiles()?.forEach {
            val older = duration <= Duration.ZERO || run {
                val olderThan = System.currentTimeMillis() / 1000L - duration.inWholeSeconds
                try {
                    val stat = Os.lstat(it.path)
                    stat.st_atime < olderThan
                } catch (e: Exception) {
                    false
                }
            }
            if (older) {
                if (it.isDirectory) {
                    cleanupDir(it, duration)
                    if (it.isDirectory) {
                        it.delete()
                    }
                } else {
                    it.delete()
                }
            }
        }
    }

    class Provider : ContentProvider() {
        companion object {
            private val defaultColumns = arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE)
        }

        private fun getFileAndTypeForUri(uri: Uri): Pair<File, String> {
            return when (uri.pathSegments?.firstOrNull()) {
                RELEASE_DIR -> Pair(
                    File(context!!.cacheDir, uri.encodedPath!!),
                    "application/vnd.android.package-archive"
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
            sortOrder: String?
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
                    columns.second.toTypedArray()
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
            selectionArgs: Array<out String>?
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
