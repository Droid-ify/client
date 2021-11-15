package com.looker.droidify.content

import android.content.ContentProvider
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.provider.OpenableColumns
import android.system.Os
import com.looker.droidify.utility.extension.android.Android
import java.io.File
import java.util.*
import kotlin.concurrent.thread

object Cache {
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
        return ensureCacheDir(context, "images")
    }

    fun getPartialReleaseFile(context: Context, cacheFileName: String): File {
        return File(ensureCacheDir(context, "partial"), cacheFileName)
    }

    fun getReleaseFile(context: Context, cacheFileName: String): File {
        return File(ensureCacheDir(context, "releases"), cacheFileName).apply {
            if (!Android.sdk(24)) {
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
            context.packageManager.getPackageInfo(context.packageName, PackageManager.GET_PROVIDERS)
        val authority =
            packageInfo.providers.find { it.name == Provider::class.java.name }!!.authority
        return Uri.Builder().scheme("content").authority(authority)
            .encodedPath(subPath(context.cacheDir, file)).build()
    }

    fun getTemporaryFile(context: Context): File {
        return File(ensureCacheDir(context, "temporary"), UUID.randomUUID().toString())
    }

    fun cleanup(context: Context) {
        thread {
            cleanup(
                context,
                Pair("images", 0),
                Pair("partial", 24),
                Pair("releases", 24),
                Pair("temporary", 1)
            )
        }
    }

    private fun cleanup(context: Context, vararg dirHours: Pair<String, Int>) {
        val knownNames = dirHours.asSequence().map { it.first }.toSet()
        val files = context.cacheDir.listFiles().orEmpty()
        files.asSequence().filter { it.name !in knownNames }.forEach {
            if (it.isDirectory) {
                cleanupDir(it, 0)
                it.delete()
            } else {
                it.delete()
            }
        }
        dirHours.forEach { (name, hours) ->
            if (hours > 0) {
                val file = File(context.cacheDir, name)
                if (file.exists()) {
                    if (file.isDirectory) {
                        cleanupDir(file, hours)
                    } else {
                        file.delete()
                    }
                }
            }
        }
    }

    private fun cleanupDir(dir: File, hours: Int) {
        dir.listFiles()?.forEach {
            val older = hours <= 0 || run {
                val olderThan = System.currentTimeMillis() / 1000L - hours * 60 * 60
                try {
                    val stat = Os.lstat(it.path)
                    stat.st_atime < olderThan
                } catch (e: Exception) {
                    false
                }
            }
            if (older) {
                if (it.isDirectory) {
                    cleanupDir(it, hours)
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
                "releases" -> Pair(
                    File(context!!.cacheDir, uri.encodedPath!!),
                    "application/vnd.android.package-archive"
                )
                else -> throw SecurityException()
            }
        }

        override fun onCreate(): Boolean = true

        override fun query(
            uri: Uri, projection: Array<String>?,
            selection: String?, selectionArgs: Array<out String>?, sortOrder: String?,
        ): Cursor {
            val file = getFileAndTypeForUri(uri).first
            val columns = (projection ?: defaultColumns).mapNotNull {
                when (it) {
                    OpenableColumns.DISPLAY_NAME -> Pair(it, file.name)
                    OpenableColumns.SIZE -> Pair(it, file.length())
                    else -> null
                }
            }.unzip()
            return MatrixCursor(columns.first.toTypedArray()).apply { addRow(columns.second.toTypedArray()) }
        }

        override fun getType(uri: Uri): String = getFileAndTypeForUri(uri).second

        private val unsupported: Nothing
            get() = throw UnsupportedOperationException()

        override fun insert(uri: Uri, contentValues: ContentValues?): Uri = unsupported
        override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int =
            unsupported

        override fun update(
            uri: Uri, contentValues: ContentValues?,
            selection: String?, selectionArgs: Array<out String>?,
        ): Int = unsupported

        override fun openFile(uri: Uri, mode: String): ParcelFileDescriptor? {
            val openMode = when (mode) {
                "r" -> ParcelFileDescriptor.MODE_READ_ONLY
                "w", "wt" -> ParcelFileDescriptor.MODE_WRITE_ONLY or ParcelFileDescriptor.MODE_CREATE or
                        ParcelFileDescriptor.MODE_TRUNCATE
                "wa" -> ParcelFileDescriptor.MODE_WRITE_ONLY or ParcelFileDescriptor.MODE_CREATE or
                        ParcelFileDescriptor.MODE_APPEND
                "rw" -> ParcelFileDescriptor.MODE_READ_WRITE or ParcelFileDescriptor.MODE_CREATE
                "rwt" -> ParcelFileDescriptor.MODE_READ_WRITE or ParcelFileDescriptor.MODE_CREATE or
                        ParcelFileDescriptor.MODE_TRUNCATE
                else -> throw IllegalArgumentException()
            }
            val file = getFileAndTypeForUri(uri).first
            return ParcelFileDescriptor.open(file, openMode)
        }
    }
}
