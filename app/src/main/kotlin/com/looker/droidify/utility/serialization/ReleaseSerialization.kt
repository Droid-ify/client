package com.looker.droidify.utility.serialization

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.core.JsonToken
import com.looker.core.common.extension.collectNotNull
import com.looker.core.common.extension.collectNotNullStrings
import com.looker.core.common.extension.forEachKey
import com.looker.core.common.extension.writeArray
import com.looker.core.common.extension.writeDictionary
import com.looker.droidify.model.Release

fun Release.serialize(generator: JsonGenerator) {
    generator.writeNumberField("serialVersion", 1)
    generator.writeBooleanField("selected", selected)
    generator.writeStringField("version", version)
    generator.writeNumberField("versionCode", versionCode)
    generator.writeNumberField("added", added)
    generator.writeNumberField("size", size)
    generator.writeNumberField("minSdkVersion", minSdkVersion)
    generator.writeNumberField("targetSdkVersion", targetSdkVersion)
    generator.writeNumberField("maxSdkVersion", maxSdkVersion)
    generator.writeStringField("source", source)
    generator.writeStringField("release", release)
    generator.writeStringField("hash", hash)
    generator.writeStringField("hashType", hashType)
    generator.writeStringField("signature", signature)
    generator.writeStringField("obbMain", obbMain)
    generator.writeStringField("obbMainHash", obbMainHash)
    generator.writeStringField("obbMainHashType", obbMainHashType)
    generator.writeStringField("obbPatch", obbPatch)
    generator.writeStringField("obbPatchHash", obbPatchHash)
    generator.writeStringField("obbPatchHashType", obbPatchHashType)
    generator.writeArray("permissions") { permissions.forEach { writeString(it) } }
    generator.writeArray("features") { features.forEach { writeString(it) } }
    generator.writeArray("platforms") { platforms.forEach { writeString(it) } }
    generator.writeArray("incompatibilities") {
        incompatibilities.forEach {
            writeDictionary {
                when (it) {
                    is Release.Incompatibility.MinSdk -> {
                        writeStringField("type", "minSdk")
                    }

                    is Release.Incompatibility.MaxSdk -> {
                        writeStringField("type", "maxSdk")
                    }

                    is Release.Incompatibility.Platform -> {
                        writeStringField("type", "platform")
                    }

                    is Release.Incompatibility.Feature -> {
                        writeStringField("type", "feature")
                        writeStringField("feature", it.feature)
                    }
                }::class
            }
        }
    }
}

fun JsonParser.release(): Release {
    var selected = false
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
    var hashType = ""
    var signature = ""
    var obbMain = ""
    var obbMainHash = ""
    var obbMainHashType = ""
    var obbPatch = ""
    var obbPatchHash = ""
    var obbPatchHashType = ""
    var permissions = emptyList<String>()
    var features = emptyList<String>()
    var platforms = emptyList<String>()
    var incompatibilities = emptyList<Release.Incompatibility>()
    forEachKey { it ->
        when {
            it.boolean("selected") -> selected = valueAsBoolean
            it.string("version") -> version = valueAsString
            it.number("versionCode") -> versionCode = valueAsLong
            it.number("added") -> added = valueAsLong
            it.number("size") -> size = valueAsLong
            it.number("minSdkVersion") -> minSdkVersion = valueAsInt
            it.number("targetSdkVersion") -> targetSdkVersion = valueAsInt
            it.number("maxSdkVersion") -> maxSdkVersion = valueAsInt
            it.string("source") -> source = valueAsString
            it.string("release") -> release = valueAsString
            it.string("hash") -> hash = valueAsString
            it.string("hashType") -> hashType = valueAsString
            it.string("signature") -> signature = valueAsString
            it.string("obbMain") -> obbMain = valueAsString
            it.string("obbMainHash") -> obbMainHash = valueAsString
            it.string("obbMainHashType") -> obbMainHashType = valueAsString
            it.string("obbPatch") -> obbPatch = valueAsString
            it.string("obbPatchHash") -> obbPatchHash = valueAsString
            it.string("obbPatchHashType") -> obbPatchHashType = valueAsString
            it.array("permissions") -> permissions = collectNotNullStrings()
            it.array("features") -> features = collectNotNullStrings()
            it.array("platforms") -> platforms = collectNotNullStrings()
            it.array("incompatibilities") ->
                incompatibilities =
                    collectNotNull(JsonToken.START_OBJECT) {
                        var type = ""
                        var feature = ""
                        forEachKey {
                            when {
                                it.string("type") -> type = valueAsString
                                it.string("feature") -> feature = valueAsString
                                else -> skipChildren()
                            }
                        }
                        when (type) {
                            "minSdk" -> Release.Incompatibility.MinSdk
                            "maxSdk" -> Release.Incompatibility.MaxSdk
                            "platform" -> Release.Incompatibility.Platform
                            "feature" -> Release.Incompatibility.Feature(feature)
                            else -> null
                        }
                    }

            else -> skipChildren()
        }
    }
    return Release(
        selected,
        version,
        versionCode,
        added,
        size,
        minSdkVersion,
        targetSdkVersion,
        maxSdkVersion,
        source,
        release,
        hash,
        hashType,
        signature,
        obbMain,
        obbMainHash,
        obbMainHashType,
        obbPatch,
        obbPatchHash,
        obbPatchHashType,
        permissions,
        features,
        platforms,
        incompatibilities
    )
}
