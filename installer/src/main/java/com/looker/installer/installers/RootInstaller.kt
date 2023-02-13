package com.looker.installer.installers

import android.content.Context
import com.looker.core.common.SdkCheck
import com.looker.core.common.cache.Cache
import com.looker.core.model.newer.PackageName
import com.looker.installer.model.InstallItem
import com.looker.installer.model.InstallState
import com.topjohnwu.superuser.Shell
import kotlinx.coroutines.suspendCancellableCoroutine
import java.io.File
import kotlin.coroutines.resume

internal class RootInstaller(private val context: Context) : BaseInstaller {

	companion object {
		private const val ROOT_INSTALL_PACKAGE = "cat %s | pm install --user %s -t -r -S %s"
		private const val DELETE_PACKAGE = "%s rm %s"

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

		private val File.install
			get() = String.format(
				ROOT_INSTALL_PACKAGE,
				absolutePath,
				getCurrentUserState,
				length()
			)

		private val File.deletePackage
			get() = String.format(
				DELETE_PACKAGE,
				getUtilBoxPath,
				absolutePath.quote
			)
	}

	override suspend fun performInstall(
		installItem: InstallItem
	): InstallState = suspendCancellableCoroutine { cont ->
		val releaseFile = Cache.getReleaseFile(context, installItem.installFileName)
		val shellResult = Shell.cmd(releaseFile.install).exec()

		if (shellResult.isSuccess) {
			cont.resume(InstallState.Installed)
			Shell.cmd(releaseFile.deletePackage).submit()
		}
	}

	override suspend fun performUninstall(packageName: PackageName) =
		context.uninstallPackage(packageName)

	override fun cleanup() {}
}