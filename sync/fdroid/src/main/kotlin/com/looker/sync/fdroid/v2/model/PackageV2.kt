package com.looker.sync.fdroid.v2.model

import kotlinx.serialization.Serializable

@Serializable
data class PackageV2(
    val metadata: MetadataV2,
    val versions: Map<String, VersionV2>,
)

@Serializable
data class PackageV2Diff(
    val metadata: MetadataV2Diff?,
    val versions: Map<String, VersionV2Diff?>? = null,
) {
    fun toPackage(): PackageV2 = PackageV2(
        metadata = MetadataV2(
            added = metadata?.added ?: 0L,
            lastUpdated = metadata?.lastUpdated ?: 0L,
            name = metadata?.name
                ?.mapNotNull { (key, value) -> value?.let { key to value } }?.toMap(),
            summary = metadata?.summary
                ?.mapNotNull { (key, value) -> value?.let { key to value } }?.toMap(),
            description = metadata?.description
                ?.mapNotNull { (key, value) -> value?.let { key to value } }?.toMap(),
            icon = metadata?.icon,
            authorEmail = metadata?.authorEmail,
            authorName = metadata?.authorName,
            authorPhone = metadata?.authorPhone,
            authorWebsite = metadata?.authorWebsite,
            bitcoin = metadata?.bitcoin,
            categories = metadata?.categories ?: emptyList(),
            changelog = metadata?.changelog,
            donate = metadata?.donate ?: emptyList(),
            featureGraphic = metadata?.featureGraphic,
            flattrID = metadata?.flattrID,
            issueTracker = metadata?.issueTracker,
            liberapay = metadata?.liberapay,
            license = metadata?.license,
            litecoin = metadata?.litecoin,
            openCollective = metadata?.openCollective,
            preferredSigner = metadata?.preferredSigner,
            promoGraphic = metadata?.promoGraphic,
            sourceCode = metadata?.sourceCode,
            screenshots = metadata?.screenshots,
            tvBanner = metadata?.tvBanner,
            translation = metadata?.translation,
            video = metadata?.video,
            webSite = metadata?.webSite,
        ),
        versions = versions
            ?.mapNotNull { (key, value) -> value?.let { key to it.toVersion() } }
            ?.toMap() ?: emptyMap()
    )

    fun patchInto(pack: PackageV2): PackageV2 {
        val versionsToRemove = versions?.filterValues { it == null }?.keys ?: emptySet()
        val versionsToAdd = versions
            ?.mapNotNull { (key, value) ->
                value?.let { value ->
                    if (pack.versions.keys.contains(key))
                        pack.versions[key]?.let { value.patchInto(it) }
                    else value.toVersion()
                }?.let { key to it }
            } ?: emptyList()

        return pack.copy(
            metadata = pack.metadata.copy(
                added = pack.metadata.added,
                lastUpdated = metadata?.lastUpdated ?: pack.metadata.lastUpdated,
                name = metadata?.name
                    ?.mapNotNull { (key, value) -> value?.let { key to value } }?.toMap()
                    ?: pack.metadata.name,
                summary = metadata?.summary
                    ?.mapNotNull { (key, value) -> value?.let { key to value } }?.toMap()
                    ?: pack.metadata.summary,
                description = metadata?.description
                    ?.mapNotNull { (key, value) -> value?.let { key to value } }?.toMap()
                    ?: pack.metadata.description,
                icon = metadata?.icon ?: pack.metadata.icon,
                authorEmail = metadata?.authorEmail ?: pack.metadata.authorEmail,
                authorName = metadata?.authorName ?: pack.metadata.authorName,
                authorPhone = metadata?.authorPhone ?: pack.metadata.authorPhone,
                authorWebsite = metadata?.authorWebsite ?: pack.metadata.authorWebsite,
                bitcoin = metadata?.bitcoin ?: pack.metadata.bitcoin,
                categories = metadata?.categories ?: pack.metadata.categories,
                changelog = metadata?.changelog ?: pack.metadata.changelog,
                donate = metadata?.donate?.takeIf { it.isNotEmpty() } ?: pack.metadata.donate,
                featureGraphic = metadata?.featureGraphic ?: pack.metadata.featureGraphic,
                flattrID = metadata?.flattrID ?: pack.metadata.flattrID,
                issueTracker = metadata?.issueTracker ?: pack.metadata.issueTracker,
                liberapay = metadata?.liberapay ?: pack.metadata.liberapay,
                license = metadata?.license ?: pack.metadata.license,
                litecoin = metadata?.litecoin ?: pack.metadata.litecoin,
                openCollective = metadata?.openCollective ?: pack.metadata.openCollective,
                preferredSigner = metadata?.preferredSigner ?: pack.metadata.preferredSigner,
                promoGraphic = metadata?.promoGraphic ?: pack.metadata.promoGraphic,
                sourceCode = metadata?.sourceCode ?: pack.metadata.sourceCode,
                screenshots = metadata?.screenshots ?: pack.metadata.screenshots,
                tvBanner = metadata?.tvBanner ?: pack.metadata.tvBanner,
                translation = metadata?.translation ?: pack.metadata.translation,
                video = metadata?.video ?: pack.metadata.video,
                webSite = metadata?.webSite ?: pack.metadata.webSite,
            ),
            versions = pack.versions
                .minus(versionsToRemove)
                .plus(versionsToAdd),
        )
    }
}

