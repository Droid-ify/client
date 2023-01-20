package com.looker.installer.installer

import android.content.Context
import android.net.Uri
import androidx.core.net.toFile
import androidx.core.net.toUri
import com.looker.core.common.SdkCheck
import com.looker.installer.utils.BaseInstaller
import com.topjohnwu.superuser.Shell
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

internal class RootInstaller(context: Context) : BaseInstaller(context) {

	override suspend fun install(packageName: String, uri: Uri, file: File) {
		mRootInstaller(file.toUri())
	}

	private suspend fun mRootInstaller(cacheFile: Uri) {
		withContext(Dispatchers.IO) {
			val file = cacheFile.toFile()
			Shell.cmd(file.install)
				.submit { if (it.isSuccess) Shell.cmd(file.deletePackage).submit() }
		}
	}

	companion object {
		private val getCurrentUserState: String =
			if (SdkCheck.isOreo) Shell.cmd("am get-current-user").exec().out[0]
			else Shell.cmd("dumpsys activity | grep -E \"mUserLru\"")
				.exec().out[0].trim()
				.removePrefix("mUserLru: [").removeSuffix("]")

		private val String.quote
			get() = "\"${this.replace(Regex("""[\\$"`]""")) { c -> "\\${c.value}" }}\""

		private val getUtilBoxPath: String
			get() {
				listOf("toybox", "busybox").forEach {
					val shellResult = Shell.cmd("which $it").exec()
					if (shellResult.out.isNotEmpty()) {
						val utilBoxPath = shellResult.out.joinToString("")
						if (utilBoxPath.isNotEmpty()) return utilBoxPath.quote
					}
				}
				return ""
			}

		internal val File.install
			get() = String.format(
				ROOT_INSTALL_PACKAGE,
				absolutePath,
				getCurrentUserState,
				length()
			)

		internal val String.uninstall
			get() = String.format(
				ROOT_UNINSTALL_PACKAGE,
				getCurrentUserState,
				this
			)

		internal val File.deletePackage
			get() = String.format(
				DELETE_PACKAGE,
				getUtilBoxPath,
				absolutePath.quote
			)
	}
}