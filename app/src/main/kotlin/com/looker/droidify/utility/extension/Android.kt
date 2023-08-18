@file:Suppress("PackageDirectoryMismatch")

package com.looker.droidify.utility.extension.android

import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build
import com.looker.core.common.SdkCheck

object Android {
	val sdk: Int
		get() = Build.VERSION.SDK_INT

	val name: String
		get() = "Android ${Build.VERSION.RELEASE}"

	val platforms = Build.SUPPORTED_ABIS.toSet()

	val primaryPlatform: String?
		get() = Build.SUPPORTED_64_BIT_ABIS?.firstOrNull()
			?: Build.SUPPORTED_32_BIT_ABIS?.firstOrNull()
}

// GET_SIGNATURES should always present for getPackageArchiveInfo
@Suppress("DEPRECATION")
private val signaturesFlagCompat: Int
	get() = (if (SdkCheck.isPie) PackageManager.GET_SIGNING_CERTIFICATES
	else 0) or PackageManager.GET_SIGNATURES

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
