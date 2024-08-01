package com.looker.sync.fdroid.common

import kotlinx.serialization.json.Json

object JsonParser {

    val parser = Json { ignoreUnknownKeys = true }

}
