package com.looker.droidify.utility.extension.app_file

import android.content.Context
import com.looker.core_common.cache.Cache
import com.looker.droidify.content.Preferences
import com.looker.droidify.utility.extension.toIntDef
import com.looker.installer.AppInstaller
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

suspend fun String.installApk(context: Context?, fileName: String) =
	withContext(Dispatchers.IO) {
		val installerType = Preferences[Preferences.Key.InstallerType].toIntDef
		val fileUri = context?.let { Cache.getReleaseUri(it, fileName) }
			?: throw IllegalStateException()
		val file = context?.let { Cache.getReleaseFile(it, fileName) }
			?: throw IllegalStateException()
		AppInstaller.getInstance(context, installerType)?.defaultInstaller
			?.install(this@installApk, fileUri, file)
	}

suspend fun String.uninstallApk(context: Context?) = withContext(Dispatchers.IO) {
	val installerType = Preferences[Preferences.Key.InstallerType].toIntDef
	AppInstaller.getInstance(context, installerType)?.defaultInstaller
		?.uninstall(this@uninstallApk)
}