@Serializable
data class MetadataV2(
    val name: LocalizedString? = null,
    val summary: LocalizedString? = null,
    val description: LocalizedString? = null,
    val icon: LocalizedIcon? = null,
    val added: Long,
    val lastUpdated: Long,
    val authorEmail: String? = null,
    val authorName: String? = null,
    val authorPhone: String? = null,
    val authorWebsite: String? = null,
    val bitcoin: String? = null,
    val categories: List<String> = emptyList(),
    val changelog: String? = null,
    val donate: List<String> = emptyList(),
    val featureGraphic: LocalizedIcon? = null,
    val flattrID: String? = null,
    val issueTracker: String? = null,
    val liberapay: String? = null,
    val license: String? = null,
    val litecoin: String? = null,
    val openCollective: String? = null,
    val preferredSigner: String? = null,
    val promoGraphic: LocalizedIcon? = null,
    val sourceCode: String? = null,
    val screenshots: ScreenshotsV2? = null,
    val tvBanner: LocalizedIcon? = null,
    val translation: String? = null,
    val video: LocalizedString? = null,
    val webSite: String? = null,
)

@Serializable
data class MetadataV2Diff(
    val name: NullableLocalizedString? = null,
    val summary: NullableLocalizedString? = null,
    val description: NullableLocalizedString? = null,
    val icon: LocalizedIcon? = null,
    val added: Long? = null,
    val lastUpdated: Long? = null,
    val authorEmail: String? = null,
    val authorName: String? = null,
    val authorPhone: String? = null,
    val authorWebsite: String? = null,
    val bitcoin: String? = null,
    val categories: List<String> = emptyList(),
    val changelog: String? = null,
    val donate: List<String> = emptyList(),
    val featureGraphic: LocalizedIcon? = null,
    val flattrID: String? = null,
    val issueTracker: String? = null,
    val liberapay: String? = null,
    val license: String? = null,
    val litecoin: String? = null,
    val openCollective: String? = null,
    val preferredSigner: String? = null,
    val promoGraphic: LocalizedIcon? = null,
    val sourceCode: String? = null,
    val screenshots: ScreenshotsV2? = null,
    val tvBanner: LocalizedIcon? = null,
    val translation: String? = null,
    val video: LocalizedString? = null,
    val webSite: String? = null,
)

@Serializable
data class VersionV2(
    val added: Long,
    val file: FileV2,
    val src: FileV2? = null,
    val signer: SignerV2? = null,
    val whatsNew: LocalizedString = emptyMap(),
    val manifest: ManifestV2,
    val antiFeatures: Map<String, LocalizedString> = emptyMap(),
)

@Serializable
data class VersionV2Diff(
    val added: Long? = null,
    val file: FileV2? = null,
    val src: FileV2? = null,
    val signer: SignerV2? = null,
    val whatsNew: LocalizedString? = null,
    val manifest: ManifestV2? = null,
    val antiFeatures: Map<String, LocalizedString>? = null,
) {
    fun toVersion() = VersionV2(
        added = added ?: 0,
        file = file ?: FileV2(""),
        src = src ?: FileV2(""),
        signer = signer ?: SignerV2(emptyList()),
        whatsNew = whatsNew ?: emptyMap(),
        manifest = manifest ?: ManifestV2(
            versionName = "",
            versionCode = 0,
        ),
        antiFeatures = antiFeatures ?: emptyMap(),
    )

    fun patchInto(version: VersionV2): VersionV2 {
        return version.copy(
            added = added ?: version.added,
            file = file ?: version.file,
            src = src ?: version.src,
            signer = signer ?: version.signer,
            whatsNew = whatsNew ?: version.whatsNew,
            manifest = manifest ?: version.manifest,
            antiFeatures = antiFeatures ?: version.antiFeatures,
        )
    }
}

@Serializable
data class ManifestV2(
    val versionName: String,
    val versionCode: Long,
    val signer: SignerV2? = null,
    val usesSdk: UsesSdkV2? = null,
    val minSdkVersion: Int? = null,
    val maxSdkVersion: Int? = null,
    val usesPermission: List<PermissionV2> = emptyList(),
    val usesPermissionSdk23: List<PermissionV2> = emptyList(),
    val features: List<FeatureV2> = emptyList(),
    val nativecode: List<String> = emptyList(),
)

@Serializable
data class UsesSdkV2(
    val minSdkVersion: Int,
    val targetSdkVersion: Int,
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

@Serializable
data class SignerV2(
    val sha256: List<String>,
    val hasMultipleSigners: Boolean = false,
)


@Serializable
data class ScreenshotsV2(
    val phone: LocalizedFiles? = null,
    val sevenInch: LocalizedFiles? = null,
    val tenInch: LocalizedFiles? = null,
    val wear: LocalizedFiles? = null,
    val tv: LocalizedFiles? = null,
) {

    val isNull: Boolean =
        phone == null && sevenInch == null && tenInch == null && wear == null && tv == null

}
