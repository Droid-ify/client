package com.looker.droidify.utility.serialization

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.core.JsonToken
import com.looker.core.common.extension.collectNotNull
import com.looker.core.common.extension.collectNotNullStrings
import com.looker.core.common.extension.forEachKey
import com.looker.core.common.extension.writeArray
import com.looker.core.common.extension.writeDictionary
import com.looker.droidify.model.Product
import com.looker.droidify.model.Release

fun Product.serialize(generator: JsonGenerator) {
    generator.writeNumberField(REPOSITORYID, repositoryId)
    generator.writeNumberField(SERIALVERSION, 1)
    generator.writeStringField(PACKAGENAME, packageName)
    generator.writeStringField(NAME, name)
    generator.writeStringField(SUMMARY, summary)
    generator.writeStringField(DESCRIPTION, description)
    generator.writeStringField(WHATSNEW, whatsNew)
    generator.writeStringField(ICON, icon)
    generator.writeStringField(METADATAICON, metadataIcon)
    generator.writeStringField(AUTHORNAME, author.name)
    generator.writeStringField(AUTHOREMAIL, author.email)
    generator.writeStringField(AUTHORWEB, author.web)
    generator.writeStringField(SOURCE, source)
    generator.writeStringField(CHANGELOG, changelog)
    generator.writeStringField(WEB, web)
    generator.writeStringField(TRACKER, tracker)
    generator.writeNumberField(ADDED, added)
    generator.writeNumberField(UPDATED, updated)
    generator.writeNumberField(SUGGESTEDVERSIONCODE, suggestedVersionCode)
    generator.writeArray(CATEGORIES) { categories.forEach(::writeString) }
    generator.writeArray(ANTIFEATURES) { antiFeatures.forEach(::writeString) }
    generator.writeArray(LICENSES) { licenses.forEach(::writeString) }
    generator.writeArray(DONATES) {
        donates.forEach {
            writeDictionary {
                when (it) {
                    is Product.Donate.Regular -> {
                        writeStringField(TYPE, DONATION_EMPTY)
                        writeStringField(URL, it.url)
                    }

                    is Product.Donate.Bitcoin -> {
                        writeStringField(TYPE, DONATION_BITCOIN)
                        writeStringField(ADDRESS, it.address)
                    }

                    is Product.Donate.Litecoin -> {
                        writeStringField(TYPE, DONATION_LITECOIN)
                        writeStringField(ADDRESS, it.address)
                    }

                    is Product.Donate.Flattr -> {
                        writeStringField(TYPE, DONATION_FLATTR)
                        writeStringField(ID, it.id)
                    }

                    is Product.Donate.Liberapay -> {
                        writeStringField(TYPE, DONATION_LIBERAPAY)
                        writeStringField(ID, it.id)
                    }

                    is Product.Donate.OpenCollective -> {
                        writeStringField(TYPE, DONATION_OPENCOLLECTIVE)
                        writeStringField(ID, it.id)
                    }
                }::class
            }
        }
    }
    generator.writeArray(SCREENSHOTS) {
        screenshots.forEach {
            writeDictionary {
                writeStringField(LOCALE, it.locale)
                writeStringField(TYPE, it.type.jsonName)
                writeStringField(PATH, it.path)
            }
        }
    }
    generator.writeArray(RELEASES) { releases.forEach { writeDictionary { it.serialize(this) } } }
}

