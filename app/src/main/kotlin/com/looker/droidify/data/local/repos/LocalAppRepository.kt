package com.looker.droidify.data.local.repos

import com.looker.droidify.data.AppRepository
import com.looker.droidify.data.local.dao.AppDao
import com.looker.droidify.data.local.model.toApp
import com.looker.droidify.data.local.model.toAppMinimal
import com.looker.droidify.datastore.SettingsRepository
import com.looker.droidify.datastore.get
import com.looker.droidify.datastore.model.SortOrder
import com.looker.droidify.domain.model.App
import com.looker.droidify.domain.model.AppMinimal
import com.looker.droidify.domain.model.PackageName
import com.looker.droidify.sync.v2.model.DefaultName
import com.looker.droidify.sync.v2.model.Tag
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

class LocalAppRepository(
    private val appDao: AppDao,
    private val settingsRepository: SettingsRepository,
) : AppRepository {

    private val locale = settingsRepository.get { language }

    override fun apps(
        sortOrder: SortOrder,
        searchQuery: String?,
        repoId: Int?,
        categoriesToInclude: List<DefaultName>?,
        categoriesToExclude: List<DefaultName>?,
        antiFeaturesToInclude: List<Tag>?,
        antiFeaturesToExclude: List<Tag>?,
    ): Flow<List<AppMinimal>> = appDao.stream(
        sortOrder = sortOrder,
        searchQuery = searchQuery,
        repoId = repoId,
        categoriesToInclude = categoriesToInclude,
        categoriesToExclude = categoriesToExclude,
        antiFeaturesToInclude = antiFeaturesToInclude,
        antiFeaturesToExclude = antiFeaturesToExclude,
    ).map { apps ->
        apps.map { app ->
            app.toAppMinimal(
                locale = locale.first(),
                suggestedVersion = appDao.suggestedVersionName(app.id),
            )
        }
    }

    override fun getApp(packageName: PackageName): Flow<List<App>> {
        return appDao.queryAppEntity(packageName.name)
            .map { appEntityRelations ->
                appEntityRelations.map { it.toApp(locale.first()) }
            }
    }

    override suspend fun addToFavourite(packageName: PackageName): Boolean {
        val favourites = settingsRepository.get { favouriteApps }.first()
        val wasInFavourites = packageName.name in favourites
        settingsRepository.toggleFavourites(packageName.name)
        return !wasInFavourites
    }
}
