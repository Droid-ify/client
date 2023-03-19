package com.looker.core.data.fdroid.model.v2

import com.looker.core.data.fdroid.IndexParser
import com.looker.core.data.fdroid.model.IndexFile
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString

@Serializable
data class PackageV2(
	val metadata: MetadataV2,
	val versions: Map<String, PackageVersionV2> = emptyMap(),
) {
	fun walkFiles(fileConsumer: (FileV2?) -> Unit) {
		metadata.walkFiles(fileConsumer)
		versions.values.forEach { it.walkFiles(fileConsumer) }
	}
}

@Serializable
data class MetadataV2(
	val name: LocalizedTextV2? = null,
	val summary: LocalizedTextV2? = null,
	val description: LocalizedTextV2? = null,
	val added: Long,
	val lastUpdated: Long,
	val webSite: String? = null,
	val changelog: String? = null,
	val license: String? = null,
	val sourceCode: String? = null,
	val issueTracker: String? = null,
	val translation: String? = null,
	val preferredSigner: String? = null,
	val categories: List<String> = emptyList(),
	val authorName: String? = null,
	val authorEmail: String? = null,
	val authorWebSite: String? = null,
	val authorPhone: String? = null,
	val donate: List<String> = emptyList(),
	val liberapayID: String? = null,
	val liberapay: String? = null,
	val openCollective: String? = null,
	val bitcoin: String? = null,
	val litecoin: String? = null,
	val flattrID: String? = null,
	val icon: LocalizedFileV2? = null,
	val featureGraphic: LocalizedFileV2? = null,
	val promoGraphic: LocalizedFileV2? = null,
	val tvBanner: LocalizedFileV2? = null,
	val video: LocalizedTextV2? = null,
	val screenshots: Screenshots? = null,
) {
	fun walkFiles(fileConsumer: (FileV2?) -> Unit) {
		icon?.values?.forEach { fileConsumer(it) }
		featureGraphic?.values?.forEach { fileConsumer(it) }
		promoGraphic?.values?.forEach { fileConsumer(it) }
		tvBanner?.values?.forEach { fileConsumer(it) }
		screenshots?.phone?.values?.forEach { it.forEach(fileConsumer) }
		screenshots?.sevenInch?.values?.forEach { it.forEach(fileConsumer) }
		screenshots?.tenInch?.values?.forEach { it.forEach(fileConsumer) }
		screenshots?.wear?.values?.forEach { it.forEach(fileConsumer) }
		screenshots?.tv?.values?.forEach { it.forEach(fileConsumer) }
	}
}


@Serializable
data class Screenshots(
	val phone: LocalizedFileListV2? = null,
	val sevenInch: LocalizedFileListV2? = null,
	val tenInch: LocalizedFileListV2? = null,
	val wear: LocalizedFileListV2? = null,
	val tv: LocalizedFileListV2? = null,
) {
	val isNull: Boolean
		get() = phone == null && sevenInch == null && tenInch == null && wear == null && tv == null
}

interface PackageVersion {
	val versionCode: Long
	val signer: SignerV2?
	val releaseChannels: List<String>?
	val packageManifest: PackageManifest
	val hasKnownVulnerability: Boolean
}

const val ANTI_FEATURE_KNOWN_VULNERABILITY: String = "KnownVuln"

@Serializable
data class PackageVersionV2(
	val added: Long,
	val file: FileV1,
	val src: FileV2? = null,
	val manifest: ManifestV2,
	override val releaseChannels: List<String> = emptyList(),
	val antiFeatures: Map<String, LocalizedTextV2> = emptyMap(),
	val whatsNew: LocalizedTextV2 = emptyMap(),
) : PackageVersion {
	override val versionCode: Long = manifest.versionCode
	override val signer: SignerV2? = manifest.signer
	override val packageManifest: PackageManifest = manifest
	override val hasKnownVulnerability: Boolean
		get() = antiFeatures.contains(ANTI_FEATURE_KNOWN_VULNERABILITY)

	fun walkFiles(fileConsumer: (FileV2?) -> Unit) {
		fileConsumer(src)
	}
}

@Serializable
data class FileV1(
	override val name: String,
	override val sha256: String,
	override val size: Long? = null,
	@SerialName("ipfsCIDv1")
	override val ipfsCidV1: String? = null,
) : IndexFile {
	companion object {
		@JvmStatic
		fun deserialize(string: String?): FileV1? {
			if (string == null) return null
			return IndexParser.json.decodeFromString(string)
		}
	}

	override suspend fun serialize(): String {
		return IndexParser.json.encodeToString(this)
	}
}


interface PackageManifest {
	val minSdkVersion: Int?
	val maxSdkVersion: Int?
	val featureNames: List<String>?
	val nativecode: List<String>?
}

@Serializable
data class ManifestV2(
	val versionName: String,
	val versionCode: Long,
	val usesSdk: UsesSdkV2? = null,
	override val maxSdkVersion: Int? = null,
	val signer: SignerV2? = null, // yes this can be null for stuff like non-apps
	val usesPermission: List<PermissionV2> = emptyList(),
	val usesPermissionSdk23: List<PermissionV2> = emptyList(),
	override val nativecode: List<String> = emptyList(),
	val features: List<FeatureV2> = emptyList(),
) : PackageManifest {
	override val minSdkVersion: Int? = usesSdk?.minSdkVersion
	override val featureNames: List<String> = features.map { it.name }
}

@Serializable
data class UsesSdkV2(
	val minSdkVersion: Int,
	val targetSdkVersion: Int,
)

@Serializable
data class SignerV2(
	val sha256: List<String>,
	val hasMultipleSigners: Boolean = false,
)

@Serializable
data class PermissionV2(
	val name: String,
	val maxSdkVersion: Int? = null,
)

@Serializable
data class FeatureV2(
	val name: String,
)
