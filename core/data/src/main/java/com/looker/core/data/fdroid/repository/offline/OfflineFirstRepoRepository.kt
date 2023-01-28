package com.looker.core.data.fdroid.repository.offline

import com.looker.core.data.fdroid.repository.RepoRepository
import com.looker.core.database.dao.AppDao
import com.looker.core.database.dao.RepoDao
import com.looker.core.database.model.RepoEntity
import com.looker.core.database.model.toEntity
import com.looker.core.database.model.toExternalModel
import com.looker.core.datastore.UserPreferencesRepository
import com.looker.core.model.newer.Repo
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class OfflineFirstRepoRepository(
	private val appDao: AppDao,
	private val repoDao: RepoDao,
	private val userPreferencesRepository: UserPreferencesRepository
) : RepoRepository {
	override fun getRepos(): Flow<List<Repo>> =
		repoDao.getRepoStream().map { it.map(RepoEntity::toExternalModel) }

	override suspend fun updateRepo(repo: Repo): Boolean = try {
		repoDao.updateRepo(repo.toEntity())
		true
	} catch (e: Exception) {
		false
	}

	override suspend fun enableRepository(repo: Repo, enable: Boolean) {
		repoDao.updateRepo(repo.copy(enabled = enable).toEntity())
	}

	override suspend fun sync(): Boolean {
		TODO("Not yet implemented")
	}
}