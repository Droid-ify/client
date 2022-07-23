package com.looker.core_data.model

import com.looker.core_database.model.Apk
import com.looker.core_database.model.App
import com.looker.core_database.model.Repo
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Actual output of out index.json
 * [apps] are all the apps in a Repository
 * [apkPackage] is a Map of [App.packageName] and [Apk]
 */
@Serializable
internal data class JsonData(
	@SerialName("repo") val repo: Repo,
	@SerialName("apps") val apps: List<App>,
	@SerialName("packages") val apkPackage: Map<String, List<Apk>>
)