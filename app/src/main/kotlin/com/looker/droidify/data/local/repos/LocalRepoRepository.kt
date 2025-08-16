package com.looker.droidify.data.local.repos

import android.content.Context
import com.looker.droidify.data.RepoRepository
import com.looker.droidify.data.encryption.EncryptionStorage
import com.looker.droidify.data.local.dao.AppDao
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
import com.looker.droidify.sync.LocalSyncable
import com.looker.droidify.sync.v1.V1Syncable
import com.looker.droidify.sync.v2.EntrySyncable
import com.looker.droidify.utility.common.log
import com.looker.droidify.work.SyncWorker
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import kotlin.time.measureTime
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope

class LocalRepoRepository @Inject constructor(
    encryptionStorage: EncryptionStorage,
    downloader: Downloader,
    @ApplicationContext private val context: Context,
    @IoDispatcher syncDispatcher: CoroutineDispatcher,
    private val repoDao: RepoDao,
    private val authDao: AuthDao,
    private val indexDao: IndexDao,
    private val settingsRepository: SettingsRepository,
    private val appDao: AppDao,
) : RepoRepository {

    private val localSyncable = LocalSyncable(context = context)

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
    private val keyStream = encryptionStorage.key
    private val locale = settings.map { it.language }

    override suspend fun getRepo(id: Int): Repo? {
        val repoEntity = repoDao.getRepo(id) ?: return null
        val key = keyStream.first()
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
        keyStream,
    ) { repo, enabled, key ->
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
        keyStream,
    ) { repos, enabledIds, key ->
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

    override val addresses: Flow<Set<String>>
        get() = combine(
            repoDao.stream(),
            repoDao.mirrors(),
        ) { repos, mirrors ->
            repos.map { it.address }.toSet() + mirrors.map { it.url }
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
            val key = keyStream.first()
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
        if (enable) {
            SyncWorker.syncRepo(context, repo.id)
        } else {
            repoDao.resetTimestamp(repo.id)
            runCatching {
                val indexDir = File(context.cacheDir, "index")
                if (indexDir.exists()) {
                    indexDir.listFiles()?.forEach { file ->
                        if (file.name.startsWith("repo_${repo.id}_")) {
                            file.delete()
                        }
                    }
                }
            }
            appDao.deleteByRepoId(repo.id)
        }
    }

    override suspend fun sync(repo: Repo): Boolean {
        val (fingerprint, index) = localSyncable.sync(repo)
        return if (index != null) {
            val time = measureTime {
                indexDao.insertIndex(
                    fingerprint = fingerprint,
                    index = index,
                    expectedRepoId = repo.id,
                )
            }
            log("sync() took $time", "RoomQuery")
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
