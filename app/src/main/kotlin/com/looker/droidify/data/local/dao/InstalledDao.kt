package com.looker.droidify.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.looker.droidify.data.local.model.InstalledEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for installed applications.
 * Provides methods to interact with the installed table in the database.
 */
@Dao
interface InstalledDao {

    /**
     * Get an installed app by package name as a Flow.
     * @param packageName The package name of the app.
     * @return A Flow emitting the installed app or null if not found.
     */
    @Query("SELECT * FROM installed WHERE packageName = :packageName")
    fun stream(packageName: String): Flow<InstalledEntity?>

    /**
     * Get all installed apps as a Flow.
     * @return A Flow emitting a list of all installed apps.
     */
    @Query("SELECT * FROM installed")
    fun streamAll(): Flow<List<InstalledEntity>>

    /**
     * Get an installed app by package name.
     * @param packageName The package name of the app.
     * @return The installed app or null if not found.
     */
    @Query("SELECT * FROM installed WHERE packageName = :packageName")
    suspend fun get(packageName: String): InstalledEntity?

    /**
     * Insert or update an installed app.
     * @param installedEntity The installed app to insert or update.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(installedEntity: InstalledEntity)

    /**
     * Insert or update multiple installed apps.
     * @param installedEntities The list of installed apps to insert or update.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(installedEntities: List<InstalledEntity>)

    /**
     * Replace all installed apps with a new list.
     * @param installedEntities The new list of installed apps.
     */
    @Transaction
    suspend fun replaceAll(installedEntities: List<InstalledEntity>) {
        deleteAll()
        insertAll(installedEntities)
    }

    /**
     * Delete an installed app by package name.
     * @param packageName The package name of the app to delete.
     * @return The number of rows affected.
     */
    @Query("DELETE FROM installed WHERE packageName = :packageName")
    suspend fun delete(packageName: String): Int

    /**
     * Delete all installed apps.
     */
    @Query("DELETE FROM installed")
    suspend fun deleteAll()
}
