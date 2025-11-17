package com.looker.droidify.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Query
import androidx.room.Upsert
import com.looker.droidify.data.local.model.RBLogEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RBLogDao {
    @Upsert
    suspend fun upsert(logs: List<RBLogEntity>)

    @Delete
    suspend fun delete(log: RBLogEntity)

    @Query("SELECT * FROM rblog WHERE packageName = :packageName")
    suspend fun get(packageName: String): List<RBLogEntity>

    @Query("SELECT * FROM rblog WHERE packageName = :packageName")
    fun getFlow(packageName: String): Flow<List<RBLogEntity>>
}
