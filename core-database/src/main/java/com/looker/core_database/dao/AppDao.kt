package com.looker.core_database.dao

import androidx.room.*
import com.looker.core_database.model.Apk
import com.looker.core_database.model.App
import kotlinx.coroutines.flow.Flow

@Dao
interface AppDao {

	@Query(value = "SELECT * FROM app_table")
	fun getAppStream(): Flow<List<App>>

	@Query(
		value = """
			SELECT * FROM app_table
			WHERE packageName = :packageName
		"""
	)
	suspend fun getApp(packageName: String): App

	@Query(
		value = """
			SELECT apks FROM app_table
			WHERE packageName = :packageName
		"""
	)
	suspend fun getApksFor(packageName: String): List<Apk>

	@Insert(onConflict = OnConflictStrategy.IGNORE)
	suspend fun insertAppsOrIgnore(apps: List<App>)

	@Update
	suspend fun updateApp(app: App)

	@Query(
		value = """
			DELETE FROM app_table
			WHERE packageName = :packageName
		"""
	)
	suspend fun deleteApp(packageName: String)

}