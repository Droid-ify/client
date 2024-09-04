package com.looker.core.data.repository

import android.content.Context
import com.looker.core.common.extension.exceptCancellation
import com.looker.core.data.fdroid.toEntity
import com.looker.core.database.dao.AppDao
import com.looker.core.database.dao.RepoDao
import com.looker.core.database.model.toExternal
import com.looker.core.database.model.update
import com.looker.core.datastore.SettingsRepository
import com.looker.core.di.ApplicationScope
import com.looker.core.di.DefaultDispatcher
import com.looker.core.domain.RepoRepository
import com.looker.core.domain.model.Repo
import com.looker.network.Downloader
import com.looker.sync.fdroid.v2.EntrySyncable
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.withContext
import javax.inject.Inject

class OfflineFirstRepoRepository @Inject constructor(
    @ApplicationContext context: Context,
    private val appDao: AppDao,
    private val repoDao: RepoDao,
    private val settingsRepository: SettingsRepository,
    downloader: Downloader,
    @DefaultDispatcher private val dispatcher: CoroutineDispatcher,
    @ApplicationScope private val scope: CoroutineScope
) : RepoRepository {

    private val preference = runBlocking {
        settingsRepository.getInitial()
    }

    private val locale = preference.language

    override suspend fun getRepo(id: Long): Repo = withContext(dispatcher) {
        repoDao.getRepoById(id).toExternal(locale)
    }

    override fun getRepos(): Flow<List<Repo>> =
        repoDao.getRepoStream().map { it.toExternal(locale) }

    override suspend fun updateRepo(repo: Repo) {
        scope.launch {
            val entity = repoDao.getRepoById(repo.id)
            repoDao.upsertRepo(entity.update(repo))
        }
    }

    override suspend fun enableRepository(repo: Repo, enable: Boolean) {
        scope.launch {
            val entity = repoDao.getRepoById(repo.id)
            repoDao.upsertRepo(entity.copy(enabled = enable))
            if (enable) sync(repo)
        }
    }

    private val syncable = EntrySyncable(context, downloader, dispatcher)

    override suspend fun sync(repo: Repo): Boolean = coroutineScope {
        try {
            val (fingerprint, indexV2) = syncable.sync(repo)
            if (indexV2 == null) return@coroutineScope true
            val updatedRepo = indexV2.repo.toEntity(
                id = repo.id,
                fingerprint = fingerprint.value,
                etag = "",
                username = repo.authentication.username,
                password = repo.authentication.password,
            )
            val apps = indexV2.packages
                .map { (packageName, packageV2) ->
                    packageV2.toEntity(
                        packageName = packageName,
                        repoId = repo.id,
                        allowUnstable = preference.unstableUpdate,
                    )
                }
            repoDao.upsertRepo(updatedRepo)
            appDao.upsertApps(apps)
            true
        } catch (e: Exception) {
            e.exceptCancellation()
            false
        }
    }

    override suspend fun syncAll(): Boolean = supervisorScope {
        val repos = repoDao
            .getRepoStream()
            .first()
            .filter { it.enabled }
        repos.forEach {
            sync(it.toExternal("en-US"))
        }
        true
    }
}
