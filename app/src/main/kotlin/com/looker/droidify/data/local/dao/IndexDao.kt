package com.looker.droidify.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.looker.droidify.data.local.model.AntiFeatureAppRelation
import com.looker.droidify.data.local.model.AntiFeatureEntity
import com.looker.droidify.data.local.model.AntiFeatureRepoRelation
import com.looker.droidify.data.local.model.AppEntity
import com.looker.droidify.data.local.model.AuthorEntity
import com.looker.droidify.data.local.model.CategoryAppRelation
import com.looker.droidify.data.local.model.CategoryEntity
import com.looker.droidify.data.local.model.CategoryRepoRelation
import com.looker.droidify.data.local.model.DonateEntity
import com.looker.droidify.data.local.model.GraphicEntity
import com.looker.droidify.data.local.model.LinksEntity
import com.looker.droidify.data.local.model.MirrorEntity
import com.looker.droidify.data.local.model.RepoEntity
import com.looker.droidify.data.local.model.ScreenshotEntity
import com.looker.droidify.data.local.model.VersionEntity
import com.looker.droidify.data.local.model.antiFeatureEntity
import com.looker.droidify.data.local.model.appEntity
import com.looker.droidify.data.local.model.authorEntity
import com.looker.droidify.data.local.model.categoryEntity
import com.looker.droidify.data.local.model.donateEntity
import com.looker.droidify.data.local.model.linkEntity
import com.looker.droidify.data.local.model.localizedGraphics
import com.looker.droidify.data.local.model.localizedScreenshots
import com.looker.droidify.data.local.model.mirrorEntity
import com.looker.droidify.data.local.model.repoEntity
import com.looker.droidify.data.local.model.versionEntities
import com.looker.droidify.data.model.Fingerprint
import com.looker.droidify.sync.v2.model.IndexV2

@Dao
interface IndexDao {

    @Transaction
    suspend fun insertIndex(
        fingerprint: Fingerprint,
        index: IndexV2,
        expectedRepoId: Int = 0,
    ) {
        logQuery(
            "fingerprint" to fingerprint,
            "expectedRepoId" to expectedRepoId,
            "repo" to index.repo.name["en-US"],
            "no of apps to add" to index.packages.size,
        )
        val repoId = upsertRepo(
            index.repo.repoEntity(
                id = expectedRepoId,
                fingerprint = fingerprint,
            ),
        )
        val antiFeatures = index.repo.antiFeatures.flatMap { (tag, feature) ->
            feature.antiFeatureEntity(tag)
        }
        val categories = index.repo.categories.flatMap { (defaultName, category) ->
            category.categoryEntity(defaultName)
        }
        val antiFeatureRepoRelations = antiFeatures.map { AntiFeatureRepoRelation(repoId, it.tag) }
        val categoryRepoRelations = categories.map { CategoryRepoRelation(repoId, it.defaultName) }
        val mirrors = index.repo.mirrors.map { it.mirrorEntity(repoId) }
        insertAntiFeatures(antiFeatures)
        insertAntiFeatureRepoRelation(antiFeatureRepoRelations)
        insertCategories(categories)
        insertCategoryRepoRelation(categoryRepoRelations)
        insertMirror(mirrors)
        index.packages.forEach { (packageName, packages) ->
            val metadata = packages.metadata
            val author = metadata.authorEntity()
            val authorId = upsertAuthor(author)
            val appId = appIdByPackageName(repoId, packageName) ?: insertApp(
                appEntity = metadata.appEntity(
                    packageName = packageName,
                    repoId = repoId,
                    authorId = authorId,
                ),
            ).toInt()
            val versions = packages.versionEntities(appId)
            insertVersions(versions.keys.toList())
            insertAntiFeatureAppRelation(versions.values.flatten())
            val appCategories = packages.metadata.categories.map { CategoryAppRelation(appId, it) }
            insertCategoryAppRelation(appCategories)
            metadata.linkEntity(appId)?.let { insertLink(it) }
            metadata.screenshots?.localizedScreenshots(appId)?.let { insertScreenshots(it) }
            metadata.localizedGraphics(appId)?.let { insertGraphics(it) }
            metadata.donateEntity(appId)?.let { insertDonate(it) }
        }
    }

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertRepo(repoEntity: RepoEntity): Long

    @Update(onConflict = OnConflictStrategy.REPLACE)
    suspend fun updateRepo(repoEntity: RepoEntity)

    @Transaction
    suspend fun upsertRepo(repoEntity: RepoEntity): Int {
        val id = insertRepo(repoEntity)
        return if (id == -1L) {
            repoEntity.also { updateRepo(it) }.id
        } else {
            id.toInt()
        }
    }

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMirror(mirrors: List<MirrorEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAntiFeatures(antiFeatures: List<AntiFeatureEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategories(categories: List<CategoryEntity>)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAntiFeatureRepoRelation(crossRef: List<AntiFeatureRepoRelation>)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertCategoryRepoRelation(crossRef: List<CategoryRepoRelation>)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertApp(appEntity: AppEntity): Long

    @Query("SELECT id FROM app WHERE packageName = :packageName AND repoId = :repoId LIMIT 1")
    suspend fun appIdByPackageName(repoId: Int, packageName: String): Int?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAuthor(authorEntity: AuthorEntity): Long

    @Query(
        """
        SELECT id FROM author
        WHERE
            (:email IS NULL AND email IS NULL OR email = :email) AND
            (:name IS NULL AND name IS NULL OR name = :name COLLATE NOCASE) AND
            (:website IS NULL AND website IS NULL OR website = :website COLLATE NOCASE)
        LIMIT 1
        """,
    )
    suspend fun authorId(
        email: String?,
        name: String?,
        website: String?,
    ): Int?

    @Transaction
    suspend fun upsertAuthor(authorEntity: AuthorEntity): Int {
        val id = insertAuthor(authorEntity)
        return if (id == -1L) {
            authorId(
                email = authorEntity.email,
                name = authorEntity.name,
                website = authorEntity.website,
            )!!
        } else {
            id.toInt()
        }
    }

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertScreenshots(screenshotEntity: List<ScreenshotEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLink(linksEntity: LinksEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGraphics(graphicEntity: List<GraphicEntity>)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertDonate(donateEntity: List<DonateEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVersions(versions: List<VersionEntity>)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertCategoryAppRelation(crossRef: List<CategoryAppRelation>)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAntiFeatureAppRelation(crossRef: List<AntiFeatureAppRelation>)

}
