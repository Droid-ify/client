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
        insertRepoScopeData(repoId, index)

        val packageEntries = index.packages.entries.toList()

        val authorIdsByAuthor = mutableMapOf<AuthorEntity, Int>()
        packageEntries.asSequence()
            .map { it.value.metadata.authorEntity() }
            .distinct()
            .forEach { author ->
                val id = upsertAuthor(author)
                authorIdsByAuthor[author] = id
            }

        val appEntities = packageEntries.map { (packageName, packages) ->
            val authorId = authorIdsByAuthor[packages.metadata.authorEntity()]!!
            packages.metadata.appEntity(
                packageName = packageName,
                repoId = repoId,
                authorId = authorId,
            )
        }

        val packageNames = packageEntries.map { it.key }
        val existing = appIdsByPackageNames(repoId, packageNames)
        val existingIdByPackage = mutableMapOf<String, Int>().apply {
            existing.forEach { put(it.packageName, it.id) }
        }

        val toUpdate = appEntities.filter { existingIdByPackage.containsKey(it.packageName) }
        val toInsert = appEntities.filter { !existingIdByPackage.containsKey(it.packageName) }

        if (toUpdate.isNotEmpty()) upsertApps(toUpdate)
        val insertedIds: Map<String, Int> = if (toInsert.isNotEmpty()) {
            val result = insertApps(toInsert)
            toInsert.mapIndexed { idx, entity -> entity.packageName to result[idx].toInt() }.toMap()
        } else emptyMap()

        val appIdByPackage: Map<String, Int> = existingIdByPackage + insertedIds

        val allVersions = mutableListOf<VersionEntity>()
        val allAntiFeatureAppRelations = mutableListOf<AntiFeatureAppRelation>()
        val allCategoryAppRelations = mutableListOf<CategoryAppRelation>()

        val allAppNames = mutableListOf<LocalizedAppNameEntity>()
        val allAppSummaries = mutableListOf<LocalizedAppSummaryEntity>()
        val allAppDescriptions = mutableListOf<LocalizedAppDescriptionEntity>()
        val allAppIcons = mutableListOf<LocalizedAppIconEntity>()

        val allLinks = mutableListOf<LinksEntity>()
        val allScreenshots = mutableListOf<ScreenshotEntity>()
        val allGraphics = mutableListOf<GraphicEntity>()
        val allDonations = mutableListOf<DonateEntity>()

        packageEntries.forEach { (packageName, packages) ->
            val appId = appIdByPackage.getValue(packageName)
            val metadata = packages.metadata

            val versionsMap = packages.versionEntities(appId)
            allVersions += versionsMap.keys
            allAntiFeatureAppRelations += versionsMap.values.flatten()

            allCategoryAppRelations += metadata.categories.map { CategoryAppRelation(appId, it) }

            allAppNames += metadata.name.localizedAppName(appId)
            metadata.summary?.localizedAppSummary(appId)?.let { allAppSummaries += it }
            metadata.description?.localizedAppDescription(appId)?.let { allAppDescriptions += it }
            metadata.icon?.localizedAppIcon(appId)?.let { allAppIcons += it }

            metadata.linkEntity(appId)?.let { allLinks += it }
            metadata.screenshots?.localizedScreenshots(appId)?.let { allScreenshots += it }
            metadata.localizedGraphics(appId)?.let { allGraphics += it }
            metadata.donateEntity(appId)?.let { allDonations += it }
        }

        if (allVersions.isNotEmpty()) insertVersions(allVersions)
        if (allAntiFeatureAppRelations.isNotEmpty()) insertAntiFeatureAppRelation(
            allAntiFeatureAppRelations
        )
        if (allCategoryAppRelations.isNotEmpty()) insertCategoryAppRelation(allCategoryAppRelations)

        insertLocalizedAppData(
            names = allAppNames,
            summaries = allAppSummaries.ifEmpty { null },
            descriptions = allAppDescriptions.ifEmpty { null },
            icons = allAppIcons.ifEmpty { null },
        )

        if (allLinks.isNotEmpty()) insertLinks(allLinks)
        if (allScreenshots.isNotEmpty()) insertScreenshots(allScreenshots)
        if (allGraphics.isNotEmpty()) insertGraphics(allGraphics)
        if (allDonations.isNotEmpty()) insertDonate(allDonations)
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

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertApps(apps: List<AppEntity>): List<Long>

    @Upsert
    suspend fun upsertApp(appEntity: AppEntity)

    @Upsert
    suspend fun upsertApps(apps: List<AppEntity>)

    @Query("SELECT id FROM app WHERE packageName = :packageName AND repoId = :repoId LIMIT 1")
    suspend fun appIdByPackageName(repoId: Int, packageName: String): Int?

    @Query("SELECT id, packageName FROM app WHERE repoId = :repoId AND packageName IN (:packageNames)")
    suspend fun appIdsByPackageNames(repoId: Int, packageNames: List<String>): List<AppIdPackage>

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

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLinks(links: List<LinksEntity>)

    @Transaction
    suspend fun insertRepoScopeData(repoId: Int, index: IndexV2) {
        val antiFeatures = index.repo.antiFeatures.flatMap { (tag, feature) ->
            feature.antiFeatureEntity(tag)
        }
        val antiFeatureRepoRelations = antiFeatures.map { AntiFeatureRepoRelation(repoId, it.tag) }
        if (antiFeatures.isNotEmpty()) insertAntiFeatures(antiFeatures)
        if (antiFeatureRepoRelations.isNotEmpty()) insertAntiFeatureRepoRelation(
            antiFeatureRepoRelations
        )

        val categories = index.repo.categories.flatMap { (defaultName, category) ->
            category.categoryEntity(defaultName)
        }
        val categoryRepoRelations = categories.map { CategoryRepoRelation(repoId, it.defaultName) }
        if (categories.isNotEmpty()) insertCategories(categories)
        if (categoryRepoRelations.isNotEmpty()) insertCategoryRepoRelation(categoryRepoRelations)

        val mirrors = index.repo.mirrors.map { it.mirrorEntity(repoId) }
        if (mirrors.isNotEmpty()) insertMirror(mirrors)

        insertLocalizedRepoData(
            names = index.repo.name.localizedRepoName(repoId),
            descriptions = index.repo.description.localizedRepoDescription(repoId),
            icons = index.repo.icon?.localizedRepoIcon(repoId),
        )
    }
}

fun LocalizedString.localizedRepoName(repoId: Int) =
    map { LocalizedRepoNameEntity(repoId, it.key, it.value) }

fun LocalizedString.localizedRepoDescription(repoId: Int) =
    map { LocalizedRepoDescriptionEntity(repoId, it.key, it.value.replace("\n", "<br/>")) }

fun LocalizedIcon.localizedRepoIcon(repoId: Int) =
    map { LocalizedRepoIconEntity(repoId, it.key, it.value) }

fun LocalizedString.localizedAppName(appId: Int) =
    map { LocalizedAppNameEntity(appId, it.key, it.value) }

fun LocalizedString.localizedAppSummary(appId: Int) =
    map { LocalizedAppSummaryEntity(appId, it.key, it.value) }

fun LocalizedString.localizedAppDescription(appId: Int) =
    map { LocalizedAppDescriptionEntity(appId, it.key, it.value.replace("\n", "<br/>")) }

fun LocalizedIcon.localizedAppIcon(appId: Int) =
    map { LocalizedAppIconEntity(appId, it.key, it.value) }

data class AppIdPackage(
    val id: Int,
    val packageName: String,
)
