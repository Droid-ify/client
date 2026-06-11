package com.looker.droidify.installer.installers.dhizuku

import android.content.Context
import android.util.Log
import com.looker.droidify.data.model.PackageName
import com.looker.droidify.installer.installers.Installer
import com.looker.droidify.installer.model.InstallItem
import com.looker.droidify.installer.model.InstallState
import com.looker.droidify.utility.common.cache.Cache
import com.looker.droidify.utility.common.extension.getPackageInfoCompat
import com.looker.droidify.utility.common.extension.size
import kotlinx.coroutines.delay

class DhizukuInstaller(private val context: Context) : Installer {

    private val installManager = DhizukuInstallManager(context)

    override suspend fun install(installItem: InstallItem): InstallState {
        val file = Cache.getReleaseFile(context, installItem.installFileName)
        if (file.length() == 0L) {
            error("File is not valid: Size ${file.size}")
        }
        if (!ensureDhizukuInstallerReady(context)) {
            Log.e(TAG, "Dhizuku not ready for ${installItem.packageName.name}")
            return InstallState.Failed
        }
        return try {
            installManager.installApk(file.absolutePath)
            awaitPackageVisible(installItem.packageName.name)
            InstallState.Installed
        } catch (e: Exception) {
            Log.e(TAG, "Dhizuku install failed: ${installItem.packageName.name}", e)
            InstallState.Failed
        }
    }

    private suspend fun awaitPackageVisible(packageName: String) {
        repeat(PACKAGE_VISIBLE_ATTEMPTS) {
            if (context.packageManager.getPackageInfoCompat(packageName) != null) return
            delay(PACKAGE_VISIBLE_DELAY_MS)
        }
    }

    override suspend fun uninstall(packageName: PackageName) {
        installManager.uninstallPackage(packageName.name)
    }

    override fun close() = Unit

    companion object {
        private const val TAG = "DhizukuInstaller"
        private const val PACKAGE_VISIBLE_ATTEMPTS = 20
        private const val PACKAGE_VISIBLE_DELAY_MS = 250L
    }
}
