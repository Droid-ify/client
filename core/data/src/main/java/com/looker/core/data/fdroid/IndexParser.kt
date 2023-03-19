package com.looker.core.data.fdroid

import com.looker.core.data.fdroid.model.v1.IndexV1
import com.looker.core.data.fdroid.model.v2.Entry
import com.looker.core.data.fdroid.model.v2.IndexV2
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

object IndexParser {

	@Volatile
	private var JSON: Json? = null

	/**
	 * Initializing [Json] is expensive, so using this method is preferable as it keeps returning
	 * a single instance with the recommended settings.
	 */
	val json: Json
		@JvmStatic
		get() {
			return JSON ?: synchronized(this) {
				Json {
					ignoreUnknownKeys = true
				}
			}
		}

	@JvmStatic
	fun parseV1(str: String): IndexV1 {
		return json.decodeFromString(str)
	}

	@JvmStatic
	fun parseV2(str: String): IndexV2 {
		return json.decodeFromString(str)
	}

	@JvmStatic
	fun parseEntry(str: String): Entry {
		return json.decodeFromString(str)
	}

}
