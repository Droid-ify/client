package com.looker.core.common.extension

import android.content.Intent
import android.content.pm.*
import com.looker.core.common.SdkCheck
import com.looker.core.common.hex
import java.security.MessageDigest

val PackageInfo.singleSignature: Signature?
	get() = if (SdkCheck.isPie) {
		val signingInfo = signingInfo
		if (signingInfo?.hasMultipleSigners() == false) signingInfo.apkContentsSigners
			?.let { if (it.size == 1) it[0] else null }
		else null
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
	((this.getApplicationInfoCompat(packageName)
		.flags) and ApplicationInfo.FLAG_SYSTEM) != 0
} catch (e: Exception) {
	false
}

fun PackageManager.getLauncherActivities(packageName: String): List<Pair<String, String>> {
		return queryIntentActivities(
			Intent(Intent.ACTION_MAIN).addCategory(
				Intent.CATEGORY_LAUNCHER
			), 0
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
