package com.looker.droidify.utility.serialization

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.core.JsonParser
import com.looker.core.common.extension.forEachKey
import com.looker.droidify.model.ProductItem

fun ProductItem.serialize(generator: JsonGenerator) {
    generator.writeNumberField("serialVersion", 1)
    generator.writeNumberField("repositoryId", repositoryId)
    generator.writeStringField("packageName", packageName)
    generator.writeStringField("name", name)
    generator.writeStringField("summary", summary)
    generator.writeStringField("icon", icon)
    generator.writeStringField("metadataIcon", metadataIcon)
    generator.writeStringField("version", version)
    generator.writeStringField("installedVersion", installedVersion)
    generator.writeBooleanField("compatible", compatible)
    generator.writeBooleanField("canUpdate", canUpdate)
    generator.writeNumberField("matchRank", matchRank)
}

fun JsonParser.productItem(): ProductItem {
    var repositoryId = 0L
    var packageName = ""
    var name = ""
    var summary = ""
    var icon = ""
    var metadataIcon = ""
    var version = ""
    var installedVersion = ""
    var compatible = false
    var canUpdate = false
    var matchRank = 0
    forEachKey {
        when {
            it.number("repositoryId") -> repositoryId = valueAsLong
            it.string("packageName") -> packageName = valueAsString
            it.string("name") -> name = valueAsString
            it.string("summary") -> summary = valueAsString
            it.string("icon") -> icon = valueAsString
            it.string("metadataIcon") -> metadataIcon = valueAsString
            it.string("version") -> version = valueAsString
            it.string("installedVersion") -> installedVersion = valueAsString
            it.boolean("compatible") -> compatible = valueAsBoolean
            it.boolean("canUpdate") -> canUpdate = valueAsBoolean
            it.number("matchRank") -> matchRank = valueAsInt
            else -> skipChildren()
        }
    }
    return ProductItem(
        repositoryId, packageName, name, summary, icon, metadataIcon,
        version, installedVersion, compatible, canUpdate, matchRank
    )
}
