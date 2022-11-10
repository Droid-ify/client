@file:Suppress("PackageDirectoryMismatch")

package com.looker.droidify.utility.extension.android

import android.content.pm.PackageInfo
import android.content.pm.Signature
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.os.Build
import com.looker.core.common.sdkAbove

fun Cursor.asSequence(): Sequence<Cursor> {
	return generateSequence { if (moveToNext()) this else null }
}

fun Cursor.firstOrNull(): Cursor? {
	return if (moveToFirst()) this else null
}

fun SQLiteDatabase.execWithResult(sql: String) {
	rawQuery(sql, null).use { it.count }
}

val PackageInfo.versionCodeCompat: Long
	get() = sdkAbove(
		sdk = Build.VERSION_CODES.P,
		onSuccessful = { longVersionCode },
		orElse = { @Suppress("DEPRECATION") versionCode.toLong() }
	)

val PackageInfo.singleSignature: Signature?
	get() {
		return sdkAbove(
			sdk = Build.VERSION_CODES.P,
			onSuccessful = {
				val signingInfo = signingInfo
				if (signingInfo?.hasMultipleSigners() == false) signingInfo.apkContentsSigners
					?.let { if (it.size == 1) it[0] else null } else null
			},
			orElse = {
				@Suppress("DEPRECATION")
				signatures?.let { if (it.size == 1) it[0] else null }
			}
		)
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
		val signaturesFlag: Int
			get() = sdkAbove(
				sdk = Build.VERSION_CODES.P,
				onSuccessful = { android.content.pm.PackageManager.GET_SIGNING_CERTIFICATES },
				orElse = { 0 }
			) or @Suppress("DEPRECATION") android.content.pm.PackageManager.GET_SIGNATURES
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
