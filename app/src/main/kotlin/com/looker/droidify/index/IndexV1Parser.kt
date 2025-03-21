package com.looker.droidify.index

import android.content.res.Resources
import androidx.core.os.ConfigurationCompat.getLocales
import androidx.core.os.LocaleListCompat
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.core.JsonToken
import com.looker.droidify.utility.common.extension.Json
import com.looker.droidify.utility.common.extension.collectDistinctNotEmptyStrings
import com.looker.droidify.utility.common.extension.collectNotNull
import com.looker.droidify.utility.common.extension.forEach
import com.looker.droidify.utility.common.extension.forEachKey
import com.looker.droidify.utility.common.extension.illegal
import com.looker.droidify.model.Product
import com.looker.droidify.model.Product.Donate.Bitcoin
import com.looker.droidify.model.Product.Donate.Liberapay
import com.looker.droidify.model.Product.Donate.Litecoin
import com.looker.droidify.model.Product.Donate.OpenCollective
import com.looker.droidify.model.Product.Donate.Regular
import com.looker.droidify.model.Product.Screenshot.Type.LARGE_TABLET
import com.looker.droidify.model.Product.Screenshot.Type.PHONE
import com.looker.droidify.model.Product.Screenshot.Type.SMALL_TABLET
import com.looker.droidify.model.Product.Screenshot.Type.TV
import com.looker.droidify.model.Product.Screenshot.Type.VIDEO
import com.looker.droidify.model.Product.Screenshot.Type.WEAR
import com.looker.droidify.model.Release
import com.looker.droidify.utility.common.SdkCheck
import com.looker.droidify.utility.common.nullIfEmpty
import java.io.InputStream

object IndexV1Parser {
    interface Callback {
        fun onRepository(
            mirrors: List<String>,
            name: String,
            description: String,
            version: Int,
            timestamp: Long
        )

        fun onProduct(product: Product)
        fun onReleases(packageName: String, releases: List<Release>)
    }

    private class Screenshots(
        val video: List<String>,
        val phone: List<String>,
        val smallTablet: List<String>,
        val largeTablet: List<String>,
        val wear: List<String>,
        val tv: List<String>,
    )

    private class Localized(
        val name: String,
        val summary: String,
        val description: String,
        val whatsNew: String,
        val metadataIcon: String,
        val screenshots: Screenshots?
    )

    private fun <T> Map<String, Localized>.getAndCall(
        key: String,
        callback: (String, Localized) -> T?
    ): T? {
        return this[key]?.let { callback(key, it) }
    }

    /**
     * Gets the best localization for the given [localeList]
     * from collections.
     */
    private fun <T> Map<String, T>?.getBestLocale(localeList: LocaleListCompat): T? {
        if (isNullOrEmpty()) return null
        val firstMatch = localeList.getFirstMatch(keys.toTypedArray()) ?: return null
        val tag = firstMatch.toLanguageTag()
        // try first matched tag first (usually has region tag, e.g. de-DE)
        return get(tag) ?: run {
            // split away stuff like script and try language and region only
            val langCountryTag = "${firstMatch.language}-${firstMatch.country}"
            getOrStartsWith(langCountryTag) ?: run {
                // split away region tag and try language only
                val langTag = firstMatch.language
                // try language, then English and then just take the first of the list
                getOrStartsWith(langTag) ?: get("en-US") ?: get("en") ?: values.first()
            }
        }
    }

    /**
     * Returns the value from the map with the given key or if that key is not contained in the map,
     * tries the first map key that starts with the given key.
     * If nothing matches, null is returned.
     *
     * This is useful when looking for a language tag like `fr_CH` and falling back to `fr`
     * in a map that has `fr_FR` as a key.
     */
    private fun <T> Map<String, T>.getOrStartsWith(s: String): T? = get(s) ?: run {
        entries.forEach { (key, value) ->
            if (key.startsWith(s)) return value
        }
        return null
    }

    private fun <T> Map<String, Localized>.find(callback: (String, Localized) -> T?): T? {
        return getAndCall("en-US", callback)
            ?: getAndCall("en_US", callback)
            ?: getAndCall("en", callback)
    }

