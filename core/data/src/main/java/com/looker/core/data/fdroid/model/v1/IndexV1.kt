package com.looker.core.data.fdroid.model.v1

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import java.io.InputStream

@Serializable
data class IndexV1(
	val repo: RepoDto,
	val requests: Requests = Requests(emptyList(), emptyList()),
	val apps: List<AppDto> = emptyList(),
	val packages: Map<String, List<PackageDto>> = emptyMap(),
) {

	companion object {
		private val jsonObject = Json {
			encodeDefaults = true
			ignoreUnknownKeys = true
		}

		@OptIn(ExperimentalSerializationApi::class)
		fun decodeFromInputStream(inputStream: InputStream): IndexV1 =
			jsonObject.decodeFromStream(inputStream)

	}

}

@Serializable
data class Requests(
	val install: List<String>,
	val uninstall: List<String>,
)