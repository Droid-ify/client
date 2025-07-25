package com.looker.droidify.data

import com.looker.droidify.domain.model.Repo
import kotlinx.coroutines.flow.Flow

interface RepoRepository {

    suspend fun getRepo(id: Long): Repo?

    fun getRepos(): Flow<List<Repo>>

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
