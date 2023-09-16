package com.looker.core.data.fdroid.repository.offline

import com.looker.core.common.extension.exceptCancellation
import com.looker.core.data.di.ApplicationScope
import com.looker.core.data.di.DefaultDispatcher
import com.looker.core.data.fdroid.repository.RepoRepository
import com.looker.core.data.fdroid.sync.IndexManager
import com.looker.core.data.fdroid.toEntity
import com.looker.core.database.dao.AppDao
import com.looker.core.database.dao.RepoDao
import com.looker.core.database.model.toExternal
import com.looker.core.database.model.update
import com.looker.core.datastore.SettingsRepository
import com.looker.core.model.newer.Repo
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class OfflineFirstRepoRepository @Inject constructor(
	private val appDao: AppDao,
	private val repoDao: RepoDao,
	private val settingsRepository: SettingsRepository,
	private val indexManager: IndexManager,
	@DefaultDispatcher private val dispatcher: CoroutineDispatcher,
	@ApplicationScope private val scope: CoroutineScope
) : RepoRepository {

	private val preference = runBlocking {
		settingsRepository.fetchInitialPreferences()
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

	override suspend fun sync(repo: Repo): Boolean = coroutineScope {
		val index = try {
			indexManager.getIndex(listOf(repo))[repo]!!
		} catch (e: Exception) {
			e.exceptCancellation()
			return@coroutineScope false
		}
		val updatedRepo = index.repo.toEntity(
			id = repo.id,
			fingerprint = repo.fingerprint,
			username = repo.authentication.username,
			password = repo.authentication.password,
			enabled = true
		)
		repoDao.upsertRepo(updatedRepo)
		val apps = index.packages.map {
			it.value.toEntity(it.key, repo.id, preference.unstableUpdate)
		}
		appDao.upsertApps(apps)
		true
	}

	override suspend fun syncAll(): Boolean = supervisorScope {
		val repos = repoDao.getRepoStream().first().filter { it.enabled }
		val indices = try {
			indexManager.getIndex(repos.toExternal(locale))
		} catch (e: Exception) {
			e.exceptCancellation()
			return@supervisorScope false
		}
		indices.forEach { (repo, index) ->
			val updatedRepo = index.repo.toEntity(
				id = repo.id,
				fingerprint = repo.fingerprint,
				username = repo.authentication.username,
				password = repo.authentication.password,
				enabled = true
			)
			repoDao.upsertRepo(updatedRepo)
			val apps = index.packages.map {
				it.value.toEntity(it.key, repo.id, preference.unstableUpdate)
			}
			appDao.upsertApps(apps)
		}
		true
	}
}