package com.looker.core.common.extension

import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.content.pm.Signature
import com.looker.core.common.SdkCheck
import com.looker.core.common.hex
import java.security.MessageDigest

val PackageInfo.singleSignature: Signature?
    get() = if (SdkCheck.isPie) {
        val signingInfo = signingInfo
        if (signingInfo?.hasMultipleSigners() == false) {
            signingInfo.apkContentsSigners
                ?.let { if (it.size == 1) it[0] else null }
        } else {
            null
        }
    } else {
        @Suppress("DEPRECATION")
        signatures?.let { if (it.size == 1) it[0] else null }
    }

fun Signature.calculateHash() = MessageDigest.getInstance("MD5")
    .digest(toCharsString().toByteArray())
    .hex()

@Suppress("DEPRECATION")
val PackageInfo.versionCodeCompat: Long
    get() = if (SdkCheck.isPie) longVersionCode else versionCode.toLong()

fun PackageManager.isSystemApplication(packageName: String): Boolean = try {
    (
        (
            this.getApplicationInfoCompat(packageName)
                .flags
            ) and ApplicationInfo.FLAG_SYSTEM
        ) != 0
} catch (e: Exception) {
    false
}

fun PackageManager.getLauncherActivities(packageName: String): List<Pair<String, String>> {
    return queryIntentActivities(
        Intent(Intent.ACTION_MAIN).addCategory(
            Intent.CATEGORY_LAUNCHER
        ),
        0
    )
        .asSequence()
        .mapNotNull { resolveInfo -> resolveInfo.activityInfo }
        .filter { activityInfo -> activityInfo.packageName == packageName }
        .mapNotNull { activityInfo ->
            val label = try {
                activityInfo.loadLabel(this).toString()
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
            label?.let { labelName ->
                activityInfo.name to labelName
            }
        }
        .toList()
}

fun PackageManager.getApplicationInfoCompat(
    filePath: String
): ApplicationInfo = if (SdkCheck.isTiramisu) {
    getApplicationInfo(
        filePath,
        PackageManager.ApplicationInfoFlags.of(0L)
    )
} else {
    @Suppress("DEPRECATION")
    getApplicationInfo(filePath, 0)
}

@Suppress("DEPRECATION")
private val signaturesFlagCompat: Int
    get() = (
        if (SdkCheck.isPie) {
            PackageManager.GET_SIGNING_CERTIFICATES
        } else {
            0
        }
        ) or PackageManager.GET_SIGNATURES

fun PackageManager.getPackageInfoCompat(
    packageName: String,
    signatureFlag: Int = signaturesFlagCompat
): PackageInfo? = try {
    if (SdkCheck.isTiramisu) {
        getPackageInfo(
            packageName,
            PackageManager.PackageInfoFlags.of(signatureFlag.toLong())
        )
    } else {
        @Suppress("DEPRECATION")
        getPackageInfo(packageName, signatureFlag)
    }
} catch (e: Exception) {
    null
}

fun PackageManager.getPackageName(
    packageName: String?,
): CharSequence? {
    if (packageName == null) return null
    return try {
        getApplicationLabel(
            getApplicationInfo(
                packageName,
                PackageManager.GET_META_DATA
            )
        )
    } catch (e: PackageManager.NameNotFoundException) {
        null
    }
}

fun PackageManager.getPackageArchiveInfoCompat(
    filePath: String,
    signatureFlag: Int = signaturesFlagCompat
): PackageInfo? = try {
    if (SdkCheck.isTiramisu) {
        getPackageArchiveInfo(
            filePath,
            PackageManager.PackageInfoFlags.of(signatureFlag.toLong())
        )
    } else {
        @Suppress("DEPRECATION")
        getPackageArchiveInfo(filePath, signatureFlag)
    }
} catch (e: Exception) {
    null
}

fun PackageManager.getInstalledPackagesCompat(
    signatureFlag: Int = signaturesFlagCompat
): List<PackageInfo>? = try {
    if (SdkCheck.isTiramisu) {
        getInstalledPackages(PackageManager.PackageInfoFlags.of(signatureFlag.toLong()))
    } else {
        @Suppress("DEPRECATION")
        getInstalledPackages(signatureFlag)
    }
} catch (e: Exception) {
    null
}
