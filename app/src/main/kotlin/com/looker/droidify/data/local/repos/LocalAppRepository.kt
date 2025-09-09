package com.looker.droidify.data.local.repos

import com.looker.droidify.data.AppRepository
import com.looker.droidify.data.local.dao.AppDao
import com.looker.droidify.data.local.dao.RepoDao
import com.looker.droidify.data.local.model.toApp
import com.looker.droidify.data.local.model.toAppMinimal
import com.looker.droidify.data.model.App
import com.looker.droidify.data.model.AppMinimal
import com.looker.droidify.data.model.PackageName
import com.looker.droidify.datastore.SettingsRepository
import com.looker.droidify.datastore.get
import com.looker.droidify.datastore.model.SortOrder
import com.looker.droidify.sync.v2.model.DefaultName
import com.looker.droidify.sync.v2.model.Tag
import com.looker.droidify.utility.common.log
import javax.inject.Inject
import kotlin.time.measureTimedValue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class LocalAppRepository @Inject constructor(
    private val appDao: AppDao,
    private val repoDao: RepoDao,
    private val settingsRepository: SettingsRepository,
) : AppRepository {

    private val locale = settingsRepository.get { language }

    override suspend fun apps(
        sortOrder: SortOrder,
        searchQuery: String?,
        repoId: Int?,
        categoriesToInclude: List<DefaultName>?,
        categoriesToExclude: List<DefaultName>?,
        antiFeaturesToInclude: List<Tag>?,
        antiFeaturesToExclude: List<Tag>?,
    ): List<AppMinimal> = withContext(Dispatchers.Default) {
        val timedValue = measureTimedValue {
            val apps = appDao.query(
                sortOrder = sortOrder,
                searchQuery = searchQuery?.ifEmpty { null },
                repoId = repoId,
                categoriesToInclude = categoriesToInclude?.ifEmpty { null },
                categoriesToExclude = categoriesToExclude?.ifEmpty { null },
                antiFeaturesToInclude = antiFeaturesToInclude?.ifEmpty { null },
                antiFeaturesToExclude = antiFeaturesToExclude?.ifEmpty { null },
            )

            if (apps.isEmpty()) return@measureTimedValue emptyList()

            val repoIds = apps.map { it.repoId }.distinct()

            val addresses = repoDao.getAddressByIds(repoIds)
            val versions = appDao.suggestedVersionNamesAll()

            val currentLocale = locale.first()
            apps.map { app ->
                val address = addresses[app.repoId]!!
                app.toAppMinimal(
                    locale = currentLocale,
                    baseAddress = address,
                    suggestedVersion = versions[app.id] ?: "",
                )
            }
        }
        log("apps() took ${timedValue.duration}", "RoomQuery")
        timedValue.value
    }

    override val categories: Flow<List<DefaultName>>
        get() = repoDao.categories().map { it.map { category -> category.defaultName } }

    override fun getApp(packageName: PackageName): Flow<List<App>> {
        return appDao.queryAppEntity(packageName.name)
            .map { appEntityRelations ->
                appEntityRelations.map {
                    val repo = repoDao.getRepo(it.app.repoId)!!
                    it.toApp(locale.first(), repo)
                }
            }
    }

    override suspend fun addToFavourite(packageName: PackageName): Boolean {
        val favourites = settingsRepository.get { favouriteApps }.first()
        val wasInFavourites = packageName.name in favourites
        settingsRepository.toggleFavourites(packageName.name)
        return !wasInFavourites
    }
}
