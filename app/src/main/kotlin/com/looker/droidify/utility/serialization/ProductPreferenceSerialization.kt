package com.looker.droidify.utility.serialization

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.core.JsonParser
import com.looker.core.common.extension.forEachKey
import com.looker.droidify.model.ProductPreference

fun ProductPreference.serialize(generator: JsonGenerator) {
    generator.writeBooleanField("ignoreUpdates", ignoreUpdates)
    generator.writeNumberField("ignoreVersionCode", ignoreVersionCode)
}

fun JsonParser.productPreference(): ProductPreference {
    var ignoreUpdates = false
    var ignoreVersionCode = 0L
    forEachKey {
        when {
            it.boolean("ignoreUpdates") -> ignoreUpdates = valueAsBoolean
            it.number("ignoreVersionCode") -> ignoreVersionCode = valueAsLong
            else -> skipChildren()
        }
    }
    return ProductPreference(ignoreUpdates, ignoreVersionCode)
}
