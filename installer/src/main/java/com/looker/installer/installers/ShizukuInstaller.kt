package com.looker.installer.installers

import android.content.Context
import com.looker.core.common.SdkCheck
import com.looker.core.common.cache.Cache
import com.looker.core.model.newer.PackageName
import com.looker.installer.model.InstallItem
import com.looker.installer.model.InstallState
import kotlinx.coroutines.suspendCancellableCoroutine
import rikka.shizuku.Shizuku
import java.io.BufferedReader
import java.io.InputStream
import kotlin.coroutines.resume

@Suppress("DEPRECATION")
internal class ShizukuInstaller(private val context: Context) : Installer {

	companion object {
		private val SESSION_ID_REGEX = Regex("(?<=\\[).+?(?=])")
	}

	override suspend fun install(
		installItem: InstallItem
	): InstallState = suspendCancellableCoroutine { cont ->
		var sessionId: String? = null
		val uri = Cache.getReleaseUri(context, installItem.installFileName)
		val releaseFileLength =
			Cache.getReleaseFile(context, installItem.installFileName).length()
		val packageName = installItem.packageName.name
		try {
			val size =
				releaseFileLength.takeIf { it >= 0 } ?: run {
					cont.cancel()
					throw IllegalStateException()
				}
			if (cont.isCompleted) return@suspendCancellableCoroutine
			context.contentResolver.openInputStream(uri).use {
				val createCommand =
					if (SdkCheck.isNougat) "pm install-create --user current -i $packageName -S $size"
					else "pm install-create -i $packageName -S $size"
				val createResult = exec(createCommand)
				sessionId = SESSION_ID_REGEX.find(createResult.out)?.value
					?: run {
						cont.cancel()
						throw RuntimeException("Failed to create install session")
					}
				if (cont.isCompleted) return@suspendCancellableCoroutine

				val writeResult = exec("pm install-write -S $size $sessionId base -", it)
				if (writeResult.resultCode != 0) {
					cont.cancel()
					throw RuntimeException("Failed to write APK to session $sessionId")
				}
				if (cont.isCompleted) return@suspendCancellableCoroutine

				val commitResult = exec("pm install-commit $sessionId")
				if (commitResult.resultCode != 0) {
					cont.cancel()
					throw RuntimeException("Failed to commit install session $sessionId")
				}
				if (cont.isCompleted) return@suspendCancellableCoroutine
				cont.resume(InstallState.Installed)
			}
		} catch (e: Exception) {
			if (sessionId != null) exec("pm install-abandon $sessionId")
			cont.resume(InstallState.Failed)
		}
	}

	override suspend fun uninstall(packageName: PackageName) =
		context.uninstallPackage(packageName)

	override fun cleanup() {}

	private data class ShellResult(val resultCode: Int, val out: String)

	private fun exec(command: String, stdin: InputStream? = null): ShellResult {
		val process = Shizuku.newProcess(arrayOf("sh", "-c", command), null, null)
		if (stdin != null) {
			process.outputStream.use { stdin.copyTo(it) }
		}
		val output = process.inputStream.bufferedReader().use(BufferedReader::readText)
		val resultCode = process.waitFor()
		return ShellResult(resultCode, output)
	}
}