package com.looker.droidify.installer.installers

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.net.toUri
import com.looker.droidify.R
import com.looker.droidify.data.model.PackageName
import com.looker.droidify.datastore.model.LegacyInstallerComponent
import com.looker.droidify.installer.model.InstallItem
import com.looker.droidify.installer.model.InstallState
import com.looker.droidify.utility.common.SdkCheck
import com.looker.droidify.utility.common.cache.Cache
import com.looker.droidify.utility.common.extension.contentUri
import com.looker.droidify.utility.common.extension.intent

@Suppress("DEPRECATION")
class LegacyInstaller(
    private val context: Context,
    private val comp: LegacyInstallerComponent?,
) : Installer {

    companion object {
        private const val TAG = "LegacyInstaller"
        private const val APK_MIME = "application/vnd.android.package-archive"
    }

    override suspend fun install(
        installItem: InstallItem,
    ): InstallState {
        val installFlags = Intent.FLAG_ACTIVITY_NEW_TASK or
            if (SdkCheck.isNougat) Intent.FLAG_GRANT_READ_URI_PERMISSION else 0
        val file = Cache.getReleaseFile(context, installItem.installFileName)
        val fileUri = if (SdkCheck.isNougat) file.contentUri(context) else file.toUri()

        val installIntent = intent(Intent.ACTION_INSTALL_PACKAGE) {
            setDataAndType(fileUri, APK_MIME)
            flags = installFlags
            if (comp is LegacyInstallerComponent.Component) {
                component = ComponentName(comp.clazz, comp.activity)
            }
        }
        val intent = if (comp is LegacyInstallerComponent.AlwaysChoose) {
            Intent.createChooser(installIntent, context.getString(R.string.select_installer))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        } else {
            installIntent
        }
        return try {
            context.startActivity(intent)
            InstallState.Installed
        } catch (e: Exception) {
            Log.e(TAG, "Failed to launch installer for ${installItem.packageName.name}", e)
            InstallState.Failed
        }
    }

    override suspend fun uninstall(packageName: PackageName) =
        context.uninstallPackage(packageName)
}

@Suppress("DEPRECATION")
fun Context.uninstallPackage(packageName: PackageName) {
    try {
        startActivity(
            intent(Intent.ACTION_UNINSTALL_PACKAGE) {
                data = "package:${packageName.name}".toUri()
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            },
        )
    } catch (e: Exception) {
        e.printStackTrace()
    }
}
