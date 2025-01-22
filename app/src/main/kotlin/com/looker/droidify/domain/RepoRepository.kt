package com.looker.droidify.domain

import com.looker.droidify.domain.model.Repo
import kotlinx.coroutines.flow.Flow

interface RepoRepository {

    suspend fun getRepo(id: Long): Repo?

    fun getRepos(): Flow<List<Repo>>

    suspend fun updateRepo(repo: Repo)

    suspend fun enableRepository(repo: Repo, enable: Boolean)

    suspend fun sync(repo: Repo): Boolean

    suspend fun syncAll(): Boolean
}
