package com.looker.droidify.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.looker.droidify.data.local.model.AntiFeatureEntity
import com.looker.droidify.data.local.model.AntiFeatureRepoRelation
import com.looker.droidify.data.local.model.CategoryEntity
import com.looker.droidify.data.local.model.CategoryRepoRelation
import com.looker.droidify.data.local.model.MirrorEntity
import com.looker.droidify.data.local.model.RepoEntity
import com.looker.droidify.data.local.model.antiFeatureEntity
import com.looker.droidify.data.local.model.categoryEntity
import com.looker.droidify.data.local.model.mirrorEntity
import com.looker.droidify.data.local.model.repoEntity
import com.looker.droidify.domain.model.Fingerprint
import com.looker.droidify.sync.v2.model.RepoV2
import kotlinx.coroutines.flow.Flow

@Dao
interface RepoDao {

    @Query("SELECT * FROM repository")
    fun stream(): Flow<List<RepoEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(repoEntity: RepoEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMirror(mirrors: List<MirrorEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAntiFeatures(antiFeatures: List<AntiFeatureEntity>)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAntiFeatureCrossRef(crossRef: List<AntiFeatureRepoRelation>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategories(categories: List<CategoryEntity>)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertCategoryCrossRef(crossRef: List<CategoryRepoRelation>)

    @Transaction
    suspend fun insertRepo(
        repo: RepoV2,
        fingerprint: Fingerprint,
        username: String? = null,
        password: String? = null,
        id: Int = 0,
    ): Int {
        val repoId = insert(
            repo.repoEntity(
                id = id,
                fingerprint = fingerprint,
                username = username,
                password = password,
            ),
        ).toInt()
        val antiFeatures = repo.antiFeatures.flatMap { (tag, feature) ->
            feature.antiFeatureEntity(tag)
        }
        val antiFeatureCrossRef = antiFeatures.map {
            AntiFeatureRepoRelation(repoId, it.tag)
        }
        val categories = repo.categories.flatMap { (defaultName, category) ->
            category.categoryEntity(defaultName)
        }
        val categoryCrossRef = categories.map {
            CategoryRepoRelation(repoId, it.defaultName)
        }
        val mirrors = repo.mirrors.map { it.mirrorEntity(repoId) }
        insertAntiFeatures(antiFeatures)
        insertAntiFeatureCrossRef(antiFeatureCrossRef)
        insertCategories(categories)
        insertCategoryCrossRef(categoryCrossRef)
        insertMirror(mirrors)
        return repoId
    }

    @Query("DELETE FROM repository WHERE id = :id")
    suspend fun delete(id: Int)

}
