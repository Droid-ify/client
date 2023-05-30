package com.looker.core.data.fdroid.repository.offline

import com.looker.core.data.fdroid.repository.AppRepository
import com.looker.core.database.dao.AppDao
import com.looker.core.database.dao.InstalledDao
import com.looker.core.database.model.*
import com.looker.core.datastore.UserPreferencesRepository
import com.looker.core.datastore.distinctMap
import com.looker.core.model.newer.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import javax.inject.Inject

class OfflineFirstAppRepository @Inject constructor(
	installedDao: InstalledDao,
	private val appDao: AppDao,
	private val userPreferencesRepository: UserPreferencesRepository
) : AppRepository {

	private val localePreference = userPreferencesRepository
		.userPreferencesFlow
		.distinctMap { it.language }

	private val installedFlow = installedDao.getInstalledStream()

	override fun getApps(): Flow<List<App>> =
		appDao.getAppStream().localizedAppList(localePreference, installedFlow)

	override fun getApp(packageName: PackageName): Flow<List<App>> =
		appDao.getApp(packageName.name).localizedAppList(localePreference, installedFlow)

	override fun getAppFromAuthor(author: Author): Flow<List<App>> =
		appDao.getAppsFromAuthor(author.name).localizedAppList(localePreference, installedFlow)

	override fun getPackages(packageName: PackageName): Flow<List<Package>> =
		appDao.getPackages(packageName.name)
			.localizedPackages(packageName, localePreference, installedFlow)

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

private fun Flow<List<AppEntity>>.localizedAppList(
	preference: Flow<String>,
	installedFlow: Flow<List<InstalledEntity>>
): Flow<List<App>> =
	combine(this, preference, installedFlow) { appsList, locale, installedList ->
		appsList.toExternal(locale) {
			it.findInstalled(installedList)
		}
	}

private fun Flow<List<PackageEntity>>.localizedPackages(
	packageName: PackageName,
	preference: Flow<String>,
	installedFlow: Flow<List<InstalledEntity>>
): Flow<List<Package>> =
	combine(this, preference, installedFlow) { packagesList, locale, installedList ->
		packagesList.toExternal(locale) {
			InstalledEntity(packageName.name, it.versionCode, it.sig) in installedList
		}
	}

private fun AppEntity.findInstalled(list: List<InstalledEntity>): PackageEntity? =
	packages.find {
		InstalledEntity(packageName, it.versionCode, it.sig) in list
	}