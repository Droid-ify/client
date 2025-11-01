package com.looker.droidify.data.local.model

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.ForeignKey.Companion.CASCADE
import androidx.room.Index
import androidx.room.Junction
import androidx.room.PrimaryKey
import androidx.room.Relation
import com.looker.droidify.data.model.App
import com.looker.droidify.data.model.FilePath
import com.looker.droidify.data.model.Html
import com.looker.droidify.data.model.Metadata
import com.looker.droidify.data.model.PackageName
import com.looker.droidify.sync.v2.model.MetadataV2

@Entity(
    tableName = "app",
    indices = [
        Index("authorId"),
        Index("repoId"),
        Index("packageName"),
        Index("packageName", "repoId", unique = true),
    ],
    foreignKeys = [
        ForeignKey(
            entity = RepoEntity::class,
            childColumns = ["repoId"],
            parentColumns = ["id"],
            onDelete = CASCADE,
        ),
        ForeignKey(
            entity = AuthorEntity::class,
            childColumns = ["authorId"],
            parentColumns = ["id"],
            onDelete = CASCADE,
        ),
    ],
)
data class AppEntity(
    val added: Long,
    val lastUpdated: Long,
    val license: String?,
    val preferredSigner: String?,
    val packageName: String,
    val authorId: Int,
    val repoId: Int,
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
)

data class AppEntityRelations(
    @Embedded val app: AppEntity,
    @Relation(
        parentColumn = "authorId",
        entityColumn = "id",
    )
    val author: AuthorEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "appId",
    )
    val names: List<LocalizedAppNameEntity>,
    @Relation(
        parentColumn = "id",
        entityColumn = "appId",
    )
    val summaries: List<LocalizedAppSummaryEntity>,
    @Relation(
        parentColumn = "id",
        entityColumn = "appId",
    )
    val descriptions: List<LocalizedAppDescriptionEntity>,
    @Relation(
        parentColumn = "id",
        entityColumn = "appId",
    )
    val icons: List<LocalizedAppIconEntity>,
    @Relation(
        parentColumn = "id",
        entityColumn = "appId",
    )
    val links: LinksEntity?,
    @Relation(
        parentColumn = "id",
        entityColumn = "defaultName",
        associateBy = Junction(CategoryAppRelation::class),
    )
    val categories: List<CategoryEntity>,
    @Relation(
        parentColumn = "id",
        entityColumn = "appId",
    )
    val donation: List<DonateEntity>?,
    @Relation(
        parentColumn = "id",
        entityColumn = "appId",
    )
    val graphics: List<GraphicEntity>?,
    @Relation(
        parentColumn = "id",
        entityColumn = "appId",
    )
    val screenshots: List<ScreenshotEntity>?,
    @Relation(
        parentColumn = "id",
        entityColumn = "appId",
    )
    val versions: List<VersionEntity>?,
    @Relation(
        parentColumn = "packageName",
        entityColumn = "packageName",
    )
    val installed: InstalledEntity?,
)

fun MetadataV2.appEntity(
    packageName: String,
    repoId: Int,
    authorId: Int,
) = AppEntity(
    added = added,
    lastUpdated = lastUpdated,
    license = license,
    preferredSigner = preferredSigner,
    packageName = packageName,
    authorId = authorId,
    repoId = repoId,
)

private fun AppEntity.toMetadata(
    appName: String,
    appSummary: String,
    appDescription: String,
    iconUrl: String?,
    baseAddress: String,
    versions: List<VersionEntity>?,
): Metadata {
    val suggestedVersion = versions?.maxByOrNull { it.versionCode }

    return Metadata(
        name = appName,
        packageName = PackageName(packageName),
        added = added,
        description = Html(appDescription),
        icon = FilePath(baseAddress, iconUrl),
        lastUpdated = lastUpdated,
        license = license ?: "Unknown",
        suggestedVersionCode = suggestedVersion?.versionCode ?: 0,
        suggestedVersionName = suggestedVersion?.versionName ?: "",
        summary = appSummary,
    )
}

fun AppEntityRelations.toApp(
    locale: String,
    repo: RepoEntity,
) = App(
    repoId = app.repoId.toLong(),
    appId = app.id.toLong(),
    categories = categories.filterLocalized(locale).map { it.defaultName },
    links = links?.toLinks(),
    metadata = app.toMetadata(
        baseAddress = repo.address,
        versions = versions,
        iconUrl = icons.ifEmpty { null }?.findLocale(locale)?.icon?.name,
        appName = names.findLocale(locale).name,
        appSummary = summaries.findLocale(locale).summary,
        appDescription = descriptions.findLocale(locale).description,
    ),
    author = author.toAuthor(),
    screenshots = screenshots?.toScreenshots(locale, repo.address),
    graphics = graphics?.toGraphics(locale, repo.address),
    donation = donation?.toDonation(),
    preferredSigner = app.preferredSigner ?: "",
    packages = versions?.toPackages(locale, installed),
)
