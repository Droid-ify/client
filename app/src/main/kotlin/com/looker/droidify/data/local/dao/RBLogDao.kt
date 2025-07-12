package com.looker.droidify.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import androidx.room.Upsert
import com.looker.droidify.data.local.model.RBLogEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RBLogDao {
    @Insert
    suspend fun insert(vararg product: RBLogEntity)

    @Upsert
    suspend fun upsert(vararg product: RBLogEntity)

    @Update(onConflict = OnConflictStrategy.REPLACE)
    suspend fun update(vararg obj: RBLogEntity): Int

    @Delete
    suspend fun delete(obj: RBLogEntity)

    @Query("SELECT * FROM rblog WHERE packageName = :packageName")
    fun get(packageName: String): List<RBLogEntity>

    @Query("SELECT * FROM rblog WHERE packageName = :packageName")
    fun getFlow(packageName: String): Flow<List<RBLogEntity>>
}
