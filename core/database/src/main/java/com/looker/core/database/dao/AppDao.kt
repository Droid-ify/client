package com.looker.core.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.looker.core.database.model.AppEntity
import com.looker.core.database.model.AppExtraEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AppDao {

    @Query("SELECT * FROM apps JOIN app_extras ON apps.id = app_extras.appId")
    fun appsMinimalStream(): Flow<List<AppEntity>>

    @Query("SELECT * FROM apps WHERE authorId = :authorId")
    fun appsMinimalByAuthorStream(authorId: Long): Flow<List<AppEntity>>

    @Query("SELECT * FROM apps WHERE repoId = :repoId")
    fun appsMinimalByRepoStream(repoId: Long): Flow<List<AppEntity>>

    @Query("SELECT * FROM apps WHERE id = :id")
    suspend fun getAppById(id: Long): Map<AppEntity, AppExtraEntity>

    @Query("SELECT * FROM apps WHERE id = :id")
    fun appByIdStream(id: Long): Flow<Map<AppEntity, AppExtraEntity>>

    @Query("SELECT * FROM apps WHERE packageName = :packageName")
    fun appByPackageNameStream(packageName: String): Flow<Map<AppEntity, AppExtraEntity>>

    @Query("SELECT * FROM apps WHERE packageName = :packageName")
    suspend fun getAppByPackageName(packageName: String): Map<AppEntity, AppExtraEntity>

    @Upsert
    suspend fun upsert(app: AppEntity)

    @Upsert
    suspend fun upsertExtra(appExtra: AppExtraEntity)

}
