package com.looker.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Upsert
import com.looker.core.database.model.AppEntity
import com.looker.core.database.model.PackageEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AppDao {

	@Query(value = "SELECT * FROM apps")
	fun getAppStream(): Flow<List<AppEntity>>

	@Query(
		value = """
			SELECT * FROM apps
			WHERE authorName = :authorName
		"""
	)
	fun getAppsFromAuthor(authorName: String): Flow<List<AppEntity>>

	@Query(value = "SELECT * FROM apps WHERE packageName = :packageName")
	fun getApp(packageName: String): Flow<List<AppEntity>>

	@Query(
		value = """
			SELECT packages FROM apps
			WHERE packageName = :packageName
		"""
	)
	fun getPackages(packageName: String): Flow<List<PackageEntity>>

	@Insert(onConflict = OnConflictStrategy.IGNORE)
	suspend fun insertOrIgnore(apps: List<AppEntity>)

	@Upsert
	suspend fun upsertApps(apps: List<AppEntity>)

	@Query(
		value = """
			DELETE FROM apps
			WHERE repoId = :repoId
		"""
	)
	suspend fun deleteApps(repoId: Long)

}