package com.looker.droidify.data.local.dao

import androidx.room.Dao
import androidx.room.MapColumn
import androidx.room.Query
import androidx.room.Upsert
import com.looker.droidify.data.local.model.DownloadStatsFile

@Dao
interface DownloadStatsFileDao {
    @Upsert
    suspend fun upsert(vararg file: DownloadStatsFile)

    @Query("SELECT fileName, lastModified FROM downloadstatsfile WHERE fetchSuccess = 1")
    suspend fun getLastModifiedDates(): Map<
        @MapColumn(columnName = "fileName") String,
        @MapColumn(columnName = "lastModified") String,
        >
}
