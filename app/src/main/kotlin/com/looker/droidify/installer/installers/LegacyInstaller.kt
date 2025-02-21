package com.looker.droidify.installer.installers

import android.content.Context
import android.content.Intent
import android.util.AndroidRuntimeException
import androidx.core.net.toUri
import com.looker.droidify.domain.model.PackageName
import com.looker.droidify.installer.model.InstallItem
import com.looker.droidify.installer.model.InstallState
import com.looker.droidify.utility.common.SdkCheck
import com.looker.droidify.utility.common.cache.Cache
import com.looker.droidify.utility.common.extension.intent
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

@Suppress("DEPRECATION")
class LegacyInstaller(private val context: Context) : Installer {

    companion object {
        private const val APK_MIME = "application/vnd.android.package-archive"
    }

    override suspend fun install(
        installItem: InstallItem,
    ): InstallState = suspendCancellableCoroutine { cont ->
        val installFlag = if (SdkCheck.isNougat) Intent.FLAG_GRANT_READ_URI_PERMISSION else 0
        val fileUri = if (SdkCheck.isNougat) {
            Cache.getReleaseUri(
                context,
                installItem.installFileName
            )
        } else {
            Cache.getReleaseFile(context, installItem.installFileName).toUri()
        }
        val installIntent = intent(Intent.ACTION_INSTALL_PACKAGE) {
            setDataAndType(fileUri, APK_MIME)
            flags = installFlag
        }
        try {
            context.startActivity(installIntent)
            cont.resume(InstallState.Installed)
        } catch (e: AndroidRuntimeException) {
            installIntent.flags = installFlag or Intent.FLAG_ACTIVITY_NEW_TASK
            context.startActivity(installIntent)
            cont.resume(InstallState.Installed)
        } catch (e: Exception) {
            cont.resume(InstallState.Failed)
        }
    }

    override suspend fun uninstall(packageName: PackageName) =
        context.uninstallPackage(packageName)

    override fun close() {}
}

suspend fun Context.uninstallPackage(packageName: PackageName) =
    suspendCancellableCoroutine { cont ->
        try {
            startActivity(
                intent(Intent.ACTION_UNINSTALL_PACKAGE) {
                    data = "package:${packageName.name}".toUri()
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
            )
            cont.resume(Unit)
        } catch (e: Exception) {
            e.printStackTrace()
            cont.resume(Unit)
        }
    }
