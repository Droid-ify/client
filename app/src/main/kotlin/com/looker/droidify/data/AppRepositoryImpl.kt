package com.looker.droidify.data

import com.looker.droidify.data.local.dao.AppDao
import com.looker.droidify.data.local.model.toApp
import com.looker.droidify.datastore.SettingsRepository
import com.looker.droidify.datastore.get
import com.looker.droidify.domain.AppRepository
import com.looker.droidify.domain.model.App
import com.looker.droidify.domain.model.Author
import com.looker.droidify.domain.model.Package
import com.looker.droidify.domain.model.PackageName
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppRepositoryImpl @Inject constructor(
    private val appDao: AppDao,
    private val settingsRepository: SettingsRepository,
) : AppRepository {

    private val locale = settingsRepository.get { language }

    override fun getApp(packageName: PackageName): Flow<List<App>> {
        return appDao.queryAppEntity(packageName.name)
            .map { appEntityRelations ->
                appEntityRelations.map { it.toApp(locale.first()) }
            }
    }

    override fun getAppFromAuthor(author: Author): Flow<List<App>> {
        // This would need to query apps by author ID
        // For now, return an empty list
        return flowOf(emptyList())
    }

    override fun getPackages(packageName: PackageName): Flow<List<Package>> {
        return appDao.queryAppEntity(packageName.name)
            .map { appEntityRelations ->
                appEntityRelations.flatMap { it.toApp(locale.first()).packages }
            }
    }

    override suspend fun addToFavourite(packageName: PackageName): Boolean {
        val favourites = settingsRepository.get { favouriteApps }.first()
        val wasInFavourites = packageName.name in favourites
        settingsRepository.toggleFavourites(packageName.name)
        return !wasInFavourites
    }
}
