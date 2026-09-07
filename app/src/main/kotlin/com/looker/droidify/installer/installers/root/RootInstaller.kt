package com.looker.droidify.installer.installers.root

import android.content.Context
import com.looker.droidify.data.model.PackageName
import com.looker.droidify.installer.installers.Installer
import com.looker.droidify.installer.installers.uninstallPackage
import com.looker.droidify.installer.model.InstallItem
import com.looker.droidify.installer.model.InstallState
import com.looker.droidify.utility.common.SdkCheck
import com.looker.droidify.utility.common.cache.Cache
import com.topjohnwu.superuser.Shell
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

class RootInstaller(private val context: Context) : Installer {

    override suspend fun install(
        installItem: InstallItem,
    ): InstallState = suspendCancellableCoroutine { cont ->
        val releaseFile = Cache.getReleaseFile(context, installItem.installFileName)
        val installCommand = INSTALL_COMMAND.format(
            releaseFile.absolutePath,
            currentUser(),
            context.packageName,
            releaseFile.length(),
        )
        Shell.cmd(installCommand).submit { shellResult ->
            val result = if (shellResult.isSuccess) {
                InstallState.Installed
            } else {
                InstallState.Failed
            }
            cont.resume(result)
        }
    }

    override suspend fun uninstall(packageName: PackageName) =
        context.uninstallPackage(packageName)
}

private const val INSTALL_COMMAND = "cat %s | pm install --user %s -i %s -t -r -S %s"

/** Returns the current user of the device. */
private fun currentUser() = if (SdkCheck.isOreo) {
    Shell.cmd("am get-current-user")
        .exec()
        .out[0]
} else {
    Shell.cmd("dumpsys activity | grep -E \"mUserLru\"")
        .exec()
        .out[0]
        .trim()
        .removePrefix("mUserLru: [")
        .removeSuffix("]")
}
