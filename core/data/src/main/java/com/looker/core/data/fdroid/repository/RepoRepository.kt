package com.looker.core.data.fdroid.repository

import com.looker.core.model.newer.Repo
import kotlinx.coroutines.flow.Flow

interface RepoRepository {

	fun getRepos(): Flow<List<Repo>>

	suspend fun updateRepo(repo: Repo): Boolean

	suspend fun enableRepository(repo: Repo, enable: Boolean)

	suspend fun sync(): Boolean

}