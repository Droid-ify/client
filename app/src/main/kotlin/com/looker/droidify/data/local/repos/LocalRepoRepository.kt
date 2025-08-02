package com.looker.droidify.data.local.repos

import android.content.Context
import com.looker.droidify.data.RepoRepository
import com.looker.droidify.data.encryption.Key
import com.looker.droidify.data.local.dao.AuthDao
import com.looker.droidify.data.local.dao.IndexDao
import com.looker.droidify.data.local.dao.RepoDao
import com.looker.droidify.data.local.model.AuthenticationEntity
import com.looker.droidify.data.local.model.RepoEntity
import com.looker.droidify.data.local.model.toAuthentication
import com.looker.droidify.data.local.model.toRepo
import com.looker.droidify.data.model.Fingerprint
import com.looker.droidify.data.model.Repo
import com.looker.droidify.datastore.SettingsRepository
import com.looker.droidify.datastore.get
import com.looker.droidify.di.IoDispatcher
import com.looker.droidify.network.Downloader
import com.looker.droidify.sync.v1.V1Syncable
import com.looker.droidify.sync.v2.EntrySyncable
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope

class LocalRepoRepository @Inject constructor(
    downloader: Downloader,
    @ApplicationContext context: Context,
    @IoDispatcher syncDispatcher: CoroutineDispatcher,
    private val repoDao: RepoDao,
    private val authDao: AuthDao,
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
    private val key = Key() // TODO: Get from settings

    override suspend fun getRepo(id: Int): Repo? {
        val repoEntity = repoDao.getRepo(id) ?: return null
        val auth = authDao.authFor(id)?.toAuthentication(key)
        val enabled = id in settings.first().enabledRepoIds
        val mirrors = getMirrors(id)
        return repoEntity.toRepo(
            locale = locale.first(),
            mirrors = mirrors,
            enabled = enabled,
            authentication = auth,
        )
    }

    override fun repo(id: Int): Flow<Repo?> = combine(
        repoDao.repo(id),
        settings.map { it.enabledRepoIds },
    ) { repo, enabled ->
        val auth = authDao.authFor(id)?.toAuthentication(key)
        val mirrors = getMirrors(id)
        repo?.toRepo(
            locale = locale.first(),
            mirrors = mirrors,
            enabled = repo.id in enabled,
            authentication = auth,
        )
    }

    override suspend fun deleteRepo(id: Int) {
        repoDao.delete(id)
    }

    override val repos: Flow<List<Repo>> = combine(
        repoDao.stream(),
        settings.map { it.enabledRepoIds },
    ) { repos, enabledIds ->
        repos.map { repoEntity ->
            val mirrors = getMirrors(repoEntity.id)
            val auth = authDao.authFor(repoEntity.id)?.toAuthentication(key)
            repoEntity.toRepo(
                locale = locale.first(),
                mirrors = mirrors,
                enabled = repoEntity.id in enabledIds,
                authentication = auth,
            )
        }
    }

    override fun getEnabledRepos(): Flow<List<Repo>> = settingsRepository
        .get { enabledRepoIds }
        .map { ids -> ids.mapNotNull { repoId -> getRepo(repoId) } }

    override suspend fun insertRepo(
        address: String,
        fingerprint: String?,
        username: String?,
        password: String?,
    ) {
        val id = indexDao.insertRepo(
            RepoEntity(
                address = address,
                fingerprint = Fingerprint(fingerprint.orEmpty()),
                icon = null,
                name = mapOf("en-US" to address),
                description = mapOf("en-US" to "unsynced...."),
                timestamp = null,
                webBaseUrl = address,
            ),
        )
        if (password != null && username != null) {
            val (encrypted, iv) = key.encrypt(password)
            val authEntity = AuthenticationEntity(
                password = encrypted,
                username = username,
                initializationVector = iv,
                repoId = id.toInt(),
            )
            authDao.insert(authEntity)
        }
    }

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
