package com.looker.sync.fdroid.common

import kotlinx.serialization.json.Json

val JsonParser = Json {
    ignoreUnknownKeys = true
    coerceInputValues = true
}
