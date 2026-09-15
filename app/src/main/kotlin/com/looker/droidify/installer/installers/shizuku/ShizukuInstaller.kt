package com.looker.droidify.installer.installers.shizuku

import android.content.Context
import android.util.Log
import com.looker.droidify.data.model.PackageName
import com.looker.droidify.installer.installers.Installer
import com.looker.droidify.installer.installers.uninstallPackage
import com.looker.droidify.installer.model.InstallItem
import com.looker.droidify.installer.model.InstallState
import com.looker.droidify.utility.common.SdkCheck
import com.looker.droidify.utility.common.cache.Cache
import java.io.BufferedReader
import java.io.InputStream
import kotlinx.coroutines.CancellationException

class ShizukuInstaller(private val context: Context) : Installer {

    companion object {
        private const val TAG = "ShizukuInstaller"
        private val SESSION_ID_REGEX = Regex("(?<=\\[).+?(?=])")
    }

    override suspend fun install(
        installItem: InstallItem,
    ): InstallState {
        val file = Cache.getReleaseFile(context, installItem.installFileName)
        val fileSize = try {
            file.length()
        } catch (e: SecurityException) {
            Log.e(TAG, "Security manager denied access to ${file.path}", e)
            return InstallState.Failed
        }
        if (fileSize == 0L) {
            Log.e(TAG, "File is not valid: ${file.path}")
            return InstallState.Failed
        }

        val installerPackage = context.packageName

        val createCommand = if (SdkCheck.isNougat) {
            "pm install-create --user current -i $installerPackage -S $fileSize"
        } else {
            "pm install-create -i $installerPackage -S $fileSize"
        }
        val createResult = exec(createCommand)

        val sessionId = SESSION_ID_REGEX.find(createResult.out)?.value
        if (sessionId == null) {
            Log.e(TAG, "Failed to create install session: ${createResult.out}")
            return InstallState.Failed
        }

        return try {
            val writeResult = file.inputStream().use { stream ->
                exec("pm install-write -S $fileSize $sessionId base -", stream)
            }
            check(writeResult.resultCode == 0) {
                "Failed to write APK to session $sessionId: ${writeResult.out}"
            }
            val commitResult = exec("pm install-commit $sessionId")
            check(commitResult.resultCode == 0) {
                "Failed to commit install session $sessionId: ${commitResult.out}"
            }
            InstallState.Installed
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Install failed for ${installItem.packageName.name}", e)
            try {
                exec("pm install-abandon $sessionId")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to abandon session $sessionId", e)
            }
            InstallState.Failed
        }
    }

    override suspend fun uninstall(packageName: PackageName) =
        context.uninstallPackage(packageName)

    private data class ShellResult(val resultCode: Int, val out: String)

    private fun exec(command: String, stdin: InputStream? = null): ShellResult {
        val process = rikka.shizuku.Shizuku.newProcess(arrayOf("sh", "-c", command), null, null)
        if (stdin != null) {
            process.outputStream.use { stdin.copyTo(it) }
        }
        val output = process.inputStream.bufferedReader().use(BufferedReader::readText)
        val resultCode = process.waitFor()
        return ShellResult(resultCode, output)
    }
}
