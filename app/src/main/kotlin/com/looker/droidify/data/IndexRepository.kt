package com.looker.droidify.data

import com.looker.droidify.data.local.model.DonateType
import com.looker.droidify.data.local.model.GraphicType
import com.looker.droidify.data.local.model.ScreenshotType
import com.looker.droidify.data.local.sql.DroidifyDb
import com.looker.droidify.data.model.Fingerprint
import com.looker.droidify.sync.v2.model.CategoryV2
import com.looker.droidify.sync.v2.model.AntiFeatureV2
import com.looker.droidify.sync.v2.model.DefaultName
import com.looker.droidify.sync.v2.model.IndexV2
import com.looker.droidify.sync.v2.model.LocalizedFiles
import com.looker.droidify.sync.v2.model.LocalizedIcon
import com.looker.droidify.sync.v2.model.MetadataV2
import com.looker.droidify.sync.v2.model.PackageV2
import com.looker.droidify.sync.v2.model.RepoV2
import com.looker.droidify.sync.v2.model.Tag
import com.looker.droidify.sync.v2.model.VersionV2
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext

class IndexRepository(
    private val db: DroidifyDb,
    private val dispatcher: CoroutineDispatcher,
) {

    suspend fun insertIndex(
        repoId: Long,
        fingerprint: Fingerprint,
        index: IndexV2,
        etag: String? = null,
    ) = withContext(dispatcher) {
        db.transaction {
            insertRepo(repoId, fingerprint, index.repo, etag)
            index.packages.forEach { (packageName, packageV2) ->
                insertPackage(repoId, packageName, packageV2)
            }
        }
    }

    private fun insertRepo(repoId: Long, fingerprint: Fingerprint, repo: RepoV2, etag: String?) {
        db.repositoryQueries.updateRepoVersionInfo(
            fingerprint = fingerprint,
            etag = etag,
            timestamp = repo.timestamp,
            id = repoId,
        )

        val locales = repo.name.keys + repo.description.keys + repo.icon?.keys.orEmpty()
        locales.forEach { locale ->
            val icon = repo.icon?.get(locale)
            db.repositoryQueries.insertLocalizedRepo(
                repoId = repoId,
                locale = locale,
                name = repo.name[locale],
                description = repo.description[locale],
                iconName = icon?.name,
                iconSha256 = icon?.sha256?.hexOrNull(),
                iconSize = icon?.size,
            )
        }

        db.repositoryQueries.deleteMirrors(repoId)
        repo.mirrors.forEach { mirror ->
            db.repositoryQueries.insertMirror(
                url = mirror.url,
                countryCode = mirror.countryCode,
                isPrimary = mirror.isPrimary == true,
                repoId = repoId,
            )
        }

        repo.categories.forEach { (defaultName, category) ->
            insertCategory(repoId, defaultName, category)
        }
        repo.antiFeatures.forEach { (tag, antiFeature) ->
            insertAntiFeature(repoId, tag, antiFeature)
        }
    }

    private fun insertCategory(repoId: Long, defaultName: DefaultName, category: CategoryV2) {
        val locales = category.name.keys + category.description.keys + category.icon.keys
        locales.forEach { locale ->
            db.categoryQueries.insertCategory(
                icon = category.icon[locale]?.name,
                name = category.name[locale] ?: defaultName,
                description = category.description[locale],
                locale = locale,
                defaultName = defaultName,
            )
        }
        db.categoryQueries.insertCategoryRepoRelation(repoId = repoId, defaultName = defaultName)
    }

    private fun insertAntiFeature(repoId: Long, tag: Tag, antiFeature: AntiFeatureV2) {
        val locales = antiFeature.name.keys + antiFeature.description.keys + antiFeature.icon.keys
        locales.forEach { locale ->
            db.antiFeatureQueries.insertAntiFeature(
                icon = antiFeature.icon[locale]?.name,
                name = antiFeature.name[locale] ?: tag,
                description = antiFeature.description[locale],
                locale = locale,
                tag = tag,
            )
        }
        db.antiFeatureQueries.insertAntiFeatureRepoRelation(repoId = repoId, tag = tag)
    }

    private fun insertPackage(repoId: Long, packageName: String, packageV2: PackageV2) {
        val metadata = packageV2.metadata

        val authorEmail = metadata.authorEmail.orEmpty()
        val authorName = metadata.authorName.orEmpty()
        val authorWebSite = metadata.authorWebSite.orEmpty()
        val authorId = db.appMetadataQueries.insertAuthor(
            email = authorEmail,
            name = authorName,
            website = authorWebSite,
        ).executeAsOne()

        val appId = db.appQueries.insertApp(
            added = metadata.added,
            lastUpdated = metadata.lastUpdated,
            preferredSigner = metadata.preferredSigner?.hexOrNull(),
            packageName = packageName,
            authorId = authorId,
            repoId = repoId,
        ).executeAsOne()

        insertLocalizedApp(appId, metadata)
        metadata.categories.forEach { defaultName ->
            db.categoryQueries.insertCategoryAppRelation(appId = appId, defaultName = defaultName)
        }
        db.appMetadataQueries.insertLinks(
            license = metadata.license,
            changelog = metadata.changelog,
            issueTracker = metadata.issueTracker,
            translation = metadata.translation,
            sourceCode = metadata.sourceCode,
            webSite = metadata.webSite,
            appId = appId,
        )
        insertGraphics(appId, metadata)
        insertScreenshots(appId, metadata)
        insertDonations(appId, metadata)

        packageV2.versions.values.forEach { version -> insertVersion(appId, version) }
    }

    private fun insertLocalizedApp(appId: Long, metadata: MetadataV2) {
        val locales = metadata.name?.keys.orEmpty() +
            metadata.summary?.keys.orEmpty() +
            metadata.icon?.keys.orEmpty() +
            metadata.description?.keys.orEmpty()
        locales.forEach { locale ->
            val icon = metadata.icon?.get(locale)
            db.appQueries.insertLocalizedApp(
                appId = appId,
                locale = locale,
                name = metadata.name?.get(locale),
                summary = metadata.summary?.get(locale),
                iconName = icon?.name,
                iconSha256 = icon?.sha256?.hexOrNull(),
                iconSize = icon?.size,
                description = metadata.description?.get(locale),
            )
        }
    }

    private fun insertGraphics(appId: Long, metadata: MetadataV2) {
        fun LocalizedIcon?.insert(type: GraphicType) = this?.forEach { (locale, file) ->
            db.appMetadataQueries.insertGraphic(
                url = file.name,
                type = type.value,
                locale = locale,
                appId = appId,
            )
        }
        metadata.featureGraphic.insert(GraphicType.FEATURE_GRAPHIC)
        metadata.promoGraphic.insert(GraphicType.PROMO_GRAPHIC)
        metadata.tvBanner.insert(GraphicType.TV_BANNER)
        metadata.video?.forEach { (locale, url) ->
            db.appMetadataQueries.insertGraphic(
                url = url,
                type = GraphicType.VIDEO.value,
                locale = locale,
                appId = appId,
            )
        }
    }

    private fun insertScreenshots(appId: Long, metadata: MetadataV2) {
        fun LocalizedFiles?.insert(type: ScreenshotType) = this?.forEach { (locale, files) ->
            files.forEach { file ->
                db.appMetadataQueries.insertScreenshot(
                    path = file.name,
                    type = type.value,
                    locale = locale,
                    appId = appId,
                )
            }
        }
        val screenshots = metadata.screenshots ?: return
        screenshots.phone.insert(ScreenshotType.PHONE)
        screenshots.sevenInch.insert(ScreenshotType.SEVEN_INCH)
        screenshots.tenInch.insert(ScreenshotType.TEN_INCH)
        screenshots.wear.insert(ScreenshotType.WEAR)
        screenshots.tv.insert(ScreenshotType.TV)
    }

    private fun insertDonations(appId: Long, metadata: MetadataV2) {
        fun String?.insert(type: DonateType) = this?.let { value ->
            db.appMetadataQueries.insertDonate(type = type.value, value_ = value, appId = appId)
        }
        metadata.donate.forEach { url -> url.insert(DonateType.REGULAR) }
        metadata.bitcoin.insert(DonateType.BITCOIN)
        metadata.litecoin.insert(DonateType.LITECOIN)
        metadata.liberapay.insert(DonateType.LIBERAPAY)
        metadata.openCollective.insert(DonateType.OPEN_COLLECTIVE)
    }

    private fun insertVersion(appId: Long, version: VersionV2) {
        val manifest = version.manifest
        val minSdkVersion = manifest.usesSdk?.minSdkVersion ?: 1
        val apkSha256 = requireNotNull(version.file.sha256.hexOrNull()) {
            "Invalid apk sha256 for ${version.file.name}"
        }
        val versionId = db.versionQueries.insertVersion(
            added = version.added,
            whatsNew = version.whatsNew,
            versionName = manifest.versionName,
            versionCode = manifest.versionCode,
            maxSdkVersion = manifest.maxSdkVersion,
            minSdkVersion = minSdkVersion,
            targetSdkVersion = manifest.usesSdk?.targetSdkVersion ?: minSdkVersion,
            apkName = version.file.name,
            apkSha256 = apkSha256,
            apkSize = version.file.size,
            appId = appId,
        ).executeAsOne()

        (manifest.usesPermission + manifest.usesPermissionSdk23).forEach { permission ->
            db.versionQueries.insertPermission(
                name = permission.name,
                maxSdkVersion = permission.maxSdkVersion,
                versionId = versionId,
            )
        }
        manifest.features.forEach { feature ->
            db.versionQueries.insertFeature(name = feature.name, versionId = versionId)
        }
        manifest.nativecode.forEach { abi ->
            db.versionQueries.insertNativeCode(abi = abi, versionId = versionId)
        }
        version.antiFeatures.forEach { (tag, reason) ->
            db.antiFeatureQueries.insertAntiFeatureAppRelation(
                tag = tag,
                reason = reason,
                versionId = versionId,
            )
        }
    }
}

@OptIn(ExperimentalStdlibApi::class)
private fun String.hexOrNull(): ByteArray? = try {
    hexToByteArray()
} catch (_: IllegalArgumentException) {
    null
}
