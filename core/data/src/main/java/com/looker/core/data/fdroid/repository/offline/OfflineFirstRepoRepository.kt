package com.looker.core.data.fdroid.repository.offline

import com.looker.core.data.di.ApplicationScope
import com.looker.core.data.di.DefaultDispatcher
import com.looker.core.data.fdroid.repository.RepoRepository
import com.looker.core.data.fdroid.sync.IndexDownloader
import com.looker.core.data.fdroid.sync.getFingerprint
import com.looker.core.data.fdroid.sync.getIndexV1
import com.looker.core.data.fdroid.toEntity
import com.looker.core.database.dao.AppDao
import com.looker.core.database.dao.RepoDao
import com.looker.core.database.model.RepoEntity
import com.looker.core.database.model.toExternal
import com.looker.core.database.model.update
import com.looker.core.datastore.UserPreferencesRepository
import com.looker.core.model.newer.Repo
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import org.fdroid.index.IndexConverter
import javax.inject.Inject

class OfflineFirstRepoRepository @Inject constructor(
	private val appDao: AppDao,
	private val repoDao: RepoDao,
	private val indexDownloader: IndexDownloader,
	private val userPreferencesRepository: UserPreferencesRepository,
	@DefaultDispatcher private val dispatcher: CoroutineDispatcher,
	@ApplicationScope private val scope: CoroutineScope
) : RepoRepository {

	private val preference = runBlocking {
		userPreferencesRepository
			.fetchInitialPreferences()
	}

	private val locale = preference.language

	private val indexConverter = IndexConverter()

	override suspend fun getRepo(id: Long): Repo = withContext(dispatcher) {
		repoDao.getRepoById(id).toExternal(locale)
	}

	override fun getRepos(): Flow<List<Repo>> =
		repoDao.getRepoStream().map { it.toExternal(locale) }

	override suspend fun updateRepo(repo: Repo) {
		scope.launch {
			val entity = repoDao.getRepoById(repo.id)
			repoDao.updateRepo(entity.update(repo))
		}
	}

	override suspend fun enableRepository(repo: Repo, enable: Boolean) {
		scope.launch {
			val entity = repoDao.getRepoById(repo.id)
			repoDao.updateRepo(entity.copy(enabled = enable))
			if (enable) sync(repo)
		}
	}

	override suspend fun sync(repo: Repo): Boolean =
		coroutineScope {
			val entity = repoDao.getRepoById(repo.id)
			val (_, indexJar) = indexDownloader.downloadIndexJar(entity)
			val newFingerprint = async { indexJar.getFingerprint() }
			val index = indexJar.getIndexV1()
			val convertedV2 = indexConverter.toIndexV2(index)
			val updatedRepo = convertedV2.repo.toEntity(
				id = repo.id,
				fingerprint = repo.fingerprint.ifEmpty { newFingerprint.await() },
				etag = repo.versionInfo.etag,
				username = repo.authentication.username,
				password = repo.authentication.password,
				enabled = true
			)
			repoDao.updateRepo(updatedRepo)
			val apps = convertedV2.packages.map {
				it.value.toEntity(it.key, repo.id, preference.unstableUpdate)
			}
			appDao.upsertApps(apps)
			true
		}

	override suspend fun syncAll(): Boolean =
		coroutineScope {
			val repos = repoDao.getRepoStream().first()
			val repoChannel = Channel<RepoEntity>()
			with(indexDownloader) {
				processRepos(repoChannel) { repo, jar ->
					val newFingerprint = async { jar.getFingerprint() }
					val index = jar.getIndexV1()
					val convertedIndex = indexConverter.toIndexV2(index)
					val updatedRepo = convertedIndex.repo.toEntity(
						id = repo.id!!,
						fingerprint = repo.fingerprint.ifEmpty { newFingerprint.await() },
						etag = repo.etag,
						username = repo.username,
						password = repo.password,
						enabled = true
					)
					repoDao.updateRepo(updatedRepo)
					val apps = convertedIndex.packages.map {
						it.value.toEntity(it.key, repo.id!!, preference.unstableUpdate)
					}
					appDao.upsertApps(apps)
				}
			}
			repos.forEach { repoChannel.send(it) }
			true
		}
}