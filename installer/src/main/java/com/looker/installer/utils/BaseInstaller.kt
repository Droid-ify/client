package com.looker.installer.utils

import android.content.Context
import android.content.Intent
import androidx.core.net.toUri

abstract class BaseInstaller(val context: Context) : InstallationEvents {
	companion object {
		const val ROOT_INSTALL_PACKAGE = "cat %s | pm install --user %s -t -r -S %s"
		const val ROOT_UNINSTALL_PACKAGE = "pm uninstall --user %s %s"
		const val DELETE_PACKAGE = "%s rm %s"
	}

	override suspend fun uninstall(packageName: String) {
		val intent = Intent(Intent.ACTION_UNINSTALL_PACKAGE, "package:$packageName".toUri())
			.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

		context.startActivity(intent)
	}
}