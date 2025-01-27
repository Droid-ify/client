package com.looker.droidify.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.looker.droidify.data.local.model.RepoEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RepoDao {

    @Query("SELECT * FROM repository")
    fun stream(): Flow<List<RepoEntity>>

    @Upsert
    suspend fun upsert(repoEntity: RepoEntity)

    @Query("DELETE FROM repository WHERE id = :id")
    suspend fun delete(id: Int)

}
