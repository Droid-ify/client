package com.looker.droidify.installer.installers.shizuku

import android.content.Context
import com.looker.droidify.data.model.PackageName
import com.looker.droidify.installer.installers.Installer
import com.looker.droidify.installer.installers.uninstallPackage
import com.looker.droidify.installer.model.InstallItem
import com.looker.droidify.installer.model.InstallState
import com.looker.droidify.utility.common.SdkCheck
import com.looker.droidify.utility.common.cache.Cache
import com.looker.droidify.utility.common.extension.size
import java.io.BufferedReader
import java.io.InputStream
import kotlin.coroutines.resume
import kotlinx.coroutines.suspendCancellableCoroutine

class ShizukuInstaller(private val context: Context) : Installer {

    companion object {
        private val SESSION_ID_REGEX = Regex("(?<=\\[).+?(?=])")
    }

    override suspend fun install(
        installItem: InstallItem,
    ): InstallState = suspendCancellableCoroutine { cont ->
        var sessionId: String? = null
        val file = Cache.getReleaseFile(context, installItem.installFileName)
        val packageName = installItem.packageName.name
        try {
            val fileSize = file.length()
            if (fileSize == 0L) {
                cont.cancel()
                error("File is not valid: Size ${file.size}")
            }
            if (cont.isCompleted) return@suspendCancellableCoroutine
            file.inputStream().use {
                val createCommand =
                    if (SdkCheck.isNougat) {
                        "pm install-create --user current -i $packageName -S $fileSize"
                    } else {
                        "pm install-create -i $packageName -S $fileSize"
                    }
                val createResult = exec(createCommand)
                sessionId = SESSION_ID_REGEX.find(createResult.out)?.value
                    ?: run {
                        cont.cancel()
                        error("Failed to create install session")
                    }
                if (cont.isCompleted) return@suspendCancellableCoroutine

                val writeResult = exec("pm install-write -S $fileSize $sessionId base -", it)
                if (writeResult.resultCode != 0) {
                    cont.cancel()
                    error("Failed to write APK to session $sessionId")
                }
                if (cont.isCompleted) return@suspendCancellableCoroutine

                val commitResult = exec("pm install-commit $sessionId")
                if (commitResult.resultCode != 0) {
                    cont.cancel()
                    error("Failed to commit install session $sessionId")
                }
                if (cont.isCompleted) return@suspendCancellableCoroutine
                cont.resume(InstallState.Installed)
            }
        } catch (_: Exception) {
            if (sessionId != null) exec("pm install-abandon $sessionId")
            cont.resume(InstallState.Failed)
        }
    }

    override suspend fun uninstall(packageName: PackageName) =
        context.uninstallPackage(packageName)

    override fun close() = Unit

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
