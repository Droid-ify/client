package com.looker.sync.fdroid.common

import com.looker.sync.fdroid.v1.model.AppV1
import com.looker.sync.fdroid.v1.model.IndexV1
import com.looker.sync.fdroid.v1.model.Localized
import com.looker.sync.fdroid.v1.model.PackageV1
import com.looker.sync.fdroid.v1.model.RepoV1
import com.looker.sync.fdroid.v2.model.AntiFeatureV2
import com.looker.sync.fdroid.v2.model.CategoryV2
import com.looker.sync.fdroid.v2.model.FeatureV2
import com.looker.sync.fdroid.v2.model.FileV2
import com.looker.sync.fdroid.v2.model.IndexV2
import com.looker.sync.fdroid.v2.model.LocalizedFiles
import com.looker.sync.fdroid.v2.model.LocalizedIcon
import com.looker.sync.fdroid.v2.model.LocalizedString
import com.looker.sync.fdroid.v2.model.ManifestV2
import com.looker.sync.fdroid.v2.model.MetadataV2
import com.looker.sync.fdroid.v2.model.MirrorV2
import com.looker.sync.fdroid.v2.model.PackageV2
import com.looker.sync.fdroid.v2.model.PermissionV2
import com.looker.sync.fdroid.v2.model.RepoV2
import com.looker.sync.fdroid.v2.model.ScreenshotsV2
import com.looker.sync.fdroid.v2.model.SignerV2
import com.looker.sync.fdroid.v2.model.UsesSdkV2
import com.looker.sync.fdroid.v2.model.VersionV2

private const val V1_LOCALE = "en-US"

internal fun IndexV1.toV2(): IndexV2 {
    val antiFeatures: HashSet<String> = hashSetOf()
    val categories: HashSet<String> = hashSetOf()

    val packagesV2: HashMap<String, PackageV2> = hashMapOf()

    apps.forEach { app ->
        antiFeatures.addAll(app.antiFeatures)
        categories.addAll(app.categories)
        val versions = packages[app.packageName]
        val preferredSigner = versions?.firstOrNull()?.signer
        val whatsNew: LocalizedString? = app.localized
            ?.localizedString(null) { it.whatsNew }
        val packageV2 = PackageV2(
            versions = versions?.associate { packageV1 ->
                packageV1.hash to packageV1.toVersionV2(
                    whatsNew = whatsNew,
                    packageAntiFeatures = app.antiFeatures + (packageV1.antiFeatures ?: emptyList())
                )
            } ?: emptyMap(),
            metadata = app.toV2(preferredSigner)
        )
        packagesV2.putIfAbsent(app.packageName, packageV2)
    }

    return IndexV2(
        repo = repo.toRepoV2(
            categories = categories,
            antiFeatures = antiFeatures
        ),
        packages = packagesV2,
    )
}

private fun RepoV1.toRepoV2(
    categories: Set<String>,
    antiFeatures: Set<String>,
): RepoV2 = RepoV2(
    address = address,
    timestamp = timestamp,
    icon = mapOf(V1_LOCALE to FileV2("/icons/$icon")),
    name = mapOf(V1_LOCALE to name),
    description = mapOf(V1_LOCALE to description),
    mirrors = mirrors.toMutableList()
        .apply { add(0, address) }
        .map { MirrorV2(url = it, isPrimary = (it == address).takeIf { it }) },
    antiFeatures = antiFeatures.associateWith { name ->
        AntiFeatureV2(
            name = mapOf(V1_LOCALE to name),
            icon = mapOf(V1_LOCALE to FileV2("/icons/ic_antifeature_${name.normalizeName()}.png")),
        )
    },
    categories = categories.associateWith { name ->
        CategoryV2(
            name = mapOf(V1_LOCALE to name),
            icon = mapOf(V1_LOCALE to FileV2("/icons/category_${name.normalizeName()}.png")),
        )
    },
)

private fun String.normalizeName(): String = lowercase().replace(" & ", "_")

private fun AppV1.toV2(preferredSigner: String?): MetadataV2 = MetadataV2(
    added = added ?: 0L,
    lastUpdated = lastUpdated ?: 0L,
    icon = localized?.localizedIcon(packageName, icon) { it.icon },
    name = localized?.localizedString(name) { it.name },
    description = localized?.localizedString(description) { it.description },
    summary = localized?.localizedString(summary) { it.summary },
    authorEmail = authorEmail,
    authorName = authorName,
    authorPhone = authorPhone,
    authorWebsite = authorWebSite ?: webSite,
    bitcoin = bitcoin,
    categories = categories,
    changelog = changelog,
    donate = if (donate != null) listOf(donate) else emptyList(),
    featureGraphic = localized?.localizedIcon(packageName) { it.featureGraphic },
    flattrID = flattrID,
    issueTracker = issueTracker,
    liberapay = liberapay,
    license = license,
    litecoin = litecoin,
    openCollective = openCollective,
    preferredSigner = preferredSigner,
    promoGraphic = localized?.localizedIcon(packageName) { it.promoGraphic },
    screenshots = localized?.screenshotV2(packageName),
    sourceCode = sourceCode,
    translation = translation,
    tvBanner = localized?.localizedIcon(packageName) { it.tvBanner },
    video = localized?.localizedString(null) { it.video },
    webSite = webSite,
)