    private fun <T> Map<String, Localized>.findLocalized(callback: (Localized) -> T?): T? {
        return getBestLocale(getLocales(Resources.getSystem().configuration))?.let { callback(it) }
    }

    private fun Map<String, Localized>.findString(
        fallback: String,
        callback: (Localized) -> String
    ): String {
        return (find { _, localized -> callback(localized).nullIfEmpty() } ?: fallback).trim()
    }

    private fun Map<String, Localized>.findLocalizedString(
        fallback: String,
        callback: (Localized) -> String
    ): String {
        // @BLumia: it's possible a key of a certain Localized object is empty, so we still need a fallback
        return (
            findLocalized { localized -> callback(localized).trim().nullIfEmpty() } ?: findString(
                fallback,
                callback
            )
            ).trim()
    }

    internal object DonateComparator : Comparator<Product.Donate> {
        private val classes = listOf(
            Regular::class,
            Bitcoin::class,
            Litecoin::class,
            Liberapay::class,
            OpenCollective::class
        )

        override fun compare(donate1: Product.Donate, donate2: Product.Donate): Int {
            val index1 = classes.indexOf(donate1::class)
            val index2 = classes.indexOf(donate2::class)
            return when {
                index1 >= 0 && index2 == -1 -> -1
                index2 >= 0 && index1 == -1 -> 1
                else -> index1.compareTo(index2)
            }
        }
    }

    private const val DICT_REPO = "repo"
    private const val DICT_PRODUCT = "apps"
    private const val DICT_RELEASE = "packages"

    private const val KEY_REPO_ADDRESS = "address"
    private const val KEY_REPO_MIRRORS = "mirrors"
    private const val KEY_REPO_NAME = "name"
    private const val KEY_REPO_DESC = "description"
    private const val KEY_REPO_VER = "version"
    private const val KEY_REPO_TIME = "timestamp"

    fun parse(repositoryId: Long, inputStream: InputStream, callback: Callback) {
        val jsonParser = Json.factory.createParser(inputStream)
        if (jsonParser.nextToken() != JsonToken.START_OBJECT) {
            jsonParser.illegal()
        } else {
            jsonParser.forEachKey { key ->
                when {
                    key.dictionary(DICT_REPO) -> {
                        var address = ""
                        var mirrors = emptyList<String>()
                        var name = ""
                        var description = ""
                        var version = 0
                        var timestamp = 0L
                        forEachKey {
                            when {
                                it.string(KEY_REPO_ADDRESS) -> address = valueAsString
                                it.array(KEY_REPO_MIRRORS) -> mirrors =
                                    collectDistinctNotEmptyStrings()

                                it.string(KEY_REPO_NAME) -> name = valueAsString
                                it.string(KEY_REPO_DESC) -> description = valueAsString
                                it.number(KEY_REPO_VER) -> version = valueAsInt
                                it.number(KEY_REPO_TIME) -> timestamp = valueAsLong
                                else -> skipChildren()
                            }
                        }
                        val realMirrors = (
                            if (address.isNotEmpty()) {
                                listOf(address)
                            } else {
                                emptyList()
                            }
                            ) + mirrors
                        callback.onRepository(
                            mirrors = realMirrors.distinct(),
                            name = name,
                            description = description,
                            version = version,
                            timestamp = timestamp
                        )
                    }

                    key.array(DICT_PRODUCT) -> forEach(JsonToken.START_OBJECT) {
                        val product = parseProduct(repositoryId)
                        callback.onProduct(product)
                    }

                    key.dictionary(DICT_RELEASE) -> forEachKey {
                        if (it.token == JsonToken.START_ARRAY) {
                            val packageName = it.key
                            val releases = collectNotNull(JsonToken.START_OBJECT) { parseRelease() }
                            callback.onReleases(packageName, releases)
                        } else {
                            skipChildren()
                        }
                    }

                    else -> skipChildren()
                }
            }
        }
    }

