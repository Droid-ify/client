package com.looker.droidify.installer.installers.shizuku

import android.content.Context
import android.content.pm.PackageInstaller
import android.net.Uri
import android.os.Build
import com.looker.droidify.data.model.PackageName
import com.looker.droidify.installer.installers.Installer
import com.looker.droidify.installer.installers.uninstallPackage
import com.looker.droidify.installer.model.InstallItem
import com.looker.droidify.installer.model.InstallState
import com.looker.droidify.utility.common.SdkCheck
import com.looker.droidify.utility.common.cache.Cache
import com.looker.droidify.utility.common.extension.size
import dev.re7gog.shizuku_apk_installer.ShizukuWorker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.File
import java.io.InputStream
import kotlin.coroutines.resume

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
    private val adapter by lazy {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            ShizukuWorkerAdapter(appContext)
        } else {
            null
        }
    }

    companion object {
        private val SESSION_ID_REGEX = Regex("(?<=\\[).+?(?=])")
    }

    override suspend fun install(installItem: InstallItem): InstallState {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            installWithBinder(installItem)
        } else {
            installWithShell(installItem)
        }
    }

    private suspend fun installWithBinder(installItem: InstallItem): InstallState {
        return withContext(Dispatchers.IO) {
            try {
                val permissionStatus = adapter!!.checkPermission()
                if (!permissionStatus.startsWith("granted")) {
                    return@withContext InstallState.Failed
                }

                val file = Cache.getReleaseFile(appContext, installItem.installFileName)
                if (!file.exists() || file.length() == 0L) {
                    return@withContext InstallState.Failed
                }

                val result = adapter!!.installApkFile(
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

    private suspend fun installWithShell(installItem: InstallItem): InstallState {
        return suspendCancellableCoroutine { cont ->
            var sessionId: String? = null
            val file = Cache.getReleaseFile(context, installItem.installFileName)
            try {
                val fileSize = file.length()
                if (fileSize == 0L) {
                    cont.cancel()
                    error("File is not valid: Size ${file.size}")
                }
                if (cont.isCompleted) return@suspendCancellableCoroutine
                val installerPackage = context.packageName
                file.inputStream().use {
                    val createCommand =
                        if (SdkCheck.isNougat) {
                            "pm install-create --user current -i $installerPackage -S $fileSize"
                        } else {
                            "pm install-create -i $installerPackage -S $fileSize"
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
    }

    override suspend fun uninstall(packageName: PackageName) =
        context.uninstallPackage(packageName)

    override fun close() {
        adapter?.exit()
    }

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