private fun Map<String, Localized>.screenshotV2(
    packageName: String,
): ScreenshotsV2? = ScreenshotsV2(
    phone = localizedScreenshots { locale, screenshot ->
        screenshot.phoneScreenshots?.map {
            "/$packageName/$locale/phoneScreenshots/$it"
        }
    },
    sevenInch = localizedScreenshots { locale, screenshot ->
        screenshot.sevenInchScreenshots?.map {
            "/$packageName/$locale/sevenInchScreenshots/$it"
        }
    },
    tenInch = localizedScreenshots { locale, screenshot ->
        screenshot.tenInchScreenshots?.map {
            "/$packageName/$locale/tenInchScreenshots/$it"
        }
    },
    tv = localizedScreenshots { locale, screenshot ->
        screenshot.tvScreenshots?.map {
            "/$packageName/$locale/tvScreenshots/$it"
        }
    },
    wear = localizedScreenshots { locale, screenshot ->
        screenshot.wearScreenshots?.map {
            "/$packageName/$locale/wearScreenshots/$it"
        }
    },
).takeIf { !it.isNull }

private fun PackageV1.toVersionV2(
    whatsNew: LocalizedString?,
    packageAntiFeatures: List<String>,
): VersionV2 = VersionV2(
    added = added ?: 0L,
    file = FileV2(
        name = "/$apkName",
        sha256 = hash,
        size = size,
    ),
    src = srcName?.let { FileV2("/$it") },
    signer = signer?.let { SignerV2(listOf(it)) },
    whatsNew = whatsNew ?: emptyMap(),
    antiFeatures = packageAntiFeatures.associateWith { mapOf(V1_LOCALE to it) },
    manifest = ManifestV2(
        versionName = versionName,
        versionCode = versionCode ?: 0L,
        signer = signer?.let { SignerV2(listOf(it)) },
        usesSdk = sdkV2(),
        minSdkVersion = minSdkVersion,
        maxSdkVersion = maxSdkVersion,
        usesPermission = usesPermission.map { PermissionV2(it.name, it.maxSdk) },
        usesPermissionSdk23 = usesPermission23.map { PermissionV2(it.name, it.maxSdk) },
        features = features?.map { FeatureV2(it) } ?: emptyList(),
        nativecode = nativeCode ?: emptyList()
    ),
)

private fun PackageV1.sdkV2(): UsesSdkV2? {
    return if (minSdkVersion == null && targetSdkVersion == null) {
        null
    } else {
        UsesSdkV2(
            minSdkVersion = minSdkVersion ?: 1,
            targetSdkVersion = targetSdkVersion ?: 1,
        )
    }
}

private inline fun Map<String, Localized>.localizedString(
    default: String?,
    crossinline block: (Localized) -> String?,
): LocalizedString? {
    if (default != null) {
        return mapOf(V1_LOCALE to default)
    }
    return mapValuesNotNull { (_, localized) ->
        block(localized)
    }.takeIf { it.isNotEmpty() }
}


private inline fun Map<String, Localized>.localizedIcon(
    packageName: String,
    default: String? = null,
    crossinline block: (Localized) -> String?,
): LocalizedIcon? {
    if (default != null) {
        return mapOf(
            V1_LOCALE to FileV2("/$packageName/$V1_LOCALE/$default")
        )
    }
    return mapValuesNotNull { (locale, localized) ->
        block(localized)?.let {
            FileV2("/$packageName/$locale/$it")
        }
    }.takeIf { it.isNotEmpty() }
}

private inline fun Map<String, Localized>.localizedScreenshots(
    crossinline block: (String, Localized) -> List<String>?,
): LocalizedFiles? {
    return mapValuesNotNull { (locale, localized) ->
        val files = block(locale, localized)
        if (files.isNullOrEmpty()) null
        else files.map(::FileV2)
    }.takeIf { it.isNotEmpty() }
}

private inline fun <K, V, M> Map<K, V>.mapValuesNotNull(
    block: (Map.Entry<K, V>) -> M?
): Map<K, M> {
    val map = HashMap<K, M>()
    forEach { entry ->
        block(entry)?.let { map[entry.key] = it }
    }
    return map
}
