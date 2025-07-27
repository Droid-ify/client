package com.looker.droidify.data

import com.looker.droidify.model.InstalledItem
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for installed applications.
 * Provides methods to interact with installed applications data.
 */
interface InstalledRepository {

    /**
     * Get an installed app by package name as a Flow.
     * @param packageName The package name of the app.
     * @return A Flow emitting the installed app or null if not found.
     */
    fun getStream(packageName: String): Flow<InstalledItem?>

    /**
     * Get all installed apps as a Flow.
     * @return A Flow emitting a list of all installed apps.
     */
    fun getAllStream(): Flow<List<InstalledItem>>

    /**
     * Get an installed app by package name.
     * @param packageName The package name of the app.
     * @return The installed app or null if not found.
     */
    suspend fun get(packageName: String): InstalledItem?

    /**
     * Insert or update an installed app.
     * @param installedItem The installed app to insert or update.
     */
    suspend fun put(installedItem: InstalledItem)

    /**
     * Replace all installed apps with a new list.
     * @param installedItems The new list of installed apps.
     */
    suspend fun putAll(installedItems: List<InstalledItem>)

    /**
     * Delete an installed app by package name.
     * @param packageName The package name of the app to delete.
     * @return The number of rows affected.
     */
    suspend fun delete(packageName: String): Int
}
