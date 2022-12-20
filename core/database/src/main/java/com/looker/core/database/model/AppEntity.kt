package com.looker.core.database.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import com.looker.core.model.new.App
import com.looker.core.model.new.Author
import com.looker.core.model.new.Donate
import com.looker.core.model.new.Localized
import com.looker.core.model.new.Metadata
import com.looker.core.model.new.toPackageName
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
	val authorPhone: String,
	val donate: String,
	val liberapayID: String,
	val liberapay: String,
	val openCollective: String,
	val bitcoin: String,
	val litecoin: String,
	val flattrID: String,
	val suggestedVersionName: String,
	val suggestedVersionCode: String,
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
	antiFeatures = antiFeatures,
	translation = translation,
	issueTracker = issueTracker,
	sourceCode = sourceCode,
	binaries = binaries,
	license = license,
	webSite = webSite,
	metadata = Metadata(
		name = name,
		description = description,
		summary = summary,
		packageName = packageName.toPackageName(),
		icon = icon,
		changelog = changelog,
		added = added,
		lastUpdated = lastUpdated,
		suggestedVersionName = suggestedVersionName,
		suggestedVersionCode = suggestedVersionCode
	),
	author = Author(
		name = authorName,
		email = authorEmail,
		web = authorWebSite,
		phone = authorPhone
	),
	donation = buildSet {
		when {
			openCollective.isNotBlank() -> add(Donate.OpenCollective(openCollective))
			flattrID.isNotBlank() -> add(Donate.Flattr(flattrID))
			litecoin.isNotBlank() -> add(Donate.Litecoin(litecoin))
			bitcoin.isNotBlank() -> add(Donate.Bitcoin(bitcoin))
			liberapay.isNotBlank() && liberapayID.isNotBlank() ->
				add(Donate.Liberapay(liberapayID, liberapay))
			donate.isNotBlank() -> add(Donate.Regular(donate))
		}
	},
	localized = localized.mapValues { it.value.toExternalModel() },
	packages = packages.map(PackageEntity::toExternalModel)
)

fun LocalizedEntity.toExternalModel(): Localized = Localized(
	description = description,
	name = name,
	icon = icon,
	whatsNew = whatsNew,
	video = video,
	phoneScreenshots = phoneScreenshots,
	sevenInchScreenshots = sevenInchScreenshots,
	tenInchScreenshots = tenInchScreenshots,
	wearScreenshots = wearScreenshots,
	tvScreenshots = tvScreenshots,
	featureGraphic = featureGraphic,
	promoGraphic = promoGraphic,
	tvBanner = tvBanner,
	summary = summary
)