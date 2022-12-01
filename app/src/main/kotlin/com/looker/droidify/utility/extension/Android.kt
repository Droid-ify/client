@file:Suppress("PackageDirectoryMismatch")

package com.looker.droidify.utility.extension.android

import android.content.pm.PackageInfo
import android.content.pm.Signature
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.os.Build
import com.looker.core.common.Util

fun Cursor.asSequence(): Sequence<Cursor> {
	return generateSequence { if (moveToNext()) this else null }
}

fun Cursor.firstOrNull(): Cursor? {
	return if (moveToFirst()) this else null
}

fun SQLiteDatabase.execWithResult(sql: String) {
	rawQuery(sql, null).use { it.count }
}

@Suppress("DEPRECATION")
val PackageInfo.versionCodeCompat: Long
	get() = if (Util.isPie) longVersionCode else versionCode.toLong()

val PackageInfo.singleSignature: Signature?
	get() = if (Util.isPie) {
		val signingInfo = signingInfo
		if (signingInfo?.hasMultipleSigners() == false) signingInfo.apkContentsSigners
			?.let { if (it.size == 1) it[0] else null }
		else null
	} else {
		@Suppress("DEPRECATION")
		signatures?.let { if (it.size == 1) it[0] else null }
	}

object Android {
	val sdk: Int
		get() = Build.VERSION.SDK_INT

	val name: String
		get() = "Android ${Build.VERSION.RELEASE}"

	val platforms = Build.SUPPORTED_ABIS.toSet()

	val primaryPlatform: String?
		get() = Build.SUPPORTED_64_BIT_ABIS?.firstOrNull()
			?: Build.SUPPORTED_32_BIT_ABIS?.firstOrNull()

	object PackageManager {
		// GET_SIGNATURES should always present for getPackageArchiveInfo
		@Suppress("DEPRECATION")
		val signaturesFlag: Int
			get() = (if (Util.isPie) android.content.pm.PackageManager.GET_SIGNING_CERTIFICATES
			else 0) or android.content.pm.PackageManager.GET_SIGNATURES
	}

	object Device {
		val isHuaweiEmui: Boolean
			get() {
				return try {
					Class.forName("com.huawei.android.os.BuildEx")
					true
				} catch (e: Exception) {
					false
				}
			}
	}
}
