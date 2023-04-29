package com.looker.core.database.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import com.looker.core.common.nullIfEmpty
import com.looker.core.model.newer.*
import kotlinx.serialization.Serializable

@Entity(tableName = "apps", primaryKeys = ["repoId", "packageName"])
data class AppEntity(
	@ColumnInfo(name = "packageName")
	val packageName: String,
	@ColumnInfo(name = "repoId")
	val repoId: Long,
	val categories: List<String>,
	val antiFeatures: List<String>,
	val summary: String,
	val description: String,
	val changelog: String,
	val translation: String,
	val issueTracker: String,
	val sourceCode: String,
	val binaries: String,
	val name: String,
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
	val icon: String,
	val lastUpdated: Long,
	val localized: Map<String, LocalizedEntity>,
	val packages: List<PackageEntity>
)

@Serializable
data class LocalizedEntity(
	val description: String,
	val name: String,
	val icon: String,
	val whatsNew: String,
	val video: String,
	val phoneScreenshots: List<String>,
	val sevenInchScreenshots: List<String>,
	val tenInchScreenshots: List<String>,
	val wearScreenshots: List<String>,
	val tvScreenshots: List<String>,
	val featureGraphic: String,
	val promoGraphic: String,
	val tvBanner: String,
	val summary: String
)

fun AppEntity.toExternalModel(): App = App(
	repoId = repoId,
	categories = categories,
	antiFeatures = antiFeatures(),
	links = links(),
	metadata = Metadata(
		name = name,
		packageName = packageName.toPackageName(),
		added = added,
		description = description,
		icon = icon,
		lastUpdated = lastUpdated,
		license = license,
		suggestedVersionCode = suggestedVersionCode,
		suggestedVersionName = suggestedVersionName,
		summary = summary
	),
	screenshots = Screenshots(),
	graphics = Graphics(),
	author = author(),
	donation = donations(),
	packages = packages.map(PackageEntity::toExternalModel)
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