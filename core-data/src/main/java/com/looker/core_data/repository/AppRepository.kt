package com.looker.core_data.repository

import com.looker.core_database.model.App
import kotlinx.coroutines.flow.Flow

interface AppRepository {

	fun getAppsStream(): Flow<List<App>>

	fun getCategoriesStream(): Flow<List<String>>

	fun getAppsFromCategory(category: String): Flow<List<App>>

	fun getAppsFromAuthor(authorName: String): Flow<List<App>>

	fun getAppData(packageName: String): Flow<App>

	suspend fun insertApps(apps: List<App>)

	suspend fun insertApps(vararg app: App)

	suspend fun updateAppData(app: App)

	suspend fun deleteAppData(packageName: String)

}