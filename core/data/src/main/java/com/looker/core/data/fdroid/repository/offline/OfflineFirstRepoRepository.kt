package com.looker.core.data.fdroid.repository.offline

import com.looker.core.data.fdroid.repository.RepoRepository
import com.looker.core.data.fdroid.sync.*
import com.looker.core.data.fdroid.toEntity
import com.looker.core.database.dao.AppDao
import com.looker.core.database.dao.RepoDao
import com.looker.core.database.model.toExternalModel
import com.looker.core.database.model.update
import com.looker.core.database.utils.localeListCompat
import com.looker.core.datastore.UserPreferencesRepository
import com.looker.core.model.newer.Repo
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import org.fdroid.index.IndexConverter
import javax.inject.Inject

internal class OfflineFirstRepoRepository @Inject constructor(
	private val appDao: AppDao,
	private val repoDao: RepoDao,
	private val indexDownloader: IndexDownloader,
	private val userPreferencesRepository: UserPreferencesRepository
) : RepoRepository {

	private val preference = runBlocking {
		userPreferencesRepository
			.fetchInitialPreferences()
	}

	private val language = preference.language

	private val indexConverter = IndexConverter()

	override suspend fun getRepo(id: Long): Repo = repoDao.getRepoById(id).toExternalModel(
		localeListCompat(language)
	)

	override fun getRepos(): Flow<List<Repo>> =
		repoDao.getRepoStream()
			.map { it.map { repo -> repo.toExternalModel(localeListCompat(language)) } }

	override suspend fun updateRepo(repo: Repo): Boolean = try {
		val entity = repoDao.getRepoById(repo.id)
		repoDao.updateRepo(entity.update(repo))
		true
	} catch (e: Exception) {
		false
	}

	override suspend fun enableRepository(repo: Repo, enable: Boolean) {
		val entity = repoDao.getRepoById(repo.id)
		repoDao.updateRepo(entity.copy(enabled = enable))
		if (enable) sync(repo)
	}

	override suspend fun sync(repo: Repo): Boolean =
		coroutineScope {
			val (_, indexJar) = indexDownloader.downloadIndexJar(repo)
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
			val repos = repoDao.getRepoStream().first().map { it.toExternalModel(localeListCompat(language)) }
				.filter { it.enabled }
			val repoChannel = Channel<Repo>()
			with(indexDownloader) {
				processRepos(repoChannel) { repo, jar ->
					val newFingerprint = async { jar.getFingerprint() }
					val index = jar.getIndexV1()
					val convertedIndex = indexConverter.toIndexV2(index)
					val updatedRepo = convertedIndex.repo.toEntity(
						id = repo.id,
						fingerprint = repo.fingerprint.ifEmpty { newFingerprint.await() },
						etag = repo.versionInfo.etag,
						username = repo.authentication.username,
						password = repo.authentication.password,
						enabled = true
					)
					repoDao.updateRepo(updatedRepo)
					val apps = convertedIndex.packages.map {
						it.value.toEntity(it.key, repo.id, preference.unstableUpdate)
					}
					appDao.upsertApps(apps)
				}
			}
			repos.forEach { repoChannel.send(it) }
			true
		}
}