    private const val KEY_PRODUCT_PACKAGENAME = "packageName"
    private const val KEY_PRODUCT_NAME = "name"
    private const val KEY_PRODUCT_SUMMARY = "summary"
    private const val KEY_PRODUCT_DESCRIPTION = "description"
    private const val KEY_PRODUCT_ICON = "icon"
    private const val KEY_PRODUCT_AUTHORNAME = "authorName"
    private const val KEY_PRODUCT_AUTHOREMAIL = "authorEmail"
    private const val KEY_PRODUCT_AUTHORWEBSITE = "authorWebSite"
    private const val KEY_PRODUCT_SOURCECODE = "sourceCode"
    private const val KEY_PRODUCT_CHANGELOG = "changelog"
    private const val KEY_PRODUCT_WEBSITE = "webSite"
    private const val KEY_PRODUCT_ISSUETRACKER = "issueTracker"
    private const val KEY_PRODUCT_ADDED = "added"
    private const val KEY_PRODUCT_LASTUPDATED = "lastUpdated"
    private const val KEY_PRODUCT_SUGGESTEDVERSIONCODE = "suggestedVersionCode"
    private const val KEY_PRODUCT_CATEGORIES = "categories"
    private const val KEY_PRODUCT_ANTIFEATURES = "antiFeatures"
    private const val KEY_PRODUCT_LICENSE = "license"
    private const val KEY_PRODUCT_DONATE = "donate"
    private const val KEY_PRODUCT_BITCOIN = "bitcoin"
    private const val KEY_PRODUCT_LIBERAPAYID = "liberapay"
    private const val KEY_PRODUCT_LITECOIN = "litecoin"
    private const val KEY_PRODUCT_OPENCOLLECTIVE = "openCollective"
    private const val KEY_PRODUCT_LOCALIZED = "localized"
    private const val KEY_PRODUCT_WHATSNEW = "whatsNew"
    private const val KEY_PRODUCT_PHONE_SCREENSHOTS = "phoneScreenshots"
    private const val KEY_PRODUCT_SEVEN_INCH_SCREENSHOTS = "sevenInchScreenshots"
    private const val KEY_PRODUCT_TEN_INCH_SCREENSHOTS = "tenInchScreenshots"
    private const val KEY_PRODUCT_WEAR_SCREENSHOTS = "wearScreenshots"
    private const val KEY_PRODUCT_TV_SCREENSHOTS = "tvScreenshots"
    private const val KEY_PRODUCT_VIDEO = "video"

