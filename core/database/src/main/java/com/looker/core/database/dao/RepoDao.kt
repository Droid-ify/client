package com.looker.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.looker.core.database.model.RepoEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RepoDao {

	@Query(value = "SELECT * FROM repos")
	fun getRepoStream(): Flow<List<RepoEntity>>

	@Update
	suspend fun updateRepo(repoEntity: RepoEntity)

	@Insert(onConflict = OnConflictStrategy.REPLACE)
	suspend fun insertRepo(repoEntity: RepoEntity)

	@Query(
		value = """
			DELETE FROM repos
			WHERE id = :id
		"""
	)
	suspend fun deleteRepo(id: Long)

}