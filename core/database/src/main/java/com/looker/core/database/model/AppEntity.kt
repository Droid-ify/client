package com.looker.core.database.model

import androidx.core.os.LocaleListCompat
import androidx.room.ColumnInfo
import androidx.room.Entity
import com.looker.core.common.nullIfEmpty
import com.looker.core.database.utils.localizedValue
import com.looker.core.model.newer.*

typealias LocalizedString = Map<String, String>
typealias LocalizedList = Map<String, List<String>>

@Entity(tableName = "apps", primaryKeys = ["repoId", "packageName"])
data class AppEntity(
	@ColumnInfo(name = "packageName")
	val packageName: String,
	@ColumnInfo(name = "repoId")
	val repoId: Long,
	val categories: List<String>,
	val antiFeatures: List<String>,
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
	val suggestedVersionCode: Int,
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

fun AppEntity.toExternalModel(locale: LocaleListCompat): App = App(
	repoId = repoId,
	categories = categories,
	antiFeatures = antiFeatures(),
	links = links(),
	metadata = Metadata(
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
	),
	screenshots = Screenshots(
		phone = phoneScreenshots.localizedValue(locale) ?: emptyList(),
		sevenInch = sevenInchScreenshots.localizedValue(locale) ?: emptyList(),
		tenInch = tenInchScreenshots.localizedValue(locale) ?: emptyList(),
		tv = tvScreenshots.localizedValue(locale) ?: emptyList(),
		wear = wearScreenshots.localizedValue(locale) ?: emptyList()
	),
	graphics = Graphics(
		featureGraphic = featureGraphic.localizedValue(locale) ?: "",
		promoGraphic = promoGraphic.localizedValue(locale) ?: "",
		tvBanner = tvBanner.localizedValue(locale) ?: "",
		video = video.localizedValue(locale) ?: ""
	),
	author = author(),
	donation = donations(),
	packages = packages.map { it.toExternalModel(locale) }
)

private fun AppEntity.antiFeatures(): Set<AntiFeatures> = antiFeatures.map {
	when (it) {
		"Ads" -> AntiFeatures.Ads
		"ApplicationDebuggable" -> AntiFeatures.Debug
		"DisabledAlgorithm" -> AntiFeatures.UnsafeSigning
		"KnownVuln" -> AntiFeatures.Vulnerable
		"NoSourceSince" -> AntiFeatures.SourceUnavailable
		"NonFreeAdd" -> AntiFeatures.Promotion
		"NonFreeAssets" -> AntiFeatures.CopyrightedAssets
		"NonFreeDep" -> AntiFeatures.NonFreeLibraries
		"NonFreeNet" -> AntiFeatures.NonFreeNetwork
		"Tracking" -> AntiFeatures.Tracking
		"UpstreamNonFree" -> AntiFeatures.InAppPurchase
		else -> AntiFeatures.Unknown(it)
	}
}.toSet()

private fun AppEntity.author(): Author = Author(
	name = authorName,
	email = authorEmail,
	web = authorWebSite,
)

private fun AppEntity.donations(): Donation = Donation(
	regularUrl = donate.nullIfEmpty(),
	bitcoinAddress = bitcoin.nullIfEmpty(),
	flattrId = flattrID.nullIfEmpty(),
	liteCoinAddress = litecoin.nullIfEmpty(),
	openCollectiveId = openCollective.nullIfEmpty(),
	librePayId = liberapayID.nullIfEmpty(),
	librePayAddress = liberapay.nullIfEmpty(),
)

private fun AppEntity.links(): Links = Links(
	changelog = changelog,
	issueTracker = issueTracker,
	sourceCode = sourceCode,
	translation = translation,
	webSite = webSite
)