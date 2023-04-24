package com.looker.core.database.model

import androidx.room.ColumnInfo
import androidx.room.Entity
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
	val authorPhone: String,
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
	antiFeatures = antiFeatures,
	links = Links(
		changelog = changelog,
		issueTracker = issueTracker,
		sourceCode = sourceCode,
		translation = translation,
		webSite = webSite
	),
	license = license,
	metadata = Metadata(
		name = name,
		description = description,
		summary = summary,
		packageName = packageName.toPackageName(),
		icon = icon,
		added = added,
		lastUpdated = lastUpdated,
		suggestedVersionName = suggestedVersionName,
		suggestedVersionCode = suggestedVersionCode,
		whatsNew = localized["en"]?.whatsNew ?: ""
	),
	screenshots = Screenshots(),
	graphics = Graphics(),
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
	packages = packages.map(PackageEntity::toExternalModel)
)