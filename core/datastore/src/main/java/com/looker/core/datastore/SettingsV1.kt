package com.looker.core.datastore

data class SettingsV1(
    val currentLocale: String = "system",
    val allowIncompatibleVersion: Boolean = false
)
