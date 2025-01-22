package com.looker.core.database.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.ForeignKey.Companion.CASCADE
import androidx.room.Index
import androidx.room.PrimaryKey
import com.looker.core.common.nullIfEmpty
import com.looker.core.database.utils.localizedValue
import com.looker.core.domain.model.App
import com.looker.core.domain.model.AppMinimal
import com.looker.core.domain.model.Author
import com.looker.core.domain.model.Donation
import com.looker.core.domain.model.Graphics
import com.looker.core.domain.model.Links
import com.looker.core.domain.model.Metadata
import com.looker.core.domain.model.Package
import com.looker.core.domain.model.Screenshots
import com.looker.core.domain.model.toPackageName

internal typealias LocalizedString = Map<String, String>
internal typealias LocalizedList = Map<String, List<String>>

@Entity(
    tableName = "apps",
    foreignKeys = [
        ForeignKey(
            entity = RepoEntity::class,
            parentColumns = ["id"],
            childColumns = ["repoId"],
            onDelete = CASCADE,
        ),
        ForeignKey(
            entity = AuthorEntity::class,
            parentColumns = ["id"],
            childColumns = ["authorId"],
        ),
    ],
    indices = [
        // Sorting index
        Index("repoId", "added", "lastUpdated", "name"),
        // Searching index
        Index("packageName", "summary", "description"),
    ],
)
data class AppEntity(
    val packageName: String,
    val categories: List<String>,
    val icon: LocalizedString,
    val name: LocalizedString,
    val summary: LocalizedString,
    val description: LocalizedString,
    val suggestedVersionName: String,
    val suggestedVersionCode: Long,
    val license: String,
    val added: Long,
    val lastUpdated: Long,
    val authorId: Long,
    val repoId: Long,
    @PrimaryKey(autoGenerate = true)
    val id: Long = -1L,
)

@Entity(
    tableName = "app_extras",
    foreignKeys = [
        ForeignKey(
            entity = AppEntity::class,
            parentColumns = ["id"],
            childColumns = ["appId"],
            onDelete = CASCADE,
        ),
    ],
    indices = [Index("appId")],
)
data class AppExtraEntity(
    val changelog: String,
    val translation: String,
    val issueTracker: String,
    val sourceCode: String,
    val binaries: String,
    val webSite: String,
    val appId: Long,
    @PrimaryKey(autoGenerate = true)
    val id: Long = -1L,
)

fun AppEntity.minimal(locale: String) = AppMinimal(
    appId = id,
    name = name.localizedValue(locale) ?: "",
    summary = summary.localizedValue(locale) ?: "",
    icon = icon.localizedValue(locale) ?: "",
    suggestedVersion = suggestedVersionName,
)

fun List<AppEntity>.toAppMinimal(locale: String) = map {
    it.minimal(locale)
}

fun AppEntity.toExternal(
    locale: String,
    donationEntity: DonationEntity,
    extraEntity: AppExtraEntity,
    authorEntity: AuthorEntity,
    graphicEntity: GraphicEntity,
    screenshotEntity: ScreenshotEntity,
    packages: List<Package>
): App = App(
    repoId = repoId,
    appId = id,
    categories = categories,
    metadata = metadata(locale),
    links = extraEntity.links(),
    screenshots = screenshotEntity.screenshots(),
    graphics = graphicEntity.graphics(),
    author = authorEntity.author(),
    donation = donationEntity.donations(),
    packages = packages,
)

fun List<AppEntity>.toExternal(
    locale: String,
    donationEntity: DonationEntity,
    extraEntity: AppExtraEntity,
    authorEntity: AuthorEntity,
    graphicEntity: GraphicEntity,
    screenshotEntity: ScreenshotEntity,
    packages: List<Package>
): List<App> = map {
    it.toExternal(
        locale = locale,
        donationEntity = donationEntity,
        extraEntity = extraEntity,
        authorEntity = authorEntity,
        graphicEntity = graphicEntity,
        screenshotEntity = screenshotEntity,
        packages = packages,
    )
}

private fun AuthorEntity.author(): Author = Author(
    name = name,
    email = email,
    web = web,
    id = id,
)

private fun DonationEntity.donations(): Donation = Donation(
    regularUrl = regularUrl.nullIfEmpty(),
    bitcoinAddress = bitcoin.nullIfEmpty(),
    flattrId = flattrID.nullIfEmpty(),
    liteCoinAddress = litecoin.nullIfEmpty(),
    openCollectiveId = openCollective.nullIfEmpty(),
    librePayId = liberapay.nullIfEmpty(),
)

private fun GraphicEntity.graphics(): Graphics = Graphics(
    featureGraphic = featureGraphic,
    promoGraphic = promoGraphic,
    tvBanner = tvBanner,
    video = video,
)

private fun AppExtraEntity.links(): Links = Links(
    changelog = changelog,
    issueTracker = issueTracker,
    sourceCode = sourceCode,
    translation = translation,
    webSite = webSite
)

private fun AppEntity.metadata(locale: String): Metadata = Metadata(
    packageName = packageName.toPackageName(),
    icon = icon.localizedValue(locale) ?: "",
    name = name.localizedValue(locale) ?: "",
    summary = summary.localizedValue(locale) ?: "",
    description = description.localizedValue(locale) ?: "",
    added = added,
    lastUpdated = lastUpdated,
    license = license,
    suggestedVersionCode = suggestedVersionCode,
    suggestedVersionName = suggestedVersionName,
)

private fun ScreenshotEntity.screenshots(): Screenshots = Screenshots(
    phone = phone,
    sevenInch = sevenInch,
    tenInch = tenInch,
    tv = tv,
    wear = wear
)
