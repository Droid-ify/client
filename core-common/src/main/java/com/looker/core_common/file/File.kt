package com.looker.core_common.file

import android.content.Context
import android.system.ErrnoException
import android.system.Os
import android.system.StructStat
import android.util.Log
import com.looker.core_common.cache.Cache
import com.looker.core_common.cache.Cache.IMAGES_DIR
import com.looker.core_common.cache.Cache.PARTIAL_DIR
import com.looker.core_common.cache.Cache.RELEASE_DIR
import com.looker.core_common.cache.Cache.TEMP_DIR
import java.io.File
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.milliseconds

private const val TAG = "FileExtensions"

fun Context.deletePartialFiles() {
	Cache.ensureCacheDir(this, PARTIAL_DIR).cleanOldFiles(24.hours)
}

fun Context.deleteTemporaryFiles() {
	Cache.ensureCacheDir(this, TEMP_DIR).cleanOldFiles(24.hours)
}

fun Context.deleteOldReleases() {
	Cache.ensureCacheDir(this, RELEASE_DIR).cleanOldFiles(24.hours)
}

fun Context.deleteOldIcons() {
	Cache.ensureCacheDir(this, IMAGES_DIR).cleanOldFiles(365.days)
}

fun File?.cleanOldFiles(duration: Duration) {
	if (this == null) {
		Log.d(TAG, "cleanOldFiles: No files to clear")
		return
	}
	val olderThan =
		(System.currentTimeMillis() - duration.inWholeMilliseconds).milliseconds

	if (this.isDirectory) {
		val files: Array<out File>? = listFiles()
		if (files == null) {
			Log.d(TAG, "cleanOldFiles: No more files to clear")
			return
		}
		for (file in files) {
			file.cleanOldFiles(olderThan)
		}
		deleteAndLog()
	} else {
		deleteIfOld(olderThan)
	}
}

fun File?.deleteIfOld(duration: Duration) {
	if (this == null || !this.exists()) {
		Log.d(TAG, "deleteIfOld: No files to clear")
		return
	}

	try {
		val stat: StructStat = Os.lstat(absolutePath)
		if (stat.st_atime * 1000L < duration.inWholeMilliseconds) deleteAndLog()
	} catch (e: ErrnoException) {
		Log.e(TAG, "deleteIfOld: An error occurred while deleting: ", e)
	}
}

fun File.deleteAndLog() {
	Log.d(TAG, "Delete file: $this")
	delete()
}