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
    generator.writeNumberField("repositoryId", repositoryId)
    generator.writeNumberField("serialVersion", 1)
    generator.writeStringField("packageName", packageName)
    generator.writeStringField("name", name)
    generator.writeStringField("summary", summary)
    generator.writeStringField("description", description)
    generator.writeStringField("whatsNew", whatsNew)
    generator.writeStringField("icon", icon)
    generator.writeStringField("metadataIcon", metadataIcon)
    generator.writeStringField("authorName", author.name)
    generator.writeStringField("authorEmail", author.email)
    generator.writeStringField("authorWeb", author.web)
    generator.writeStringField("source", source)
    generator.writeStringField("changelog", changelog)
    generator.writeStringField("web", web)
    generator.writeStringField("tracker", tracker)
    generator.writeNumberField("added", added)
    generator.writeNumberField("updated", updated)
    generator.writeNumberField("suggestedVersionCode", suggestedVersionCode)
    generator.writeArray("categories") { categories.forEach(::writeString) }
    generator.writeArray("antiFeatures") { antiFeatures.forEach(::writeString) }
    generator.writeArray("licenses") { licenses.forEach(::writeString) }
    generator.writeArray("donates") {
        donates.forEach {
            writeDictionary {
                when (it) {
                    is Product.Donate.Regular -> {
                        writeStringField("type", "")
                        writeStringField("url", it.url)
                    }

                    is Product.Donate.Bitcoin -> {
                        writeStringField("type", "bitcoin")
                        writeStringField("address", it.address)
                    }

                    is Product.Donate.Litecoin -> {
                        writeStringField("type", "litecoin")
                        writeStringField("address", it.address)
                    }

                    is Product.Donate.Flattr -> {
                        writeStringField("type", "flattr")
                        writeStringField("id", it.id)
                    }

                    is Product.Donate.Liberapay -> {
                        writeStringField("type", "liberapay")
                        writeStringField("id", it.id)
                    }

                    is Product.Donate.OpenCollective -> {
                        writeStringField("type", "openCollective")
                        writeStringField("id", it.id)
                    }
                }::class
            }
        }
    }
    generator.writeArray("screenshots") {
        screenshots.forEach {
            writeDictionary {
                writeStringField("locale", it.locale)
                writeStringField("type", it.type.jsonName)
                writeStringField("path", it.path)
            }
        }
    }
    generator.writeArray("releases") { releases.forEach { writeDictionary { it.serialize(this) } } }
}

fun JsonParser.product(): Product {
    var repositoryId = 0L
    var packageName = ""
    var name = ""
    var summary = ""
    var description = ""
    var whatsNew = ""
    var icon = ""
    var metadataIcon = ""
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
    var licenses = emptyList<String>()
    var donates = emptyList<Product.Donate>()
    var screenshots = emptyList<Product.Screenshot>()
    var releases = emptyList<Release>()
    forEachKey { it ->
        when {
            it.string("repositoryId") -> repositoryId = valueAsLong
            it.string("packageName") -> packageName = valueAsString
            it.string("name") -> name = valueAsString
            it.string("summary") -> summary = valueAsString
            it.string("description") -> description = valueAsString
            it.string("whatsNew") -> whatsNew = valueAsString
            it.string("icon") -> icon = valueAsString
            it.string("metadataIcon") -> metadataIcon = valueAsString
            it.string("authorName") -> authorName = valueAsString
            it.string("authorEmail") -> authorEmail = valueAsString
            it.string("authorWeb") -> authorWeb = valueAsString
            it.string("source") -> source = valueAsString
            it.string("changelog") -> changelog = valueAsString
            it.string("web") -> web = valueAsString
            it.string("tracker") -> tracker = valueAsString
            it.number("added") -> added = valueAsLong
            it.number("updated") -> updated = valueAsLong
            it.number("suggestedVersionCode") -> suggestedVersionCode = valueAsLong
            it.array("categories") -> categories = collectNotNullStrings()
            it.array("antiFeatures") -> antiFeatures = collectNotNullStrings()
            it.array("licenses") -> licenses = collectNotNullStrings()
            it.array("donates") -> donates = collectNotNull(JsonToken.START_OBJECT) {
                var type = ""
                var url = ""
                var address = ""
                var id = ""
                forEachKey {
                    when {
                        it.string("type") -> type = valueAsString
                        it.string("url") -> url = valueAsString
                        it.string("address") -> address = valueAsString
                        it.string("id") -> id = valueAsString
                        else -> skipChildren()
                    }
                }
                when (type) {
                    "" -> Product.Donate.Regular(url)
                    "bitcoin" -> Product.Donate.Bitcoin(address)
                    "litecoin" -> Product.Donate.Litecoin(address)
                    "flattr" -> Product.Donate.Flattr(id)
                    "liberapay" -> Product.Donate.Liberapay(id)
                    "openCollective" -> Product.Donate.OpenCollective(id)
                    else -> null
                }
            }

            it.array("screenshots") ->
                screenshots =
                    collectNotNull(JsonToken.START_OBJECT) {
                        var locale = ""
                        var type = ""
                        var path = ""
                        forEachKey {
                            when {
                                it.string("locale") -> locale = valueAsString
                                it.string("type") -> type = valueAsString
                                it.string("path") -> path = valueAsString
                                else -> skipChildren()
                            }
                        }
                        Product.Screenshot.Type.entries.find { it.jsonName == type }
                            ?.let { Product.Screenshot(locale, it, path) }
                    }

            it.array("releases") ->
                releases =
                    collectNotNull(JsonToken.START_OBJECT) { release() }

            else -> skipChildren()
        }
    }
    return Product(
        repositoryId,
        packageName,
        name,
        summary,
        description,
        whatsNew,
        icon,
        metadataIcon,
        Product.Author(authorName, authorEmail, authorWeb),
        source,
        changelog,
        web,
        tracker,
        added,
        updated,
        suggestedVersionCode,
        categories,
        antiFeatures,
        licenses,
        donates,
        screenshots,
        releases
    )
}
