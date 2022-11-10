package com.looker.droidify.utility.extension.app_file

import android.content.Context
import com.looker.core.common.cache.Cache
import com.looker.core.datastore.model.InstallerType
import com.looker.installer.AppInstaller
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

suspend fun String.installApk(context: Context?, fileName: String, installerType: InstallerType) =
	withContext(Dispatchers.IO) {
		val fileUri = context?.let { Cache.getReleaseUri(it, fileName) }
			?: throw IllegalStateException()
		val file = Cache.getReleaseFile(context, fileName)
		AppInstaller.getInstance(context, installerType)?.defaultInstaller
			?.install(this@installApk, fileUri, file)
	}

suspend fun String.uninstallApk(context: Context?, installerType: InstallerType) =
	withContext(Dispatchers.IO) {
		AppInstaller.getInstance(context, installerType)?.defaultInstaller
			?.uninstall(this@uninstallApk)
	}