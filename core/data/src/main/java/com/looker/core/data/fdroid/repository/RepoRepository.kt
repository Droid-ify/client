package com.looker.core.data.fdroid.repository

import com.looker.core.model.newer.Repo
import kotlinx.coroutines.flow.Flow

interface RepoRepository {

	fun getRepos() : Flow<List<Repo>>

	fun getRepo(id: Long) : Flow<Repo>

}