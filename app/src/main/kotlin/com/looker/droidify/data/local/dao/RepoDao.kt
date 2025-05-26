package com.looker.droidify.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import com.looker.droidify.data.local.model.RepoEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RepoDao {

    @Query("SELECT * FROM repository")
    fun stream(): Flow<List<RepoEntity>>

    @Query("SELECT * FROM repository WHERE id = :repoId")
    fun repo(repoId: Int): Flow<RepoEntity>

    @Query("DELETE FROM repository WHERE id = :id")
    suspend fun delete(id: Int)

}
