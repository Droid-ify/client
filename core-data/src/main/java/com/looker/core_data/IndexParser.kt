package com.looker.core_data

import com.looker.core_database.model.Apk
import com.looker.core_database.model.App
import java.io.InputStream

interface IndexParser {

	interface Callback {
		fun onRepo(
			mirrors: List<String>,
			name: String,
			description: String,
			version: Int,
			timestamp: Long,
		)

		fun onApp(app: App)
		fun onApk(packageName: String, releases: List<Apk>)
	}

	suspend fun parseIndex(repoId: Long, inputStream: InputStream, callback: Callback)

	suspend fun parseApp(repoId: Long): App

	suspend fun parseApk(): Apk

}