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
import com.looker.droidify.data.model.AppMinimal
import com.looker.droidify.data.model.Donation
import com.looker.droidify.data.model.Metadata
import com.looker.droidify.data.model.PackageName
import com.looker.droidify.sync.v2.model.DefaultName
import com.looker.droidify.sync.v2.model.LocalizedIcon
import com.looker.droidify.sync.v2.model.LocalizedString
import com.looker.droidify.sync.v2.model.MetadataV2
import com.looker.droidify.sync.v2.model.localizedValue

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
    val name: LocalizedString,
    val icon: LocalizedIcon?,
    val preferredSigner: String?,
    val summary: LocalizedString?,
    val description: LocalizedString?,
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
    name = name,
    icon = icon,
    preferredSigner = preferredSigner,
    summary = summary,
    description = description,
    packageName = packageName,
    authorId = authorId,
    repoId = repoId,
)

fun AppEntity.toMetadata(locale: String, versions: List<VersionEntity>?): Metadata {
    val appName = name.localizedValue(locale) ?: "Unknown"
    val appSummary = summary?.localizedValue(locale) ?: ""
    val appDescription = description?.localizedValue(locale) ?: ""
    val iconUrl = icon?.localizedValue(locale)?.name ?: ""

    return Metadata(
        name = appName,
        packageName = PackageName(packageName),
        added = added,
        description = appDescription,
        icon = iconUrl,
        lastUpdated = lastUpdated,
        license = license ?: "Unknown",
        suggestedVersionCode = versions?.maxByOrNull { it.versionCode }?.versionCode ?: 0,
        suggestedVersionName = versions?.maxByOrNull { it.versionCode }?.versionName ?: "",
        summary = appSummary,
    )
}

fun AppEntity.toAppMinimal(locale: String, suggestedVersion: String): AppMinimal {
    return AppMinimal(
        appId = id.toLong(),
        name = name.localizedValue(locale) ?: "Unknown",
        summary = summary?.localizedValue(locale) ?: "",
        icon = icon?.localizedValue(locale)?.name ?: "",
        suggestedVersion = suggestedVersion,
    )
}

fun AppEntityRelations.toApp(locale: String): App {
    val mappedDonation = Donation()
    return App(
        repoId = app.repoId.toLong(),
        appId = app.id.toLong(),
        categories = categories.map<CategoryEntity, DefaultName> { it.defaultName },
        links = links?.toLinks(),
        metadata = app.toMetadata(locale, versions),
        author = author.toAuthor(),
        screenshots = screenshots?.toScreenshots(locale),
        graphics = graphics?.toGraphics(locale),
        donation = mappedDonation,
        preferredSigner = app.preferredSigner ?: "",
        packages = versions?.toPackages(locale, installed),
    )
}
