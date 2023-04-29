package com.looker.core.data.fdroid.model.v1

import com.looker.core.database.model.AppEntity
import kotlinx.serialization.Serializable

@Serializable
data class AppDto(
	val packageName: String,
	val categories: List<String> = emptyList(),
	val antiFeatures: List<String> = emptyList(),
	val summary: String = "",
	val description: String = "",
	val changelog: String = "",
	val translation: String = "",
	val issueTracker: String = "",
	val sourceCode: String = "",
	val binaries: String = "",
	val name: String = "",
	val authorName: String = "",
	val authorEmail: String = "",
	val authorWebSite: String = "",
	val donate: String = "",
	val liberapayID: String = "",
	val liberapay: String = "",
	val openCollective: String = "",
	val bitcoin: String = "",
	val litecoin: String = "",
	val flattrID: String = "",
	val suggestedVersionName: String = "",
	val suggestedVersionCode: String = "",
	val license: String = "",
	val webSite: String = "",
	val added: Long = 0L,
	val icon: String = "",
	val lastUpdated: Long = 0L,
	val localized: Map<String, LocalizedDto> = emptyMap(),
	val allowedAPKSigningKeys: List<String> = emptyList()
)

@Serializable
data class LocalizedDto(
	val description: String = "",
	val name: String = "",
	val icon: String = "",
	val whatsNew: String = "",
	val video: String = "",
	val phoneScreenshots: List<String> = emptyList(),
	val sevenInchScreenshots: List<String> = emptyList(),
	val tenInchScreenshots: List<String> = emptyList(),
	val wearScreenshots: List<String> = emptyList(),
	val tvScreenshots: List<String> = emptyList(),
	val featureGraphic: String = "",
	val promoGraphic: String = "",
	val tvBanner: String = "",
	val summary: String = "",
)

fun AppDto.toEntity(repoId: Long, packages: List<PackageDto>): AppEntity = AppEntity(
	packageName = packageName,
	repoId = repoId,
	categories = categories,
	antiFeatures = antiFeatures,
	summary = getLocalizedMap { summary },
	description = getLocalizedMap { description },
	changelog = changelog,
	translation = translation,
	issueTracker = issueTracker,
	sourceCode = sourceCode,
	binaries = binaries,
	name = getLocalizedMap { name },
	authorName = authorName,
	authorEmail = authorEmail,
	authorWebSite = authorWebSite,
	donate = donate,
	liberapayID = liberapayID,
	liberapay = liberapay,
	openCollective = openCollective,
	bitcoin = bitcoin,
	litecoin = litecoin,
	flattrID = flattrID,
	suggestedVersionName = suggestedVersionName,
	suggestedVersionCode = suggestedVersionCode.toInt(),
	license = license,
	webSite = webSite,
	added = added,
	icon = getLocalizedMap { icon },
	lastUpdated = lastUpdated,
	phoneScreenshots = getLocalizedMap { phoneScreenshots },
	sevenInchScreenshots = getLocalizedMap { sevenInchScreenshots },
	tenInchScreenshots = getLocalizedMap { tenInchScreenshots },
	wearScreenshots = getLocalizedMap { wearScreenshots },
	tvScreenshots = getLocalizedMap { tvScreenshots },
	featureGraphic = getLocalizedMap { featureGraphic },
	promoGraphic = getLocalizedMap { promoGraphic },
	tvBanner = getLocalizedMap { tvBanner },
	video = getLocalizedMap { video },
	packages = packages.map(PackageDto::toEntity)
)

private fun <T> AppDto.getLocalizedMap(block: LocalizedDto.() -> T): Map<String, T> {
	if (localized.isEmpty()) return emptyMap()
	val mutableMap = this.localized.toMutableMap()
	val defaultValues = LocalizedDto(
		description = description,
		name = name,
		icon = icon,
		summary = summary
	)
	mutableMap["en"] = defaultValues
	return mutableMap.mapValues { it.value.block() }
}