    private fun JsonParser.parseProduct(repositoryId: Long): Product {
        var packageName = ""
        var nameFallback = ""
        var summaryFallback = ""
        var descriptionFallback = ""
        var icon = ""
        var authorName = ""
        var authorEmail = ""
        var authorWeb = ""
        var source = ""
        var changelog = ""
        var web = ""
        var tracker = ""
        var added = 0L
        var updated = 0L
        var suggestedVersionCode = 0L
        var categories = emptyList<String>()
        var antiFeatures = emptyList<String>()
        val licenses = mutableListOf<String>()
        val donates = mutableListOf<Product.Donate>()
        val localizedMap = mutableMapOf<String, Localized>()
        forEachKey { key ->
            when {
                key.string(KEY_PRODUCT_PACKAGENAME) -> packageName = valueAsString
                key.string(KEY_PRODUCT_NAME) -> nameFallback = valueAsString
                key.string(KEY_PRODUCT_SUMMARY) -> summaryFallback = valueAsString
                key.string(KEY_PRODUCT_DESCRIPTION) -> descriptionFallback = valueAsString
                key.string(KEY_PRODUCT_ICON) -> icon = validateIcon(valueAsString)
                key.string(KEY_PRODUCT_AUTHORNAME) -> authorName = valueAsString
                key.string(KEY_PRODUCT_AUTHOREMAIL) -> authorEmail = valueAsString
                key.string(KEY_PRODUCT_AUTHORWEBSITE) -> authorWeb = valueAsString
                key.string(KEY_PRODUCT_SOURCECODE) -> source = valueAsString
                key.string(KEY_PRODUCT_CHANGELOG) -> changelog = valueAsString
                key.string(KEY_PRODUCT_WEBSITE) -> web = valueAsString
                key.string(KEY_PRODUCT_ISSUETRACKER) -> tracker = valueAsString
                key.number(KEY_PRODUCT_ADDED) -> added = valueAsLong
                key.number(KEY_PRODUCT_LASTUPDATED) -> updated = valueAsLong
                key.string(KEY_PRODUCT_SUGGESTEDVERSIONCODE) ->
                    suggestedVersionCode =
                        valueAsString.toLongOrNull() ?: 0L

                key.array(KEY_PRODUCT_CATEGORIES) -> categories = collectDistinctNotEmptyStrings()
                key.array(KEY_PRODUCT_ANTIFEATURES) -> antiFeatures =
                    collectDistinctNotEmptyStrings()

                key.string(KEY_PRODUCT_LICENSE) -> licenses += valueAsString.split(',')
                    .filter { it.isNotEmpty() }

                key.string(KEY_PRODUCT_DONATE) -> donates += Regular(valueAsString)
                key.string(KEY_PRODUCT_BITCOIN) -> donates += Bitcoin(valueAsString)
                key.string(KEY_PRODUCT_LIBERAPAYID) -> donates += Liberapay(valueAsString)
                key.string(KEY_PRODUCT_LITECOIN) -> donates += Litecoin(valueAsString)
                key.string(KEY_PRODUCT_OPENCOLLECTIVE) -> donates += OpenCollective(valueAsString)

                key.dictionary(KEY_PRODUCT_LOCALIZED) -> forEachKey { localizedKey ->
                    if (localizedKey.token == JsonToken.START_OBJECT) {
                        val locale = localizedKey.key
                        var name = ""
                        var summary = ""
                        var description = ""
                        var whatsNew = ""
                        var metadataIcon = ""
                        var phone = emptyList<String>()
                        var smallTablet = emptyList<String>()
                        var largeTablet = emptyList<String>()
                        var wear = emptyList<String>()
                        var tv = emptyList<String>()
                        var video = emptyList<String>()
                        forEachKey {
                            when {
                                it.string(KEY_PRODUCT_NAME) -> name = valueAsString
                                it.string(KEY_PRODUCT_SUMMARY) -> summary = valueAsString
                                it.string(KEY_PRODUCT_DESCRIPTION) -> description = valueAsString
                                it.string(KEY_PRODUCT_WHATSNEW) -> whatsNew = valueAsString
                                it.string(KEY_PRODUCT_ICON) -> metadataIcon = valueAsString
                                it.string(KEY_PRODUCT_VIDEO) -> video = listOf(valueAsString)
                                it.array(KEY_PRODUCT_PHONE_SCREENSHOTS) ->
                                    phone = collectDistinctNotEmptyStrings()

                                it.array(KEY_PRODUCT_SEVEN_INCH_SCREENSHOTS) ->
                                    smallTablet = collectDistinctNotEmptyStrings()

                                it.array(KEY_PRODUCT_TEN_INCH_SCREENSHOTS) ->
                                    largeTablet = collectDistinctNotEmptyStrings()

                                it.array(KEY_PRODUCT_WEAR_SCREENSHOTS) ->
                                    wear = collectDistinctNotEmptyStrings()

                                it.array(KEY_PRODUCT_TV_SCREENSHOTS) ->
                                    tv = collectDistinctNotEmptyStrings()

                                else -> skipChildren()
                            }
                        }
                        val isScreenshotEmpty =
                            arrayOf(video, phone, smallTablet, largeTablet, wear, tv)
                                .any { it.isNotEmpty() }
                        val screenshots =
                            if (isScreenshotEmpty) {
                                Screenshots(video, phone, smallTablet, largeTablet, wear, tv)
                            } else {
                                null
                            }
                        localizedMap[locale] = Localized(
                            name = name,
                            summary = summary,
                            description = description,
                            whatsNew = whatsNew,
                            metadataIcon = metadataIcon.nullIfEmpty()?.let { "$locale/$it" }
                                .orEmpty(),
                            screenshots = screenshots,
                        )
                    } else {
                        skipChildren()
                    }
                }

                else -> skipChildren()
            }
        }
        val name = localizedMap.findLocalizedString(nameFallback) { it.name }
        val summary = localizedMap.findLocalizedString(summaryFallback) { it.summary }
        val description =
            localizedMap.findLocalizedString(descriptionFallback) { it.description }.replace(
                "\n",
                "<br/>"
            )
        val whatsNew = localizedMap.findLocalizedString("") { it.whatsNew }.replace("\n", "<br/>")
        val metadataIcon = localizedMap.findLocalizedString("") { it.metadataIcon }.ifEmpty {
            localizedMap.firstNotNullOfOrNull { it.value.metadataIcon }.orEmpty()
        }
        val screenshotPairs =
            localizedMap.find { key, localized -> localized.screenshots?.let { Pair(key, it) } }
        val screenshots = screenshotPairs?.let { (key, screenshots) ->
            screenshots.video.map { Product.Screenshot(key, VIDEO, it) } +
                screenshots.phone.map { Product.Screenshot(key, PHONE, it) } +
                screenshots.smallTablet.map { Product.Screenshot(key, SMALL_TABLET, it) } +
                screenshots.largeTablet.map { Product.Screenshot(key, LARGE_TABLET, it) } +
                screenshots.wear.map { Product.Screenshot(key, WEAR, it) } +
                screenshots.tv.map { Product.Screenshot(key, TV, it) }
        }.orEmpty()
        return Product(
            repositoryId = repositoryId,
            packageName = packageName,
            name = name,
            summary = summary,
            description = description,
            whatsNew = whatsNew,
            icon = icon,
            metadataIcon = metadataIcon,
            author = Product.Author(authorName, authorEmail, authorWeb),
            source = source,
            changelog = changelog,
            web = web,
            tracker = tracker,
            added = added,
            updated = updated,
            suggestedVersionCode = suggestedVersionCode,
            categories = categories,
            antiFeatures = antiFeatures,
            licenses = licenses,
            donates = donates.sortedWith(DonateComparator),
            screenshots = screenshots,
            releases = emptyList()
        )
    }

