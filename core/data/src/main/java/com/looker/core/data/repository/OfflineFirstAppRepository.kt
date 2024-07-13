package com.looker.core.data.repository

import com.looker.core.domain.model.PackageName
import com.looker.core.domain.AppRepository
import com.looker.core.database.dao.AppDao
import com.looker.core.database.dao.InstalledDao
import com.looker.core.database.model.AppEntity
import com.looker.core.database.model.InstalledEntity
import com.looker.core.database.model.PackageEntity
import com.looker.core.database.model.toExternal
import com.looker.core.datastore.SettingsRepository
import com.looker.core.datastore.get
import com.looker.core.domain.model.App
import com.looker.core.domain.model.Author
import com.looker.core.domain.model.Package
import javax.inject.Inject
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

class OfflineFirstAppRepository @Inject constructor(
    installedDao: InstalledDao,
    private val appDao: AppDao,
    private val settingsRepository: SettingsRepository
) : AppRepository {

    private val localePreference = settingsRepository.get { language }

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

    override suspend fun addToFavourite(packageName: PackageName): Boolean = coroutineScope {
        val isFavourite =
            async {
                settingsRepository
                    .getInitial()
                    .favouriteApps
                    .any { it == packageName.name }
            }
        launch {
            settingsRepository.toggleFavourites(packageName.name)
        }
        !isFavourite.await()
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
