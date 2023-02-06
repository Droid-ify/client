package com.looker.droidify.utility.extension.app_file

import android.content.Context
import com.looker.core.datastore.model.InstallerType
import kotlinx.coroutines.coroutineScope

suspend fun installApk(
	packageName: String,
	context: Context?,
	fileName: String,
	installerType: InstallerType
) = coroutineScope {
	// TODO: New Installer
}

suspend fun uninstallApk(
	packageName: String,
	context: Context?,
	installerType: InstallerType
) = coroutineScope {
	// TODO: New Installer
}