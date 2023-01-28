package com.looker.core.data.fdroid.repository.offline

import com.looker.core.data.fdroid.repository.AppRepository
import com.looker.core.database.dao.AppDao
import com.looker.core.database.model.AppEntity
import com.looker.core.database.model.PackageEntity
import com.looker.core.database.model.toExternalModel
import com.looker.core.datastore.UserPreferencesRepository
import com.looker.core.model.newer.App
import com.looker.core.model.newer.Author
import com.looker.core.model.newer.Package
import com.looker.core.model.newer.PackageName
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class OfflineFirstAppRepository(
	private val appDao: AppDao,
	private val userPreferencesRepository: UserPreferencesRepository
) : AppRepository {
	override fun getApps(): Flow<List<App>> =
		appDao.getAppStream().map { it.map(AppEntity::toExternalModel) }

	override fun getApp(packageName: PackageName): Flow<List<App>> =
		appDao.getApp(packageName.name).map { it.map(AppEntity::toExternalModel) }

	override fun getAppFromAuthor(author: Author): Flow<List<App>> =
		appDao.getAppsFromAuthor(author.name).map { it.map(AppEntity::toExternalModel) }

	override fun getPackages(packageName: PackageName): Flow<List<Package>> =
		appDao.getPackages(packageName.name).map { it.map(PackageEntity::toExternalModel) }

	override suspend fun addToFavourite(packageName: PackageName): Boolean {
		val isFavourite =
			userPreferencesRepository
				.fetchInitialPreferences()
				.favouriteApps
				.any { it == packageName.name }
		userPreferencesRepository.addToFavourites(packageName.name, !isFavourite)
		return !isFavourite
	}
}