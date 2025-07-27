package com.looker.droidify.data.local.repos

import com.looker.droidify.data.InstalledRepository
import com.looker.droidify.data.local.dao.InstalledDao
import com.looker.droidify.data.local.model.toDomain
import com.looker.droidify.data.local.model.toEntity
import com.looker.droidify.model.InstalledItem
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Implementation of [InstalledRepository] that uses Room database.
 * @param installedDao The DAO for installed applications.
 */
class LocalInstalledRepository @Inject constructor(
    private val installedDao: InstalledDao
) : InstalledRepository {

    override fun getStream(packageName: String): Flow<InstalledItem?> {
        return installedDao.stream(packageName).map { entity ->
            entity?.toDomain()
        }
    }

    override fun getAllStream(): Flow<List<InstalledItem>> {
        return installedDao.streamAll().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun get(packageName: String): InstalledItem? {
        return installedDao.get(packageName)?.toDomain()
    }

    override suspend fun put(installedItem: InstalledItem) {
        installedDao.insert(installedItem.toEntity())
    }

    override suspend fun putAll(installedItems: List<InstalledItem>) {
        installedDao.replaceAll(installedItems.map { it.toEntity() })
    }

    override suspend fun delete(packageName: String): Int {
        return installedDao.delete(packageName)
    }
}
