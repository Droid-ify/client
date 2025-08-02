package com.looker.droidify.data

import com.looker.droidify.data.model.Repo
import kotlinx.coroutines.flow.Flow

interface RepoRepository {

    suspend fun getRepo(id: Int): Repo?

    fun repo(id: Int): Flow<Repo?>

    suspend fun deleteRepo(id: Int)

    val repos: Flow<List<Repo>>

    fun getEnabledRepos(): Flow<List<Repo>>

    suspend fun insertRepo(
        address: String,
        fingerprint: String?,
        username: String?,
        password: String?,
    )

    suspend fun enableRepository(repo: Repo, enable: Boolean)

    suspend fun sync(repo: Repo): Boolean

    suspend fun syncAll(): Boolean
}
