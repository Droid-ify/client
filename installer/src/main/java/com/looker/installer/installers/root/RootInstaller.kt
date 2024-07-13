package com.looker.installer.installers.root

import android.content.Context
import com.looker.core.common.SdkCheck
import com.looker.core.common.cache.Cache
import com.looker.core.domain.model.PackageName
import com.looker.installer.installers.Installer
import com.looker.installer.installers.uninstallPackage
import com.looker.installer.model.InstallItem
import com.looker.installer.model.InstallState
import com.topjohnwu.superuser.Shell
import kotlinx.coroutines.suspendCancellableCoroutine
import java.io.File
import kotlin.coroutines.resume

internal class RootInstaller(private val context: Context) : Installer {

    private companion object {
        const val ROOT_INSTALL_PACKAGE = "cat %s | pm install --user %s -t -r -S %s"
        const val DELETE_PACKAGE = "%s rm %s"

        val getCurrentUserState: String
            get() = if (SdkCheck.isOreo) {
                Shell.cmd("am get-current-user").exec().out[0]
            } else {
                Shell.cmd("dumpsys activity | grep -E \"mUserLru\"")
                    .exec().out[0].trim()
                    .removePrefix("mUserLru: [").removeSuffix("]")
            }

        val String.quote
            get() = "\"${this.replace(Regex("""[\\$"`]""")) { c -> "\\${c.value}" }}\""

        val getUtilBoxPath: String
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

        fun installCmd(file: File): String = String.format(
            ROOT_INSTALL_PACKAGE,
            file.absolutePath,
            getCurrentUserState,
            file.length()
        )

        fun deleteCmd(file: File): String = String.format(
            DELETE_PACKAGE,
            getUtilBoxPath,
            file.absolutePath.quote
        )
    }

    override suspend fun install(
        installItem: InstallItem
    ): InstallState = suspendCancellableCoroutine { cont ->
        val releaseFile = Cache.getReleaseFile(context, installItem.installFileName)
        Shell.cmd(installCmd(releaseFile)).submit { shellResult ->
            val result = if (shellResult.isSuccess) InstallState.Installed
            else InstallState.Failed
            cont.resume(result)
            Shell.cmd(deleteCmd(releaseFile)).submit()
        }
    }

    override suspend fun uninstall(packageName: PackageName) =
        context.uninstallPackage(packageName)

    override fun close() {}
}
