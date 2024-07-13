package com.looker.installer.installers

import android.content.Context
import android.content.Intent
import android.util.AndroidRuntimeException
import androidx.core.net.toUri
import com.looker.core.common.SdkCheck
import com.looker.core.common.cache.Cache
import com.looker.core.domain.model.PackageName
import com.looker.installer.model.InstallItem
import com.looker.installer.model.InstallState
import kotlin.coroutines.resume
import kotlinx.coroutines.suspendCancellableCoroutine

@Suppress("DEPRECATION")
internal class LegacyInstaller(private val context: Context) : Installer {

    companion object {
        private const val APK_MIME = "application/vnd.android.package-archive"
    }

    override suspend fun install(
        installItem: InstallItem
    ): InstallState = suspendCancellableCoroutine { cont ->
        val (uri, flags) = if (SdkCheck.isNougat) {
            Cache.getReleaseUri(
                context,
                installItem.installFileName
            ) to Intent.FLAG_GRANT_READ_URI_PERMISSION
        } else {
            val file = Cache.getReleaseFile(context, installItem.installFileName)
            file.toUri() to 0
        }
        try {
            context.startActivity(
                Intent(Intent.ACTION_INSTALL_PACKAGE).setDataAndType(uri, APK_MIME).setFlags(flags)
            )
            cont.resume(InstallState.Installed)
        } catch (e: AndroidRuntimeException) {
            context.startActivity(
                Intent(Intent.ACTION_INSTALL_PACKAGE).setDataAndType(uri, APK_MIME)
                    .setFlags(flags or Intent.FLAG_ACTIVITY_NEW_TASK)
            )
            cont.resume(InstallState.Installed)
        } catch (e: Exception) {
            cont.resume(InstallState.Failed)
        }
    }

    override suspend fun uninstall(packageName: PackageName) =
        context.uninstallPackage(packageName)

    override fun close() {}
}

internal suspend fun Context.uninstallPackage(packageName: PackageName) =
    suspendCancellableCoroutine { cont ->
        try {
            startActivity(
                Intent(
                    Intent.ACTION_UNINSTALL_PACKAGE,
                    "package:${packageName.name}".toUri()
                ).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
            cont.resume(Unit)
        } catch (e: Exception) {
            e.printStackTrace()
            cont.resume(Unit)
        }
    }
