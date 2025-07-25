package com.looker.droidify.data.local.repos

import android.content.Context
import com.looker.droidify.data.RepoRepository
import com.looker.droidify.data.local.dao.IndexDao
import com.looker.droidify.data.local.dao.RepoDao
import com.looker.droidify.data.local.model.toRepo
import com.looker.droidify.datastore.SettingsRepository
import com.looker.droidify.datastore.get
import com.looker.droidify.di.IoDispatcher
import com.looker.droidify.domain.model.Repo
import com.looker.droidify.network.Downloader
import com.looker.droidify.sync.v1.V1Syncable
import com.looker.droidify.sync.v2.EntrySyncable
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope

class LocalRepoRepository(
    downloader: Downloader,
    @ApplicationContext context: Context,
    @IoDispatcher syncDispatcher: CoroutineDispatcher,
    private val repoDao: RepoDao,
    private val indexDao: IndexDao,
    private val settingsRepository: SettingsRepository,
) : RepoRepository {

    private val v2Syncable = EntrySyncable(
        context = context,
        downloader = downloader,
        dispatcher = syncDispatcher,
    )

    private val v1Syncable = V1Syncable(
        context = context,
        downloader = downloader,
        dispatcher = syncDispatcher,
    )

    private val settings = settingsRepository.data
    private val locale = settings.map { it.language }

    override suspend fun getRepo(id: Long): Repo? {
        val repoId = id.toInt()
        val repoEntity = repoDao.repo(repoId).first()
        val enabled = repoId in settings.first().enabledRepoIds
        val mirrors = getMirrors(repoId)
        return repoEntity.toRepo(locale.first(), mirrors, enabled)
    }

    override fun getRepos(): Flow<List<Repo>> = combine(
        repoDao.stream(),
        settings.map { it.enabledRepoIds },
    ) { repos, enabledIds ->
        repos.map { repoEntity ->
            val mirrors = getMirrors(repoEntity.id)
            repoEntity.toRepo(locale.first(), mirrors, repoEntity.id in enabledIds)
        }
    }

    override fun getEnabledRepos(): Flow<List<Repo>> = settingsRepository
        .get { enabledRepoIds }
        .map { ids -> ids.mapNotNull { repoId -> getRepo(repoId.toLong()) } }

    override suspend fun enableRepository(repo: Repo, enable: Boolean) {
        settingsRepository.setRepoEnabled(repo.id, enable)
        if (enable) sync(repo)
    }

    override suspend fun sync(repo: Repo): Boolean {
        val (fingerprint, index) = v2Syncable.sync(repo) ?: v1Syncable.sync(repo) ?: return false
        return if (index != null) {
            indexDao.insertIndex(
                fingerprint = fingerprint,
                index = index,
                expectedRepoId = repo.id,
            )
            true
        } else false
    }

    override suspend fun syncAll(): Boolean = supervisorScope {
        val repos = getEnabledRepos().first()
        repos.forEach { repo -> launch { sync(repo) } }
        true
    }

    private suspend fun getMirrors(repoId: Int): List<String> =
        repoDao.mirrors(repoId).map { it.url }
}
