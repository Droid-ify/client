package com.looker.core.data.fdroid

import kotlinx.serialization.json.Json

val indexJson = Json {
	ignoreUnknownKeys = true
	encodeDefaults = true
}