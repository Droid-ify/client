package com.looker.core_database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.looker.core_database.model.Repo
import kotlinx.coroutines.flow.Flow

@Dao
interface RepoDao {

	@Query(value = "SELECT * FROM repo_table")
	fun getRepoStream(): Flow<List<Repo>>

	@Query(
		value = """
			SELECT * FROM repo_table
			WHERE repoId = :repoId
		"""
	)
	fun getRepo(repoId: Long): Flow<Repo>

	@Insert(onConflict = OnConflictStrategy.IGNORE)
	suspend fun insertReposOrIgnore(repoEntities: List<Repo>)

	@Update
	suspend fun updateRepo(repoEntity: Repo)

	@Query(
		value = """
			DELETE FROM repo_table
			WHERE repoId = :repoId
		"""
	)
	suspend fun deleteRepo(repoId: Long)

}