package com.looker.droidify.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import com.looker.droidify.data.local.model.CategoryEntity
import com.looker.droidify.data.local.model.MirrorEntity
import com.looker.droidify.data.local.model.RepoEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RepoDao {

    @Query("SELECT * FROM repository")
    fun stream(): Flow<List<RepoEntity>>

    @Query("SELECT * FROM repository WHERE id = :repoId")
    fun repo(repoId: Int): Flow<RepoEntity?>

    @Query("SELECT * FROM repository WHERE id = :repoId")
    suspend fun getRepo(repoId: Int): RepoEntity?

    @Query("SELECT * FROM category GROUP BY category.defaultName")
    fun categories(): Flow<List<CategoryEntity>>

    // Query category entity using CategoryRepoRelation class
    @Query(
        """
        SELECT * FROM category
        JOIN category_repo_relation ON category.defaultName = category_repo_relation.defaultName
        WHERE category_repo_relation.id = :repoId
        """
    )
    fun categoriesByRepoId(repoId: Int): Flow<List<CategoryEntity>>

    @Query("SELECT * FROM mirror WHERE repoId = :repoId")
    suspend fun mirrors(repoId: Int): List<MirrorEntity>

    @Query("SELECT * FROM mirror")
    fun mirrors(): Flow<List<MirrorEntity>>

    @Query("DELETE FROM repository WHERE id = :id")
    suspend fun delete(id: Int)

}
