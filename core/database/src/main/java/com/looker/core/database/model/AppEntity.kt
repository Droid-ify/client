package com.looker.core.database.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import com.looker.core.model.new.App
import com.looker.core.model.new.Author
import com.looker.core.model.new.Donate
import com.looker.core.model.new.Localized
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
	@Serializable
	val localized: Map<String, LocalizedEntity>,
	val allowedAPKSigningKeys: List<String>,
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