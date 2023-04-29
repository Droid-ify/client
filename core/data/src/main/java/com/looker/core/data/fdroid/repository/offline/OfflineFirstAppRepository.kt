package com.looker.core.data.fdroid.repository.offline

import com.looker.core.data.fdroid.repository.AppRepository
import com.looker.core.database.dao.AppDao
import com.looker.core.database.model.AppEntity
import com.looker.core.database.model.PackageEntity
import com.looker.core.database.model.toExternalModel
import com.looker.core.database.utils.localeListCompat
import com.looker.core.datastore.UserPreferencesRepository
import com.looker.core.datastore.distinctMap
import com.looker.core.model.newer.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import javax.inject.Inject

class OfflineFirstAppRepository @Inject constructor(
	private val appDao: AppDao,
	private val userPreferencesRepository: UserPreferencesRepository
) : AppRepository {

	private val localePreference = userPreferencesRepository
		.userPreferencesFlow
		.distinctMap { it.language }

	override fun getApps(): Flow<List<App>> =
		appDao.getAppStream().localizedAppList(localePreference)

	override fun getApp(packageName: PackageName): Flow<List<App>> =
		appDao.getApp(packageName.name).localizedAppList(localePreference)

	override fun getAppFromAuthor(author: Author): Flow<List<App>> =
		appDao.getAppsFromAuthor(author.name).localizedAppList(localePreference)

	override fun getPackages(packageName: PackageName): Flow<List<Package>> =
		appDao.getPackages(packageName.name).localizedPackages(localePreference)

	override suspend fun addToFavourite(packageName: PackageName): Boolean {
		val isFavourite =
			userPreferencesRepository
				.fetchInitialPreferences()
				.favouriteApps
				.any { it == packageName.name }
		userPreferencesRepository.toggleFavourites(packageName.name)
		return !isFavourite
	}
}

private fun Flow<List<AppEntity>>.localizedAppList(preference: Flow<String>): Flow<List<App>> =
	combine(this, preference) { appsList, locale ->
		appsList.map { it.toExternalModel(localeListCompat(locale)) }
	}

private fun Flow<List<PackageEntity>>.localizedPackages(preference: Flow<String>): Flow<List<Package>> =
	combine(this, preference) { appsList, locale ->
		appsList.map { it.toExternalModel(localeListCompat(locale)) }
	}