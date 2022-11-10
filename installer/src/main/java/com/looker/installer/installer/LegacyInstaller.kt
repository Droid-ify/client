package com.looker.installer.installer

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import com.looker.core.common.sdkAbove
import com.looker.installer.utils.BaseInstaller
import java.io.File

internal class LegacyInstaller(context: Context) : BaseInstaller(context) {

	companion object {
		const val APK_MIME = "application/vnd.android.package-archive"
	}

	override suspend fun install(packageName: String, uri: Uri, file: File) {
		val flags = sdkAbove(
			Build.VERSION_CODES.N,
			orElse = { 0 },
			onSuccessful = { Intent.FLAG_GRANT_READ_URI_PERMISSION })
		context.startActivity(
			Intent(Intent.ACTION_INSTALL_PACKAGE).setDataAndType(uri, APK_MIME).setFlags(flags)
		)
	}
}