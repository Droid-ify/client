package com.looker.droidify.installer.installers.shizuku

import android.content.Context
import android.content.pm.PackageInstaller
import android.net.Uri
import com.looker.droidify.data.model.PackageName
import com.looker.droidify.installer.installers.Installer
import com.looker.droidify.installer.installers.uninstallPackage
import com.looker.droidify.installer.model.InstallItem
import com.looker.droidify.installer.model.InstallState
import com.looker.droidify.utility.common.cache.Cache
import dev.re7gog.shizuku_apk_installer.ShizukuWorker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class ShizukuWorkerAdapter(private val context: Context) {

    private val worker = ShizukuWorker(context.applicationContext)

    suspend fun checkPermission(): String = withContext(Dispatchers.IO) {
        worker.checkPermission()
    }

    suspend fun installApkFile(file: File, installerPackageName: String = ""): Int {
        val contentUri = file.toContentUri(context)
        return withContext(Dispatchers.IO) {
            worker.installAPKs(listOf(contentUri.toString()), installerPackageName)
        }
    }

    fun exit() = worker.exit()

    companion object {
        private fun File.toContentUri(context: Context): Uri {
            return androidx.core.content.FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                this
            )
        }
    }
}

class ShizukuInstaller(private val context: Context) : Installer {
    private val appContext = context.applicationContext
    private val adapter = ShizukuWorkerAdapter(appContext)

    override suspend fun install(installItem: InstallItem): InstallState {
        return withContext(Dispatchers.IO) {
            try {
                val permissionStatus = adapter.checkPermission()
                if (!permissionStatus.startsWith("granted")) {
                    return@withContext InstallState.Failed
                }

                val file = Cache.getReleaseFile(appContext, installItem.installFileName)
                if (!file.exists() || file.length() == 0L) {
                    return@withContext InstallState.Failed
                }

                val result = adapter.installApkFile(
                    file = file,
                    installerPackageName = appContext.packageName
                )

                when (result) {
                    PackageInstaller.STATUS_SUCCESS -> InstallState.Installed
                    else -> InstallState.Failed
                }
            } catch (e: Exception) {
                InstallState.Failed
            }
        }
    }

    override suspend fun uninstall(packageName: PackageName) =
        context.uninstallPackage(packageName)

    override fun close() {
        adapter.exit()
    }
}
