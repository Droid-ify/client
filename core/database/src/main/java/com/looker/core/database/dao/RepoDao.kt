package com.looker.core.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Query
import androidx.room.Upsert
import com.looker.core.database.model.AntiFeatureEntity
import com.looker.core.database.model.CategoryEntity
import com.looker.core.database.model.RepoCategoryAntiFeatures
import com.looker.core.database.model.RepoEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RepoDao {

    @Query("SELECT * FROM category WHERE repoId = :repoId")
    suspend fun getCategoriesByRepoId(repoId: Long): List<CategoryEntity>

    @Query("SELECT * FROM anti_feature WHERE repoId = :repoId")
    suspend fun getAntiFeaturesByRepoId(repoId: Long): List<AntiFeatureEntity>

    @Query("SELECT * FROM repos")
    fun reposFullStream(): Flow<List<RepoCategoryAntiFeatures>>

    @Query("SELECT * FROM repos")
    fun reposStream(): Flow<List<RepoEntity>>

    @Query("SELECT * FROM repos WHERE id = :id")
    fun repoByIdStream(id: Long): Flow<RepoEntity?>

    @Query("SELECT * FROM repos WHERE id = :id")
    suspend fun getRepoById(id: Long): RepoEntity?

    @Upsert
    suspend fun upsert(repo: RepoEntity)

    @Delete
    suspend fun delete(id: Long)

}
