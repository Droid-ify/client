package com.looker.core_data

import com.looker.core_database.model.App

interface ParserCallback {

	suspend fun onRepo(
		mirrors: List<String>,
		name: String,
		description: String,
		version: Int,
		timestamp: Long,
	)

	suspend fun onApp(app: App)

}