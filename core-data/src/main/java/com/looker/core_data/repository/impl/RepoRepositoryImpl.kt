package com.looker.core_data.repository.impl

import com.looker.core_data.repository.RepoRepository
import com.looker.core_database.dao.RepoDao
import com.looker.core_database.model.Repo
import kotlinx.coroutines.flow.Flow

class RepoRepositoryImpl(private val repoDao: RepoDao) : RepoRepository {
	override fun getRepoStream(): Flow<List<Repo>> =
		repoDao.getRepoStream()

	override fun getRepo(repoId: Long): Flow<Repo> =
		repoDao.getRepo(repoId)

	override suspend fun addRepos(repos: List<Repo>) =
		repoDao.insertReposOrIgnore(repos)

	override suspend fun updateRepoData(repo: Repo) =
		repoDao.updateRepo(repo)

	override suspend fun deleteRepo(repoId: Long) =
		repoDao.deleteRepo(repoId)
}