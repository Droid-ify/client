package com.looker.installer.installer

import android.content.Context
import android.net.Uri
import androidx.core.net.toFile
import androidx.core.net.toUri
import com.looker.core.common.Util
import com.looker.installer.utils.BaseInstaller
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import rikka.shizuku.Shizuku
import java.io.BufferedReader
import java.io.File
import java.io.InputStream

internal class ShizukuInstaller(context: Context) : BaseInstaller(context) {

	override suspend fun install(packageName: String, uri: Uri, file: File) {
		mShizukuInstaller(packageName, file.toUri())
	}

	private suspend fun mShizukuInstaller(packageName: String, uri: Uri) =
		withContext(Dispatchers.IO) {

			var sessionId: String?

			val size =
				uri.toFile().length().takeIf { it >= 0 } ?: throw IllegalStateException()
			context.contentResolver.openInputStream(uri).use {
				val createCommand =
					if (Util.isNougat) "pm install-create --user current -i $packageName -S $size"
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

		}

	private data class ShellResult(val resultCode: Int, val out: String)

	private fun exec(command: String, stdin: InputStream? = null): ShellResult {
		@Suppress("DEPRECATION")
		val process = Shizuku.newProcess(arrayOf("sh", "-c", command), null, null)
		if (stdin != null) {
			process.outputStream.use { stdin.copyTo(it) }
		}
		val output = process.inputStream.bufferedReader().use(BufferedReader::readText)
		val resultCode = process.waitFor()
		return ShellResult(resultCode, output)
	}

	companion object {
		private val SESSION_ID_REGEX = Regex("(?<=\\[).+?(?=])")
	}
}