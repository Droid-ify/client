package com.looker.core.database.dao

import androidx.room.*
import com.looker.core.database.model.InstalledEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface InstalledDao {

	@Query("SELECT * FROM installedentity")
	fun getInstalledStream(): Flow<List<InstalledEntity>>

	@Upsert
	suspend fun upsertInstalled(installedEntity: InstalledEntity)

	@Delete
	suspend fun deleteInstalled(installedEntity: InstalledEntity)

}