package com.looker.core_database.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "app_table")
data class App(
	@PrimaryKey(autoGenerate = false)
	val packageName: String,
	val repoId: Long,
	val icon: String,
	val license: String,
	val website: String,
	val authorName: String,
	val authorWebsite: String,
	val authorEmail: String,
	val sourceCode: String,
	val changelog: String,
	val issueTracker: String,
	val helpTranslate: String,
	val added: Long,
	val lastUpdated: Long,
	val suggestedVersionName: String,
	val suggestedVersionCode: Long,
	val installedVersionCode: Long?,
	val categories: List<String>,
	val antiFeatures: List<String>,
	val regularDonate: String,
	val bitcoinId: String,
	val liteCoinAddress: String,
	val flattrId: String,
	val liberaPay: String,
	val openCollective: String,
	val localized: List<Localized>,
	val apks: List<Apk>
)