package com.looker.droidify.installer.installers

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.util.AndroidRuntimeException
import androidx.core.net.toUri
import com.looker.droidify.R
import com.looker.droidify.data.model.PackageName
import com.looker.droidify.datastore.SettingsRepository
import com.looker.droidify.datastore.get
import com.looker.droidify.datastore.model.LegacyInstallerComponent
import com.looker.droidify.installer.model.InstallItem
import com.looker.droidify.installer.model.InstallState
import com.looker.droidify.utility.common.SdkCheck
import com.looker.droidify.utility.common.cache.Cache
import com.looker.droidify.utility.common.extension.intent
import kotlin.coroutines.resume
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.suspendCancellableCoroutine

@Suppress("DEPRECATION")
class LegacyInstaller(
    private val context: Context,
    private val settingsRepository: SettingsRepository
) : Installer {

    companion object {
        private const val APK_MIME = "application/vnd.android.package-archive"
    }

    override suspend fun install(
        installItem: InstallItem,
    ): InstallState {
        val installFlag = if (SdkCheck.isNougat) Intent.FLAG_GRANT_READ_URI_PERMISSION else 0
        val fileUri = if (SdkCheck.isNougat) {
            Cache.getReleaseUri(context, installItem.installFileName)
        } else {
            Cache.getReleaseFile(context, installItem.installFileName).toUri()
        }

        val comp = settingsRepository.get { legacyInstallerComponent }.firstOrNull()

        return suspendCancellableCoroutine { cont ->
            val intent = Intent(Intent.ACTION_INSTALL_PACKAGE).apply {
                setDataAndType(fileUri, APK_MIME)
                flags = installFlag
                when (comp) {
                    is LegacyInstallerComponent.Component -> {
                        component = ComponentName(comp.clazz, comp.activity)
                    }
                    else -> {
                        // For Unspecified and AlwaysChoose, don't set component
                    }
                }
            }

            val installIntent = when (comp) {
                LegacyInstallerComponent.AlwaysChoose -> Intent.createChooser(intent, context.getString(
                    R.string.select_installer))
                else -> intent
            }

            try {
                context.startActivity(installIntent)
                cont.resume(InstallState.Installed)
            } catch (e: AndroidRuntimeException) {
                installIntent.flags = installFlag or Intent.FLAG_ACTIVITY_NEW_TASK
                try {
                    context.startActivity(installIntent)
                    cont.resume(InstallState.Installed)
                } catch (e: Exception) {
                    cont.resume(InstallState.Failed)
                }
            } catch (e: Exception) {
                cont.resume(InstallState.Failed)
            }
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
