package com.looker.core_data

import com.looker.core_database.model.App
import java.io.InputStream

interface IndexParser {

	companion object {
		internal fun validateIcon(icon: String): String {
			return if (icon.endsWith(".xml")) "" else icon
		}
	}

	suspend fun parseIndex(repoId: Long, inputStream: InputStream, parserCallback: ParserCallback)

}

interface ParserCallback {

	fun onRepo(
		mirrors: List<String>,
		name: String,
		description: String,
		version: Int,
		timestamp: Long,
	)

	fun onApp(app: App)

}