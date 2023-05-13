package com.looker.core.data.fdroid

import com.looker.core.database.model.*
import org.fdroid.index.v2.PackageV2
import org.fdroid.index.v2.PackageVersionV2
import org.fdroid.index.v2.RepoV2
import java.util.Locale.filter

fun PackageV2.toEntity(packageName: String, repoId: Long, allowUnstable: Boolean = false): AppEntity =
	AppEntity(
		repoId = repoId,
		packageName = packageName,
		categories = metadata.categories,
		summary = metadata.summary ?: emptyMap(),
		description = metadata.description ?: emptyMap(),
		changelog = metadata.changelog ?: "",
		translation = metadata.translation ?: "",
		issueTracker = metadata.issueTracker ?: "",
		sourceCode = metadata.sourceCode ?: "",
		binaries = "",
		name = metadata.name ?: emptyMap(),
		authorName = metadata.authorName ?: "",
		authorEmail = metadata.authorEmail ?: "",
		authorWebSite = metadata.authorWebSite ?: "",
		donate = metadata.donate.firstOrNull() ?: "",
		liberapayID = metadata.liberapayID ?: "",
		liberapay = metadata.liberapay ?: "",
		openCollective = metadata.openCollective ?: "",
		bitcoin = metadata.bitcoin ?: "",
		litecoin = metadata.litecoin ?: "",
		flattrID = metadata.flattrID ?: "",
		suggestedVersionCode = versions.values.firstOrNull()?.manifest?.versionCode ?: -1,
		suggestedVersionName = versions.values.firstOrNull()?.manifest?.versionName ?: "",
		license = metadata.license ?: "",
		webSite = metadata.webSite ?: "",
		added = metadata.added,
		icon = metadata.icon?.mapValues { it.value.name } ?: emptyMap(),
		lastUpdated = metadata.lastUpdated,
		phoneScreenshots = metadata.screenshots?.phone?.mapValues { it.value.map { it.name } }
			?: emptyMap(),
		tenInchScreenshots = metadata.screenshots?.tenInch?.mapValues { it.value.map { it.name } }
			?: emptyMap(),
		sevenInchScreenshots = metadata.screenshots?.sevenInch?.mapValues { it.value.map { it.name } }
			?: emptyMap(),
		tvScreenshots = metadata.screenshots?.tv?.mapValues { it.value.map { it.name } }
			?: emptyMap(),
		wearScreenshots = metadata.screenshots?.wear?.mapValues { it.value.map { it.name } }
			?: emptyMap(),
		featureGraphic = metadata.featureGraphic?.mapValues { it.value.name } ?: emptyMap(),
		promoGraphic = metadata.promoGraphic?.mapValues { it.value.name } ?: emptyMap(),
		tvBanner = metadata.tvBanner?.mapValues { it.value.name } ?: emptyMap(),
		video = metadata.video ?: emptyMap(),
		packages = versions.values.map(PackageVersionV2::toPackage)
	).checkUnstable(allowUnstable)

private fun AppEntity.checkUnstable(allowUnstable: Boolean = false): AppEntity = copy(
	packages = packages.filter {
		allowUnstable || suggestedVersionCode <= 0 || it.versionCode <= suggestedVersionCode
	}
)

fun PackageVersionV2.toPackage(): PackageEntity = PackageEntity(
	added = added,
	hash = file.sha256,
	features = manifest.features.map { it.name },
	apkName = file.name,
	hashType = "SHA-256",
	minSdkVersion = manifest.minSdkVersion ?: -1,
	maxSdkVersion = manifest.maxSdkVersion ?: -1,
	signer = manifest.signer?.sha256?.firstOrNull() ?: "",
	size = file.size ?: -1,
	usesPermission = manifest.usesPermission.map {
		PermissionEntity(name = it.name, maxSdk = it.maxSdkVersion)
	} + manifest.usesPermissionSdk23.map {
		PermissionEntity(name = it.name, maxSdk = it.maxSdkVersion, minSdk = 23)
	},
	versionCode = manifest.versionCode,
	versionName = manifest.versionName,
	srcName = src?.name ?: "",
	nativeCode = manifest.nativecode,
	antiFeatures = antiFeatures.keys.toList(),
	targetSdkVersion = manifest.usesSdk?.targetSdkVersion ?: -1,
	sig = signer?.sha256?.firstOrNull() ?: "",
	whatsNew = whatsNew
)

fun RepoV2.toEntity(
	id: Long,
	fingerprint: String,
	etag: String,
	username: String,
	password: String,
	enabled: Boolean = true
) = RepoEntity(
	id = id,
	enabled = enabled,
	fingerprint = fingerprint,
	mirrors = mirrors.map { it.url },
	address = address,
	name = name,
	description = description,
	timestamp = timestamp,
	etag = etag,
	username = username,
	password = password,
	antiFeatures = antiFeatures.mapValues {
		AntiFeatureEntity(
			name = it.value.name,
			icon = it.value.icon.mapValues { it.value.name },
			description = it.value.description
		)
	},
	categories = categories.mapValues {
		CategoryEntity(
			name = it.value.name,
			icon = it.value.icon.mapValues { it.value.name },
			description = it.value.description
		)
	}
)