package com.looker.core_database.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Entity(tableName = "app_table")
@Serializable
data class App(
	@PrimaryKey(autoGenerate = false)
	@SerialName("packageName") val packageName: String,
	@SerialName("name") val nameFallback: String,
	@SerialName("description") val descriptionFallback: String,
	@SerialName("summary") val summaryFallback: String,
	@SerialName("icon") val iconFallback: String,
	@SerialName("license") val license: String,
	@SerialName("webSite") val website: String,
	@SerialName("authorName") val authorName: String,
	@SerialName("authorWebSite") val authorWebsite: String,
	@SerialName("authorEmail") val authorEmail: String,
	@SerialName("sourceCode") val sourceCode: String,
	@SerialName("changelog") val changelog: String,
	@SerialName("issueTracker") val issueTracker: String,
	@SerialName("translate") val helpTranslate: String,
	@SerialName("added") val added: Long,
	@SerialName("lastUpdated") val lastUpdated: Long,
	@SerialName("suggestedVersionName") val suggestedVersionName: String,
	@SerialName("suggestedVersionCode") val suggestedVersionCode: Long,
	@SerialName("categories") val categories: List<String>,
	@SerialName("antiFeatures") val antiFeatures: List<String>,
	@SerialName("donate") val regularDonate: String,
	@SerialName("bitcoin") val bitcoinId: String,
	@SerialName("litecoin") val liteCoinAddress: String,
	@SerialName("flattrID") val flattrId: String,
	@SerialName("liberapayID") val liberaPay: String,
	@SerialName("openCollective") val openCollective: String,
	@SerialName("localized") val localized: Map<String, Localized>,
	val repoId: Long = 0L,
	val installedVersionCode: Long = 0L,
	val apks: List<Apk> = emptyList()
)