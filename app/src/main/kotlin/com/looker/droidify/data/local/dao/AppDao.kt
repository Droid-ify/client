package com.looker.droidify.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.looker.droidify.data.local.model.AppEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AppDao {

    @Query("SELECT * FROM app")
    fun stream(): Flow<List<AppEntity>>

    @Upsert
    suspend fun upsert(appEntity: AppEntity)

    @Query("DELETE FROM app WHERE id = :id")
    suspend fun delete(id: Int)

}
