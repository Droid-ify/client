package com.looker.droidify.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import androidx.room.Upsert
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
import com.looker.droidify.data.local.model.LocalizedAppDescriptionEntity
import com.looker.droidify.data.local.model.LocalizedAppIconEntity
import com.looker.droidify.data.local.model.LocalizedAppNameEntity
import com.looker.droidify.data.local.model.LocalizedAppSummaryEntity
import com.looker.droidify.data.local.model.LocalizedRepoDescriptionEntity
import com.looker.droidify.data.local.model.LocalizedRepoIconEntity
import com.looker.droidify.data.local.model.LocalizedRepoNameEntity
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
import com.looker.droidify.sync.v2.model.LocalizedIcon
import com.looker.droidify.sync.v2.model.LocalizedString

@Dao
interface IndexDao {

    @Transaction
    suspend fun insertIndex(
        fingerprint: Fingerprint,
        index: IndexV2,
        expectedRepoId: Int = 0,
    ) {
        val repo = index.repo.repoEntity(id = expectedRepoId, fingerprint = fingerprint)
        val repoId = upsertRepo(repo)

        val antiFeatures = index.repo.antiFeatures.flatMap { (tag, feature) ->
            feature.antiFeatureEntity(tag)
        }
        val antiFeatureRepoRelations = antiFeatures.map { AntiFeatureRepoRelation(repoId, it.tag) }
        insertAntiFeatures(antiFeatures)
        insertAntiFeatureRepoRelation(antiFeatureRepoRelations)

        val categories = index.repo.categories.flatMap { (defaultName, category) ->
            category.categoryEntity(defaultName)
        }
        val categoryRepoRelations = categories.map { CategoryRepoRelation(repoId, it.defaultName) }
        insertCategories(categories)
        insertCategoryRepoRelation(categoryRepoRelations)

        val mirrors = index.repo.mirrors.map { it.mirrorEntity(repoId) }
        insertMirror(mirrors)

        insertLocalizedRepoData(
            names = index.repo.name.localizedRepoName(repoId),
            descriptions = index.repo.description.localizedRepoDescription(repoId),
            icons = index.repo.icon?.localizedRepoIcon(repoId),
        )

        index.packages.forEach { (packageName, packages) ->
            val metadata = packages.metadata
            val author = metadata.authorEntity()
            val authorId = upsertAuthor(author)

            val appEntity = packages.metadata.appEntity(
                packageName = packageName,
                repoId = repoId,
                authorId = authorId,
            )

            val existingAppId = appIdByPackageName(repoId, packageName)
            if (existingAppId != null) upsertApp(appEntity)

            val appId = existingAppId ?: insertApp(appEntity).toInt()
            val versions = packages.versionEntities(appId)
            insertVersions(versions.keys.toList())
            insertAntiFeatureAppRelation(versions.values.flatten())

            val appCategories = metadata.categories.map { CategoryAppRelation(appId, it) }
            insertCategoryAppRelation(appCategories)

            insertLocalizedAppData(
                names = metadata.name.localizedAppName(appId),
                summaries = metadata.summary?.localizedAppSummary(appId),
                descriptions = metadata.description?.localizedAppDescription(appId),
                icons = metadata.icon?.localizedAppIcon(appId),
            )

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

    @Upsert
    suspend fun upsertApp(appEntity: AppEntity)

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

    @Transaction
    suspend fun insertLocalizedRepoData(
        names: List<LocalizedRepoNameEntity>,
        descriptions: List<LocalizedRepoDescriptionEntity>,
        icons: List<LocalizedRepoIconEntity>?,
    ) {
        if (names.isNotEmpty()) insertLocalizedRepoNames(names)
        if (descriptions.isNotEmpty()) insertLocalizedRepoDescription(descriptions)
        if (!icons.isNullOrEmpty()) insertLocalizedRepoIcons(icons)
    }

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLocalizedRepoNames(names: List<LocalizedRepoNameEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLocalizedRepoDescription(descriptions: List<LocalizedRepoDescriptionEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLocalizedRepoIcons(icons: List<LocalizedRepoIconEntity>)

    @Transaction
    suspend fun insertLocalizedAppData(
        names: List<LocalizedAppNameEntity>,
        summaries: List<LocalizedAppSummaryEntity>?,
        descriptions: List<LocalizedAppDescriptionEntity>?,
        icons: List<LocalizedAppIconEntity>?,
    ) {
        if (names.isNotEmpty()) insertLocalizedAppNames(names)
        if (!summaries.isNullOrEmpty()) insertLocalizedAppSummaries(summaries)
        if (!descriptions.isNullOrEmpty()) insertLocalizedAppDescriptions(descriptions)
        if (!icons.isNullOrEmpty()) insertLocalizedAppIcons(icons)
    }

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLocalizedAppNames(names: List<LocalizedAppNameEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLocalizedAppSummaries(summaries: List<LocalizedAppSummaryEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLocalizedAppDescriptions(descriptions: List<LocalizedAppDescriptionEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLocalizedAppIcons(icons: List<LocalizedAppIconEntity>)

}

fun LocalizedString.localizedRepoName(repoId: Int) =
    map { LocalizedRepoNameEntity(repoId, it.key, it.value) }

fun LocalizedString.localizedRepoDescription(repoId: Int) =
    map { LocalizedRepoDescriptionEntity(repoId, it.key, it.value) }

fun LocalizedIcon.localizedRepoIcon(repoId: Int) =
    map { LocalizedRepoIconEntity(repoId, it.key, it.value) }

fun LocalizedString.localizedAppName(appId: Int) =
    map { LocalizedAppNameEntity(appId, it.key, it.value) }

fun LocalizedString.localizedAppSummary(appId: Int) =
    map { LocalizedAppSummaryEntity(appId, it.key, it.value) }

fun LocalizedString.localizedAppDescription(appId: Int) =
    map { LocalizedAppDescriptionEntity(appId, it.key, it.value) }

fun LocalizedIcon.localizedAppIcon(appId: Int) =
    map { LocalizedAppIconEntity(appId, it.key, it.value) }
