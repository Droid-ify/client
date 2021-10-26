package com.looker.droidify.installer

import android.content.Context
import android.util.Log
import com.looker.droidify.content.Cache
import com.looker.droidify.utility.Utils.rootInstallerEnabled
import com.looker.droidify.utility.extension.android.Android
import com.topjohnwu.superuser.Shell
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class RootInstaller(context: Context) : BaseInstaller(context) {

    override fun install(packageName: String, cacheFileName: String) {
        val cacheFile = Cache.getReleaseFile(context, cacheFileName)
        scope.launch { mRootInstaller(cacheFile) }
    }

    override fun install(packageName: String, cacheFile: File) {
        scope.launch { mRootInstaller(cacheFile) }
    }

    override fun uninstall(packageName: String) {
        scope.launch { mRootUninstaller(packageName) }
    }

    private fun mRootInstaller(cacheFile: File) {
        if (rootInstallerEnabled) {
            val installCommand =
                String.format(
                    ROOT_INSTALL_PACKAGE,
                    cacheFile.absolutePath,
                    getCurrentUserState,
                    cacheFile.length()
                )
            val deleteCommand =
                String.format(
                    DELETE_PACKAGE,
                    getUtilBoxPath,
                    cacheFile.absolutePath.quote
                )
            scope.launch {
                Shell.su(installCommand).submit {
                    if (it.isSuccess) {
                        Shell.su(deleteCommand).submit()
                    }
                }
            }
        }
    }

    private suspend fun mRootUninstaller(packageName: String) {
        if (rootInstallerEnabled) {
            val uninstallCommand =
                String.format(ROOT_UNINSTALL_PACKAGE, getCurrentUserState, packageName)
            withContext(Dispatchers.IO) { launch { Shell.su(uninstallCommand).exec() } }
        }
    }

    private val getCurrentUserState: String =
        if (Android.sdk(25)) Shell.su("am get-current-user").exec().out[0]
        else Shell.su("dumpsys activity | grep mCurrentUser").exec().out[0].trim()
            .removePrefix("mCurrentUser=")

    private val String.quote
        get() = "\"${this.replace(Regex("""[\\$"`]""")) { c -> "\\${c.value}" }}\""

    private val getUtilBoxPath: String
        get() {
            listOf("toybox", "busybox").forEach {
                var shellResult = Shell.su("which $it").exec()
                if (shellResult.out.isNotEmpty()) {
                    val utilBoxPath = shellResult.out.joinToString("")
                    if (utilBoxPath.isNotEmpty()) {
                        val utilBoxQuoted = utilBoxPath.quote
                        shellResult = Shell.su("$utilBoxQuoted --version").exec()
                        if (shellResult.out.isNotEmpty()) {
                            val utilBoxVersion = shellResult.out.joinToString("")
                            Log.i(
                                this.javaClass.canonicalName,
                                "Using Utilbox $it : $utilBoxQuoted $utilBoxVersion"
                            )
                        }
                        return utilBoxQuoted
                    }
                }
            }
            return ""
        }
}