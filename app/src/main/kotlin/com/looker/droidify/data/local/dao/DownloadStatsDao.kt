package com.looker.droidify.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import com.looker.droidify.data.local.model.DownloadStats
import kotlinx.coroutines.flow.Flow

@Dao
interface DownloadStatsDao {
    @Upsert
    suspend fun upsert(vararg stats: DownloadStats)

    @Delete
    suspend fun delete(log: DownloadStats)

    @Query("SELECT COUNT(*) == 0 FROM downloadStats")
    fun isEmpty(): Boolean

    @Query("SELECT * FROM downloadStats WHERE packageName = :packageName")
    fun get(packageName: String): List<DownloadStats>

    @Query("SELECT * FROM downloadStats WHERE packageName = :packageName")
    fun getFlow(packageName: String): Flow<List<DownloadStats>>

    @Query("SELECT * FROM downloadStats WHERE packageName = :packageName AND date >= :since")
    fun getFlowSince(packageName: String, since: Int): Flow<List<DownloadStats>>

    @Transaction
    suspend fun multipleUpserts(updates: Collection<DownloadStats>) {
        updates.forEach { metadata ->
            upsert(metadata)
        }
    }
}
