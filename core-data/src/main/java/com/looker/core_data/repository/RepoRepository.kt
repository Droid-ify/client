package com.looker.core_data.repository

import com.looker.core_database.model.Repo
import kotlinx.coroutines.flow.Flow

interface RepoRepository {

	fun getRepoStream(): Flow<List<Repo>>

	fun getRepo(repoId: Long): Flow<Repo>

	suspend fun addRepos(repos: List<Repo>)

	suspend fun updateRepoData(repo: Repo)

	suspend fun deleteRepo(repoId: Long)

}