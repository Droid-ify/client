@file:Suppress("PackageDirectoryMismatch")

package com.looker.droidify.utility.extension.android

import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.Signature
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.os.Build

fun Cursor.asSequence(): Sequence<Cursor> {
    return generateSequence { if (moveToNext()) this else null }
}

fun Cursor.firstOrNull(): Cursor? {
    return if (moveToFirst()) this else null
}

fun SQLiteDatabase.execWithResult(sql: String) {
    rawQuery(sql, null).use { it.count }
}

val Context.notificationManager: NotificationManager
    get() = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

val PackageInfo.versionCodeCompat: Long
    get() = if (Android.sdk(28)) longVersionCode else @Suppress("DEPRECATION") versionCode.toLong()

val PackageInfo.singleSignature: Signature?
    get() {
        return if (Android.sdk(28)) {
            val signingInfo = signingInfo
            if (signingInfo?.hasMultipleSigners() == false) signingInfo.apkContentsSigners
                ?.let { if (it.size == 1) it[0] else null } else null
        } else {
            @Suppress("DEPRECATION")
            signatures?.let { if (it.size == 1) it[0] else null }
        }
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

    fun sdk(sdk: Int): Boolean {
        return Build.VERSION.SDK_INT >= sdk
    }

    object PackageManager {
        // GET_SIGNATURES should always present for getPackageArchiveInfo
        val signaturesFlag: Int
            get() = (if (sdk(28)) android.content.pm.PackageManager.GET_SIGNING_CERTIFICATES else 0) or
                    @Suppress("DEPRECATION") android.content.pm.PackageManager.GET_SIGNATURES
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
