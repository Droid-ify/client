@file:Suppress("PackageDirectoryMismatch")

package com.looker.droidify.utility.extension.android

import android.os.Build
import com.looker.core.common.Util

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
}
