package com.looker.droidify.installer

import android.content.Context
import com.looker.droidify.content.Cache
import com.topjohnwu.superuser.Shell
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class RootInstaller(context: Context) : BaseInstaller(context) {
    override suspend fun install(cacheFileName: String) {
        val cacheFile = Cache.getReleaseFile(context, cacheFileName)
        mRootInstaller(cacheFile)
    }

    override suspend fun install(packageName: String, cacheFileName: String) {
        val cacheFile = Cache.getReleaseFile(context, cacheFileName)
        mRootInstaller(cacheFile)
    }

    override suspend fun install(packageName: String, cacheFile: File) = mRootInstaller(cacheFile)

    override suspend fun uninstall(packageName: String) = mRootUninstaller(packageName)

    private suspend fun mRootInstaller(cacheFile: File) {
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
        withContext(Dispatchers.Default) {
            Shell.su(installCommand).submit { if (it.isSuccess) Shell.su(deleteCommand).submit() }
        }
    }

    private suspend fun mRootUninstaller(packageName: String) {
        val uninstallCommand =
            String.format(ROOT_UNINSTALL_PACKAGE, getCurrentUserState, packageName)
        withContext(Dispatchers.Default) { Shell.su(uninstallCommand).submit() }
    }

    private val getCurrentUserState: String =
        Shell.su("dumpsys activity | grep -E \"mUserLru\"")
            .exec().out[0].trim()
            .removePrefix("mUserLru: [").removeSuffix("]")

    private val String.quote
        get() = "\"${this.replace(Regex("""[\\$"`]""")) { c -> "\\${c.value}" }}\""

    private val getUtilBoxPath: String
        get() {
            listOf("toybox", "busybox").forEach {
                val shellResult = Shell.su("which $it").exec()
                if (shellResult.out.isNotEmpty()) {
                    val utilBoxPath = shellResult.out.joinToString("")
                    if (utilBoxPath.isNotEmpty()) return utilBoxPath.quote
                }
            }
            return ""
        }
}