package com.looker.core.data.fdroid.repository.offline

import com.looker.core.data.fdroid.model.v1.allowUnstable
import com.looker.core.data.fdroid.model.v1.toEntity
import com.looker.core.data.fdroid.repository.RepoRepository
import com.looker.core.data.fdroid.sync.*
import com.looker.core.database.dao.AppDao
import com.looker.core.database.dao.RepoDao
import com.looker.core.database.model.RepoEntity
import com.looker.core.database.model.toEntity
import com.looker.core.database.model.toExternalModel
import com.looker.core.model.newer.Repo
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class OfflineFirstRepoRepository @Inject constructor(
	private val appDao: AppDao,
	private val repoDao: RepoDao
) : RepoRepository {
	override suspend fun getRepo(id: Long): Repo = repoDao.getRepoById(id).toExternalModel()

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
		if (enable) sync(repo, false)
	}

	override suspend fun sync(repo: Repo, allowUnstable: Boolean): Boolean =
		coroutineScope {
			val indexType = IndexType.INDEX_V1
			val repoLoc = downloadIndexJar(repo.toLocation(), indexType)
			val index = repoLoc.jar.getIndexV1()
			val updatedRepo = index.repo.toEntity(
				fingerPrint = repo.fingerprint,
				etag = repo.versionInfo.etag,
				username = repo.authentication.username,
				password = repo.authentication.password
			)
			val packages = index.packages
			val apps = index.apps.map {
				it.toEntity(
					repoId = repo.id,
					packages = packages[it.packageName]
						?.allowUnstable(it, allowUnstable)
						?: emptyList()
				)
			}
			repoDao.updateRepo(updatedRepo)
			appDao.upsertApps(apps)
			true
		}

	override suspend fun syncAll(allowUnstable: Boolean): Boolean =
		coroutineScope {
			val repos = repoDao.getRepoStream().first().map(RepoEntity::toExternalModel)
				.filter { it.enabled }
			val repoChannel = Channel<Repo>()
			processRepos(repoChannel) { repo, jar ->
				val index = jar.getIndexV1()
				val newFingerprint = async { jar.getFingerprint() }
				val updatedRepo = index.repo.toEntity(
					fingerPrint = repo.fingerprint.ifEmpty { newFingerprint.await() },
					etag = repo.versionInfo.etag,
					username = repo.authentication.username,
					password = repo.authentication.password
				)
				repoDao.updateRepo(updatedRepo)
				val packages = index.packages
				val apps = index.apps.map {
					it.toEntity(
						repoId = repo.id,
						packages = packages[it.packageName]
							?.allowUnstable(it, allowUnstable)
							?: emptyList()
					)
				}
				appDao.upsertApps(apps)
			}
			repos.forEach { repoChannel.send(it) }
			false
		}
}