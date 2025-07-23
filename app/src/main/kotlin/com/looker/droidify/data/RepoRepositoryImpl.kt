package com.looker.droidify.data

import com.looker.droidify.data.local.dao.IndexDao
import com.looker.droidify.data.local.dao.RepoDao
import com.looker.droidify.data.local.model.toRepo
import com.looker.droidify.datastore.SettingsRepository
import com.looker.droidify.datastore.get
import com.looker.droidify.domain.RepoRepository
import com.looker.droidify.domain.model.Repo
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RepoRepositoryImpl @Inject constructor(
    private val repoDao: RepoDao,
    private val indexDao: IndexDao,
    private val settingsRepository: SettingsRepository,
) : RepoRepository {

    private val settings = settingsRepository.data
    private val locale = settings.map { it.language }

    override suspend fun getRepo(id: Long): Repo? {
        val repoId = id.toInt()
        val repoEntity = repoDao.repo(repoId).first()
        val enabled = repoId in settings.first().enabledRepoIds
        val mirrors = getMirrors(repoId)
        return repoEntity.toRepo(locale.first(), mirrors, enabled)
    }

    override fun getRepos(): Flow<List<Repo>> {
        return combine(
            repoDao.stream(),
            settingsRepository.get { enabledRepoIds },
        ) { repos, enabledIds ->
            repos.map { repoEntity ->
                val mirrors = getMirrors(repoEntity.id)
                repoEntity.toRepo(locale.first(), mirrors, repoEntity.id in enabledIds)
            }
        }
    }

    override suspend fun updateRepo(repo: Repo) = TODO("Yet to implement")

    override suspend fun enableRepository(repo: Repo, enable: Boolean) {
        settingsRepository.setRepoEnabled(repo.id, enable)
    }

    override suspend fun sync(repo: Repo): Boolean {
        return true
    }

    override suspend fun syncAll(): Boolean {
        return true
    }

    private suspend fun getMirrors(repoId: Int): List<String> =
        repoDao.mirrors(repoId).map { it.url }
}
