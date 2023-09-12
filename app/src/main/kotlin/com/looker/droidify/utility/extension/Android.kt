@file:Suppress("PackageDirectoryMismatch")

package com.looker.droidify.utility.extension.android

import android.os.Build

object Android {
	val name: String = "Android ${Build.VERSION.RELEASE}"

	val platforms = Build.SUPPORTED_ABIS.toSet()

	val primaryPlatform: String? = Build.SUPPORTED_64_BIT_ABIS?.firstOrNull()
		?: Build.SUPPORTED_32_BIT_ABIS?.firstOrNull()
}
