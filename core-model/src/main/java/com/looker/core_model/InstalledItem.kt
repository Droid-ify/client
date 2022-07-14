package com.looker.core_model

// Redundant to Room's Installed
class InstalledItem(
	val packageName: String,
	val version: String,
	val versionCode: Long,
	val signature: String,
)
