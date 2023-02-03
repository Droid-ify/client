package com.looker.core.data.fdroid.repository

import android.content.Context
import com.looker.core.model.newer.Repo
import kotlinx.coroutines.flow.Flow

interface RepoRepository {

	fun getRepos(): Flow<List<Repo>>

	suspend fun updateRepo(repo: Repo): Boolean

	suspend fun enableRepository(repo: Repo, enable: Boolean)

	suspend fun sync(context: Context, repo: Repo, allowUnstable: Boolean): Boolean

	suspend fun syncAll(context: Context, allowUnstable: Boolean): Boolean

}