package com.looker.core.database.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import com.looker.core.common.nullIfEmpty
import com.looker.core.domain.model.toPackageName
import com.looker.core.database.utils.localizedValue
import com.looker.core.domain.model.App
import com.looker.core.domain.model.Author
import com.looker.core.domain.model.Donation
import com.looker.core.domain.model.Graphics
import com.looker.core.domain.model.Links
import com.looker.core.domain.model.Metadata
import com.looker.core.domain.model.Screenshots

internal typealias LocalizedString = Map<String, String>
internal typealias LocalizedList = Map<String, List<String>>

@Entity(tableName = "apps", primaryKeys = ["repoId", "packageName"])
data class AppEntity(
    @ColumnInfo(name = "packageName")
    val packageName: String,
    @ColumnInfo(name = "repoId")
    val repoId: Long,
    val categories: List<String>,
    val summary: LocalizedString,
    val description: LocalizedString,
    val changelog: String,
    val translation: String,
    val issueTracker: String,
    val sourceCode: String,
    val binaries: String,
    val name: LocalizedString,
    val authorName: String,
    val authorEmail: String,
    val authorWebSite: String,
    val donate: String,
    val liberapayID: String,
    val liberapay: String,
    val openCollective: String,
    val bitcoin: String,
    val litecoin: String,
    val flattrID: String,
    val suggestedVersionName: String,
    val suggestedVersionCode: Long,
    val license: String,
    val webSite: String,
    val added: Long,
    val icon: LocalizedString,
    val phoneScreenshots: LocalizedList,
    val sevenInchScreenshots: LocalizedList,
    val tenInchScreenshots: LocalizedList,
    val wearScreenshots: LocalizedList,
    val tvScreenshots: LocalizedList,
    val featureGraphic: LocalizedString,
    val promoGraphic: LocalizedString,
    val tvBanner: LocalizedString,
    val video: LocalizedString,
    val lastUpdated: Long,
    val packages: List<PackageEntity>
)

fun AppEntity.toExternal(locale: String, installed: PackageEntity? = null): App = App(
    repoId = repoId,
    categories = categories,
    links = links(),
    metadata = metadata(locale),
    screenshots = screenshots(locale),
    graphics = graphics(locale),
    author = author(),
    donation = donations(),
    packages = packages.toExternal(locale) { it == installed }
)

fun List<AppEntity>.toExternal(
    locale: String,
    isInstalled: (AppEntity) -> PackageEntity?
): List<App> = map {
    it.toExternal(locale, isInstalled(it))
}

private fun AppEntity.author(): Author = Author(
    name = authorName,
    email = authorEmail,
    web = authorWebSite
)

private fun AppEntity.donations(): Donation = Donation(
    regularUrl = donate.nullIfEmpty(),
    bitcoinAddress = bitcoin.nullIfEmpty(),
    flattrId = flattrID.nullIfEmpty(),
    liteCoinAddress = litecoin.nullIfEmpty(),
    openCollectiveId = openCollective.nullIfEmpty(),
    librePayId = liberapayID.nullIfEmpty(),
    librePayAddress = liberapay.nullIfEmpty()
)

private fun AppEntity.graphics(locale: String): Graphics = Graphics(
    featureGraphic = featureGraphic.localizedValue(locale) ?: "",
    promoGraphic = promoGraphic.localizedValue(locale) ?: "",
    tvBanner = tvBanner.localizedValue(locale) ?: "",
    video = video.localizedValue(locale) ?: ""
)

private fun AppEntity.links(): Links = Links(
    changelog = changelog,
    issueTracker = issueTracker,
    sourceCode = sourceCode,
    translation = translation,
    webSite = webSite
)

private fun AppEntity.metadata(locale: String): Metadata = Metadata(
    name = name.localizedValue(locale) ?: "",
    packageName = packageName.toPackageName(),
    added = added,
    description = description.localizedValue(locale) ?: "",
    icon = icon.localizedValue(locale) ?: "",
    lastUpdated = lastUpdated,
    license = license,
    suggestedVersionCode = suggestedVersionCode,
    suggestedVersionName = suggestedVersionName,
    summary = summary.localizedValue(locale) ?: ""
)

private fun AppEntity.screenshots(locale: String): Screenshots = Screenshots(
    phone = phoneScreenshots.localizedValue(locale) ?: emptyList(),
    sevenInch = sevenInchScreenshots.localizedValue(locale) ?: emptyList(),
    tenInch = tenInchScreenshots.localizedValue(locale) ?: emptyList(),
    tv = tvScreenshots.localizedValue(locale) ?: emptyList(),
    wear = wearScreenshots.localizedValue(locale) ?: emptyList()
)
