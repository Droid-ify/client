package com.looker.core.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.looker.core.database.model.RepoEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RepoDao {

    @Query(value = "SELECT * FROM repos")
    fun getRepoStream(): Flow<List<RepoEntity>>

    @Query(value = "SELECT * FROM repos WHERE id = :id")
    suspend fun getRepoById(id: Long): RepoEntity

    @Upsert
    suspend fun upsertRepo(repoEntity: RepoEntity)

    @Query(
        value = """
			DELETE FROM repos
			WHERE id = :id
		"""
    )
    suspend fun deleteRepo(id: Long)

}
