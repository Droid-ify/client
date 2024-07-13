package com.looker.droidify.utility.serialization

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.core.JsonParser
import com.looker.core.common.extension.collectNotNullStrings
import com.looker.core.common.extension.forEachKey
import com.looker.core.common.extension.writeArray
import com.looker.droidify.model.Repository

fun Repository.serialize(generator: JsonGenerator) {
    generator.writeNumberField("serialVersion", 1)
    generator.writeNumberField("id", id)
    generator.writeStringField("address", address)
    generator.writeArray("mirrors") { mirrors.forEach { writeString(it) } }
    generator.writeStringField("name", name)
    generator.writeStringField("description", description)
    generator.writeNumberField("version", version)
    generator.writeBooleanField("enabled", enabled)
    generator.writeStringField("fingerprint", fingerprint)
    generator.writeStringField("lastModified", lastModified)
    generator.writeStringField("entityTag", entityTag)
    generator.writeNumberField("updated", updated)
    generator.writeNumberField("timestamp", timestamp)
    generator.writeStringField("authentication", authentication)
}

fun JsonParser.repository(): Repository {
    var id = -1L
    var address = ""
    var mirrors = emptyList<String>()
    var name = ""
    var description = ""
    var version = 0
    var enabled = false
    var fingerprint = ""
    var lastModified = ""
    var entityTag = ""
    var updated = 0L
    var timestamp = 0L
    var authentication = ""
    forEachKey {
        when {
            it.string("id") -> id = valueAsLong
            it.string("address") -> address = valueAsString
            it.array("mirrors") -> mirrors = collectNotNullStrings()
            it.string("name") -> name = valueAsString
            it.string("description") -> description = valueAsString
            it.number("version") -> version = valueAsInt
            it.boolean("enabled") -> enabled = valueAsBoolean
            it.string("fingerprint") -> fingerprint = valueAsString
            it.string("lastModified") -> lastModified = valueAsString
            it.string("entityTag") -> entityTag = valueAsString
            it.number("updated") -> updated = valueAsLong
            it.number("timestamp") -> timestamp = valueAsLong
            it.string("authentication") -> authentication = valueAsString
            else -> skipChildren()
        }
    }
    return Repository(
        id, address, mirrors, name, description, version, enabled, fingerprint,
        lastModified, entityTag, updated, timestamp, authentication
    )
}
