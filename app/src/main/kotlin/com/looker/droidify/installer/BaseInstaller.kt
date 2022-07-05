package com.looker.droidify.installer

import android.content.Context

abstract class BaseInstaller(val context: Context) : InstallationEvents {
	companion object {
		const val ROOT_INSTALL_PACKAGE = "cat %s | pm install --user %s -t -r -S %s"
		const val ROOT_UNINSTALL_PACKAGE = "pm uninstall --user %s %s"
		const val DELETE_PACKAGE = "%s rm %s"
	}
}