package com.looker.droidify.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy.Companion.REPLACE
import androidx.room.Query
import com.looker.droidify.data.local.model.DownloadStats
import kotlinx.coroutines.flow.Flow

@Dao
interface DownloadStatsDao {
    @Query(
        """
        SELECT (droidify + neoStore + fDroid + fDroidClassic + flicky + unknown)
        FROM download_stats
        WHERE packageName = :packageName
        """
    )
    fun total(packageName: String): Flow<Long>

    @Query(
        """
        SELECT (droidify + neoStore + fDroid + fDroidClassic + flicky + unknown)
        FROM download_stats
        WHERE packageName = :packageName AND timestamp >= :since
        """
    )
    fun totalSince(packageName: String, since: Long): Flow<Long>

    @Insert(onConflict = REPLACE)
    suspend fun insert(stats: List<DownloadStats>)
}