    private const val KEY_RELEASE_VERSIONNAME = "versionName"
    private const val KEY_RELEASE_VERSIONCODE = "versionCode"
    private const val KEY_RELEASE_ADDED = "added"
    private const val KEY_RELEASE_SIZE = "size"
    private const val KEY_RELEASE_MINSDKVERSION = "minSdkVersion"
    private const val KEY_RELEASE_TARGETSDKVERSION = "targetSdkVersion"
    private const val KEY_RELEASE_MAXSDKVERSION = "maxSdkVersion"
    private const val KEY_RELEASE_SRCNAME = "srcname"
    private const val KEY_RELEASE_APKNAME = "apkName"
    private const val KEY_RELEASE_HASH = "hash"
    private const val KEY_RELEASE_HASHTYPE = "hashType"
    private const val KEY_RELEASE_SIG = "sig"
    private const val KEY_RELEASE_OBBMAINFILE = "obbMainFile"
    private const val KEY_RELEASE_OBBMAINFILESHA256 = "obbMainFileSha256"
    private const val KEY_RELEASE_OBBPATCHFILE = "obbPatchFile"
    private const val KEY_RELEASE_OBBPATCHFILESHA256 = "obbPatchFileSha256"
    private const val KEY_RELEASE_USESPERMISSION = "uses-permission"
    private const val KEY_RELEASE_USESPERMISSIONSDK23 = "uses-permission-sdk-23"
    private const val KEY_RELEASE_FEATURES = "features"
    private const val KEY_RELEASE_NATIVECODE = "nativecode"

