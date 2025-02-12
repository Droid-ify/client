package com.looker.droidify.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import com.looker.droidify.data.local.model.AppEntity
import com.looker.droidify.data.local.model.AppEntityRelations
import kotlinx.coroutines.flow.Flow

@Dao
interface AppDao {

    @Query("SELECT * FROM app")
    fun stream(): Flow<List<AppEntity>>

    @Transaction
    @Query("SELECT * FROM app WHERE packageName = :packageName")
    fun queryAppEntity(packageName: String): Flow<List<AppEntityRelations>>

    @Query("SELECT COUNT(*) FROM app")
    suspend fun count(): Int

    @Query("DELETE FROM app WHERE id = :id")
    suspend fun delete(id: Int)
}
