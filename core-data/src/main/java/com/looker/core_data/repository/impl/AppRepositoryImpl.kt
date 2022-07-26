package com.looker.core_data.repository.impl

import com.looker.core_data.repository.AppRepository
import com.looker.core_database.dao.AppDao
import com.looker.core_database.model.App
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class AppRepositoryImpl(private val appDao: AppDao) : AppRepository {
	override fun getAppsStream(): Flow<List<App>> = appDao.getAppStream()

	override fun getCategoriesStream(): Flow<List<String>> =
		appDao.getAppStream().map { it.flatMap { app -> app.categories } }

	override fun getAppsFromCategory(category: String): Flow<List<App>> =
		appDao.getAppStream().map {
			it.filter { app -> app.categories.contains(category) }
		}

	override fun getAppsFromAuthor(authorName: String): Flow<List<App>> =
		appDao.getAppsFromAuthor(authorName)

	override fun getAppData(packageName: String): Flow<App> =
		appDao.getApp(packageName)

	override suspend fun insertApps(apps: List<App>) =
		appDao.insertAppsOrIgnore(apps)

	override suspend fun insertApps(vararg app: App) {
		appDao.insertAppsOrIgnore(app.toList())
	}

	override suspend fun updateAppData(app: App) =
		appDao.updateApp(app)

	override suspend fun deleteAppData(packageName: String) =
		appDao.deleteApp(packageName)
}