    private fun JsonParser.parseRelease(): Release {
        var version = ""
        var versionCode = 0L
        var added = 0L
        var size = 0L
        var minSdkVersion = 0
        var targetSdkVersion = 0
        var maxSdkVersion = 0
        var source = ""
        var release = ""
        var hash = ""
        var hashTypeCandidate = ""
        var signature = ""
        var obbMain = ""
        var obbMainHash = ""
        var obbPatch = ""
        var obbPatchHash = ""
        val permissions = linkedSetOf<String>()
        var features = emptyList<String>()
        var platforms = emptyList<String>()
        forEachKey { key ->
            when {
                key.string(KEY_RELEASE_VERSIONNAME) -> version = valueAsString
                key.number(KEY_RELEASE_VERSIONCODE) -> versionCode = valueAsLong
                key.number(KEY_RELEASE_ADDED) -> added = valueAsLong
                key.number(KEY_RELEASE_SIZE) -> size = valueAsLong
                key.number(KEY_RELEASE_MINSDKVERSION) -> minSdkVersion = valueAsInt
                key.number(KEY_RELEASE_TARGETSDKVERSION) -> targetSdkVersion = valueAsInt
                key.number(KEY_RELEASE_MAXSDKVERSION) -> maxSdkVersion = valueAsInt
                key.string(KEY_RELEASE_SRCNAME) -> source = valueAsString
                key.string(KEY_RELEASE_APKNAME) -> release = valueAsString
                key.string(KEY_RELEASE_HASH) -> hash = valueAsString
                key.string(KEY_RELEASE_HASHTYPE) -> hashTypeCandidate = valueAsString
                key.string(KEY_RELEASE_SIG) -> signature = valueAsString
                key.string(KEY_RELEASE_OBBMAINFILE) -> obbMain = valueAsString
                key.string(KEY_RELEASE_OBBMAINFILESHA256) -> obbMainHash = valueAsString
                key.string(KEY_RELEASE_OBBPATCHFILE) -> obbPatch = valueAsString
                key.string(KEY_RELEASE_OBBPATCHFILESHA256) -> obbPatchHash = valueAsString
                key.array(KEY_RELEASE_USESPERMISSION) -> collectPermissions(permissions, 0)
                key.array(KEY_RELEASE_USESPERMISSIONSDK23) -> collectPermissions(permissions, 23)
                key.array(KEY_RELEASE_FEATURES) -> features = collectDistinctNotEmptyStrings()
                key.array(KEY_RELEASE_NATIVECODE) -> platforms = collectDistinctNotEmptyStrings()
                else -> skipChildren()
            }
        }
        val hashType =
            if (hash.isNotEmpty() && hashTypeCandidate.isEmpty()) "sha256" else hashTypeCandidate
        val obbMainHashType = if (obbMainHash.isNotEmpty()) "sha256" else ""
        val obbPatchHashType = if (obbPatchHash.isNotEmpty()) "sha256" else ""
        return Release(
            selected = false,
            version = version,
            versionCode = versionCode,
            added = added,
            size = size,
            minSdkVersion = minSdkVersion,
            targetSdkVersion = targetSdkVersion,
            maxSdkVersion = maxSdkVersion,
            source = source,
            release = release,
            hash = hash,
            hashType = hashType,
            signature = signature,
            obbMain = obbMain,
            obbMainHash = obbMainHash,
            obbMainHashType = obbMainHashType,
            obbPatch = obbPatch,
            obbPatchHash = obbPatchHash,
            obbPatchHashType = obbPatchHashType,
            permissions = permissions.toList(),
            features = features,
            platforms = platforms,
            incompatibilities = emptyList()
        )
    }

    private fun JsonParser.collectPermissions(permissions: LinkedHashSet<String>, minSdk: Int) {
        forEach(JsonToken.START_ARRAY) {
            val firstToken = nextToken()
            val permission = if (firstToken == JsonToken.VALUE_STRING) valueAsString else ""
            if (firstToken != JsonToken.END_ARRAY) {
                val secondToken = nextToken()
                val maxSdk = if (secondToken == JsonToken.VALUE_NUMBER_INT) valueAsInt else 0
                if (permission.isNotEmpty() &&
                    SdkCheck.sdk >= minSdk && (
                        maxSdk <= 0 ||
                            SdkCheck.sdk <= maxSdk
                        )
                ) {
                    permissions.add(permission)
                }
                if (secondToken != JsonToken.END_ARRAY) {
                    while (true) {
                        val token = nextToken()
                        if (token == JsonToken.END_ARRAY) {
                            break
                        } else if (token.isStructStart) {
                            skipChildren()
                        }
                    }
                }
            }
        }
    }

    private fun validateIcon(icon: String): String {
        return if (icon.endsWith(".xml")) "" else icon
    }
}
