package com.looker.droidify.utility.extension.app_file

import android.content.Context
import com.looker.core.common.cache.Cache
import com.looker.core.datastore.model.InstallerType
import com.looker.installer.AppInstaller
import kotlinx.coroutines.coroutineScope

suspend fun installApk(
	packageName: String,
	context: Context?,
	fileName: String,
	installerType: InstallerType
) = coroutineScope {
	val fileUri = context?.let { Cache.getReleaseUri(it, fileName) }
		?: throw IllegalStateException()
	val file = Cache.getReleaseFile(context, fileName)
	AppInstaller.getInstance(context, installerType)?.defaultInstaller
		?.install(packageName, fileUri, file)
}

suspend fun uninstallApk(
	packageName: String,
	context: Context?,
	installerType: InstallerType
) = coroutineScope {
	AppInstaller.getInstance(context, installerType)?.defaultInstaller
		?.uninstall(packageName)
}