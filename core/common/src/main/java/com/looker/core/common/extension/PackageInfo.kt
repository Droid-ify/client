package com.looker.core.common.extension

import android.content.pm.PackageInfo
import android.content.pm.Signature
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

@Suppress("DEPRECATION")
val PackageInfo.versionCodeCompat: Long
	get() = if (SdkCheck.isPie) longVersionCode else versionCode.toLong()

fun Signature.calculateHash() = MessageDigest.getInstance("MD5")
	.digest(toCharsString().toByteArray())
	.hex()
