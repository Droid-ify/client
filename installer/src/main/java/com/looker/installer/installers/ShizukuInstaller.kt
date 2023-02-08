package com.looker.installer.installers

import android.content.Context
import com.looker.core.common.SdkCheck
import com.looker.core.common.cache.Cache
import com.looker.core.model.newer.PackageName
import com.looker.installer.model.InstallItem
import com.looker.installer.model.InstallItemState
import com.looker.installer.model.InstallState
import com.looker.installer.model.statesTo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.withContext
import rikka.shizuku.Shizuku
import java.io.BufferedReader
import java.io.InputStream

@Suppress("DEPRECATION")
internal class ShizukuInstaller(private val context: Context) : BaseInstaller {

	companion object {
		private val SESSION_ID_REGEX = Regex("(?<=\\[).+?(?=])")
	}

	override suspend fun performInstall(
		installItem: InstallItem,
		state: MutableStateFlow<InstallItemState>
	) = withContext(Dispatchers.IO) {
		state.emit(installItem statesTo InstallState.Installing)
		var sessionId: String? = null
		val uri = Cache.getReleaseUri(context, installItem.installFileName)
		val releaseFileLength = Cache.getReleaseFile(context, installItem.installFileName).length()
		val packageName = installItem.packageName.name
		try {
			val size =
				releaseFileLength.takeIf { it >= 0 } ?: throw IllegalStateException()
			context.contentResolver.openInputStream(uri).use {
				val createCommand =
					if (SdkCheck.isNougat) "pm install-create --user current -i $packageName -S $size"
					else "pm install-create -i $packageName -S $size"
				val createResult = exec(createCommand)
				sessionId = SESSION_ID_REGEX.find(createResult.out)?.value
					?: throw RuntimeException("Failed to create install session")

				val writeResult = exec("pm install-write -S $size $sessionId base -", it)
				if (writeResult.resultCode != 0) {
					throw RuntimeException("Failed to write APK to session $sessionId")
				}

				val commitResult = exec("pm install-commit $sessionId")
				if (commitResult.resultCode != 0) {
					throw RuntimeException("Failed to commit install session $sessionId")
				}
			}
		} catch (e: Exception) {
			state.emit(installItem statesTo InstallState.Failed)
			if (sessionId != null) exec("pm install-abandon $sessionId")
		}
		state.emit(installItem statesTo InstallState.Installed)
	}

	override suspend fun performUninstall(packageName: PackageName) =
		context.uninstallPackage(packageName)

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