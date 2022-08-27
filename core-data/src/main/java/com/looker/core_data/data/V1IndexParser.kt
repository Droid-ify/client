package com.looker.core_data.data

import com.looker.core_data.ParserCallback
import com.looker.core_data.model.JsonData
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import java.io.InputStream

class V1IndexParser {
	private val json = Json {
		ignoreUnknownKeys = true
		encodeDefaults = true
	}

	@OptIn(ExperimentalSerializationApi::class)
	suspend fun parseIndex(
		repoId: Long,
		inputStream: InputStream,
		parserCallback: ParserCallback
	) {
		val jsonData = json.decodeFromStream<JsonData>(inputStream)
		val repo = jsonData.repo
		val apkPackages = jsonData.apkPackage
		val apps = jsonData.apps.map { currentApp ->
			val all = apkPackages[currentApp.packageName]
			currentApp.copy(
				repoId = repoId,
				apks = all ?: emptyList()
			)
		}
		parserCallback.onRepo(
			repo.mirrors,
			repo.name,
			repo.description,
			repo.version,
			repo.timestamp
		)
		apps.forEach { parserCallback.onApp(it) }
	}
}