fun JsonParser.product(): Product {
    var repositoryId = 0L
    var packageName = KEY_EMPTY
    var name = KEY_EMPTY
    var summary = KEY_EMPTY
    var description = KEY_EMPTY
    var whatsNew = KEY_EMPTY
    var icon = KEY_EMPTY
    var metadataIcon = KEY_EMPTY
    var authorName = KEY_EMPTY
    var authorEmail = KEY_EMPTY
    var authorWeb = KEY_EMPTY
    var source = KEY_EMPTY
    var changelog = KEY_EMPTY
    var web = KEY_EMPTY
    var tracker = KEY_EMPTY
    var added = 0L
    var updated = 0L
    var suggestedVersionCode = 0L
    var categories = emptyList<String>()
    var antiFeatures = emptyList<String>()
    var licenses = emptyList<String>()
    var donates = emptyList<Product.Donate>()
    var screenshots = emptyList<Product.Screenshot>()
    var releases = emptyList<Release>()
    forEachKey { key ->
        when {
            key.string(REPOSITORYID) -> repositoryId = valueAsLong
            key.string(PACKAGENAME) -> packageName = valueAsString
            key.string(NAME) -> name = valueAsString
            key.string(SUMMARY) -> summary = valueAsString
            key.string(DESCRIPTION) -> description = valueAsString
            key.string(WHATSNEW) -> whatsNew = valueAsString
            key.string(ICON) -> icon = valueAsString
            key.string(METADATAICON) -> metadataIcon = valueAsString
            key.string(AUTHORNAME) -> authorName = valueAsString
            key.string(AUTHOREMAIL) -> authorEmail = valueAsString
            key.string(AUTHORWEB) -> authorWeb = valueAsString
            key.string(SOURCE) -> source = valueAsString
            key.string(CHANGELOG) -> changelog = valueAsString
            key.string(WEB) -> web = valueAsString
            key.string(TRACKER) -> tracker = valueAsString
            key.number(ADDED) -> added = valueAsLong
            key.number(UPDATED) -> updated = valueAsLong
            key.number(SUGGESTEDVERSIONCODE) -> suggestedVersionCode = valueAsLong
            key.array(CATEGORIES) -> categories = collectNotNullStrings()
            key.array(ANTIFEATURES) -> antiFeatures = collectNotNullStrings()
            key.array(LICENSES) -> licenses = collectNotNullStrings()
            key.array(DONATES) -> donates = collectNotNull(JsonToken.START_OBJECT) {
                var type = KEY_EMPTY
                var url = KEY_EMPTY
                var address = KEY_EMPTY
                var id = KEY_EMPTY
                forEachKey {
                    when {
                        it.string(TYPE) -> type = valueAsString
                        it.string(URL) -> url = valueAsString
                        it.string(ADDRESS) -> address = valueAsString
                        it.string(ID) -> id = valueAsString
                        else -> skipChildren()
                    }
                }
                when (type) {
                    DONATION_EMPTY -> Product.Donate.Regular(url)
                    DONATION_BITCOIN -> Product.Donate.Bitcoin(address)
                    DONATION_LITECOIN -> Product.Donate.Litecoin(address)
                    DONATION_FLATTR -> Product.Donate.Flattr(id)
                    DONATION_LIBERAPAY -> Product.Donate.Liberapay(id)
                    DONATION_OPENCOLLECTIVE -> Product.Donate.OpenCollective(id)
                    else -> null
                }
            }

            key.array(SCREENSHOTS) ->
                screenshots =
                    collectNotNull(JsonToken.START_OBJECT) {
                        var locale = KEY_EMPTY
                        var type = KEY_EMPTY
                        var path = KEY_EMPTY
                        forEachKey {
                            when {
                                it.string(LOCALE) -> locale = valueAsString
                                it.string(TYPE) -> type = valueAsString
                                it.string(PATH) -> path = valueAsString
                                else -> skipChildren()
                            }
                        }
                        Product.Screenshot.Type.entries.find { it.jsonName == type }
                            ?.let { Product.Screenshot(locale, it, path) }
                    }

            key.array(RELEASES) ->
                releases =
                    collectNotNull(JsonToken.START_OBJECT) { release() }

            else -> skipChildren()
        }
    }
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
        donates = donates,
        screenshots = screenshots,
        releases = releases
    )
}

private const val REPOSITORYID = "repositoryId"
private const val SERIALVERSION = "serialVersion"
private const val PACKAGENAME = "packageName"
private const val NAME = "name"
private const val SUMMARY = "summary"
private const val DESCRIPTION = "description"
private const val WHATSNEW = "whatsNew"
private const val ICON = "icon"
private const val METADATAICON = "metadataIcon"
private const val AUTHORNAME = "authorName"
private const val AUTHOREMAIL = "authorEmail"
private const val AUTHORWEB = "authorWeb"
private const val SOURCE = "source"
private const val CHANGELOG = "changelog"
private const val WEB = "web"
private const val TRACKER = "tracker"
private const val ADDED = "added"
private const val UPDATED = "updated"
private const val SUGGESTEDVERSIONCODE = "suggestedVersionCode"
private const val CATEGORIES = "categories"
private const val ANTIFEATURES = "antiFeatures"
private const val LICENSES = "licenses"
private const val DONATES = "donates"
private const val ADDRESS = "address"
private const val URL = "url"
private const val TYPE = "type"
private const val ID = "id"
private const val SCREENSHOTS = "screenshots"
private const val RELEASES = "releases"
private const val PATH = "path"
private const val LOCALE = "locale"

private const val KEY_EMPTY = ""
private const val DONATION_EMPTY = ""
private const val DONATION_BITCOIN = "bitcoin"
private const val DONATION_LITECOIN = "litecoin"
private const val DONATION_FLATTR = "flattr"
private const val DONATION_LIBERAPAY = "liberapay"
private const val DONATION_OPENCOLLECTIVE = "openCollective"
