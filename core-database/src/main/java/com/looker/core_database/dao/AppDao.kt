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
			WHERE authorName= :authorName
		"""
	)
	fun getAppsFromAuthor(authorName: String): Flow<List<App>>

	@Query(
		value = """
			SELECT apks FROM app_table
			WHERE packageName= :packageName
		"""
	)
	fun getApks(packageName: String): Flow<List<Apk>>

	@Query(
		value = """
			SELECT * FROM app_table
			WHERE packageName = :packageName
		"""
	)
	fun getApp(packageName: String): Flow<App>

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