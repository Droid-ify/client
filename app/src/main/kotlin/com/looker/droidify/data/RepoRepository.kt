package com.looker.droidify.data

import android.content.Context
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
import com.looker.droidify.sync.SyncState
import com.looker.droidify.sync.v1.V1Syncable
import com.looker.droidify.sync.v2.EntrySyncable
import com.looker.droidify.sync.v2.model.IndexV2
import com.looker.droidify.work.SyncWorker
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope

class RepoRepository @Inject constructor(
    encryptionStorage: EncryptionStorage,
    downloader: Downloader,
    @param:ApplicationContext private val context: Context,
    @IoDispatcher syncDispatcher: CoroutineDispatcher,
    private val repoDao: RepoDao,
    private val authDao: AuthDao,
    private val indexDao: IndexDao,
    private val settingsRepository: SettingsRepository,
    private val appDao: AppDao,
) {

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

    suspend fun getRepo(id: Int): Repo? {
        val repoEntity = repoDao.getRepo(id) ?: return null
        val key = keyStream.first()
        val auth = authDao.authFor(id)?.toAuthentication(key)
        val currentLocale = locale.first()
        val enabled = id in settings.first().enabledRepoIds
        val mirrors = getMirrors(id)
        val name = repoDao.name(id, currentLocale) ?: repoEntity.address
        val description = repoDao.description(id, currentLocale) ?: "..."
        val icon = repoDao.icon(id, currentLocale)?.icon?.name
        return repoEntity.toRepo(
            mirrors = mirrors,
            enabled = enabled,
            authentication = auth,
            name = name,
            description = description,
            icon = icon,
        )
    }

    fun repo(id: Int): Flow<Repo?> = combine(
        repoDao.repo(id),
        settings.map { it.enabledRepoIds },
        keyStream,
    ) { repo, enabled, key ->
        val auth = authDao.authFor(id)?.toAuthentication(key)
        val mirrors = getMirrors(id)
        val currentLocale = locale.first()
        val name = repoDao.name(id, currentLocale) ?: repo?.address ?: "Unknown"
        val description = repoDao.description(id, currentLocale) ?: "..."
        val icon = repoDao.icon(id, currentLocale)?.icon?.name
        repo?.toRepo(
            mirrors = mirrors,
            enabled = repo.id in enabled,
            authentication = auth,
            name = name,
            description = description,
            icon = icon,
        )
    }

    suspend fun deleteRepo(id: Int) {
        repoDao.delete(id)
    }

    val repos: Flow<List<Repo>> = combine(
        repoDao.stream(),
        settings.map { it.enabledRepoIds },
    ) { repos, enabledIds ->
        val currentLocale = locale.first()
        repos.map { repoEntity ->
            val name = repoDao.name(repoEntity.id, currentLocale) ?: repoEntity.address
            val description = repoDao.description(repoEntity.id, currentLocale) ?: "..."
            val icon = repoDao.icon(repoEntity.id, currentLocale)?.icon?.name
            repoEntity.toRepo(
                mirrors = emptyList(),
                authentication = null,
                enabled = repoEntity.id in enabledIds,
                name = name,
                description = description,
                icon = icon,
            )
        }
    }

    val addresses: Flow<Set<String>>
        get() = combine(
            repoDao.stream(),
            repoDao.mirrors(),
        ) { repos, mirrors ->
            repos.map { it.address }.toSet() + mirrors.map { it.url }
        }

    fun getEnabledRepos(): Flow<List<Repo>> = settingsRepository
        .get { enabledRepoIds }
        .map { ids -> ids.mapNotNull { repoId -> getRepo(repoId) } }

    suspend fun insertRepo(
        address: String,
        fingerprint: String?,
        username: String?,
        password: String?,
    ) {
        val id = indexDao.insertRepo(
            RepoEntity(
                address = address,
                fingerprint = Fingerprint(fingerprint.orEmpty()),
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

    suspend fun enableRepository(repo: Repo, enable: Boolean) {
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

    suspend fun sync(repo: Repo): Boolean {
        var success = false
        var parsedFingerprint: Fingerprint? = null
        var parsedIndex: IndexV2? = null
        localSyncable.sync(repo) { state ->
            when (state) {
                is SyncState.JsonParsing.Success -> {
                    parsedFingerprint = state.fingerprint
                    parsedIndex = state.index
                    success = true
                }

                else -> Unit
            }
        }
        if (parsedIndex != null && parsedFingerprint != null) {
            indexDao.insertIndex(
                fingerprint = parsedFingerprint,
                index = parsedIndex!!,
                expectedRepoId = repo.id,
            )
        }
        return success
    }

    suspend fun syncAll(): Boolean = supervisorScope {
        val repos = getEnabledRepos().first()
        repos.forEach { repo -> launch { sync(repo) } }
        true
    }

    private suspend fun getMirrors(repoId: Int): List<String> =
        repoDao.mirrors(repoId).map { it.url }
}
