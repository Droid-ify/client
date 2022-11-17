package com.looker.index.model.v1

import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

object IndexParser {

	private var commonJson: Json? = null

	val json: Json
		@JvmStatic
		get() {
			return commonJson ?: synchronized(this) {
				Json { ignoreUnknownKeys = true }
			}
		}

	fun parseV1(string: String): IndexV1 = json.decodeFromString(string)
}