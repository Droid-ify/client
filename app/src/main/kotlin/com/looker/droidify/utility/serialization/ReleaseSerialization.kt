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
    generator.writeNumberField(SERIALVERSION, 1)
    generator.writeBooleanField(SELECTED, selected)
    generator.writeStringField(VERSION, version)
    generator.writeNumberField(VERSIONCODE, versionCode)
    generator.writeNumberField(ADDED, added)
    generator.writeNumberField(SIZE, size)
    generator.writeNumberField(MINSDKVERSION, minSdkVersion)
    generator.writeNumberField(TARGETSDKVERSION, targetSdkVersion)
    generator.writeNumberField(MAXSDKVERSION, maxSdkVersion)
    generator.writeStringField(SOURCE, source)
    generator.writeStringField(RELEASE, release)
    generator.writeStringField(HASH, hash)
    generator.writeStringField(HASHTYPE, hashType)
    generator.writeStringField(SIGNATURE, signature)
    generator.writeStringField(OBBMAIN, obbMain)
    generator.writeStringField(OBBMAINHASH, obbMainHash)
    generator.writeStringField(OBBMAINHASHTYPE, obbMainHashType)
    generator.writeStringField(OBBPATCH, obbPatch)
    generator.writeStringField(OBBPATCHHASH, obbPatchHash)
    generator.writeStringField(OBBPATCHHASHTYPE, obbPatchHashType)
    generator.writeArray(PERMISSIONS) { permissions.forEach { writeString(it) } }
    generator.writeArray(FEATURES) { features.forEach { writeString(it) } }
    generator.writeArray(PLATFORMS) { platforms.forEach { writeString(it) } }
    generator.writeArray(INCOMPATIBILITIES) {
        incompatibilities.forEach {
            writeDictionary {
                when (it) {
                    is Release.Incompatibility.MinSdk -> {
                        writeStringField(INCOMPATIBILITY_TYPE, MIN_SDK)
                    }

                    is Release.Incompatibility.MaxSdk -> {
                        writeStringField(INCOMPATIBILITY_TYPE, MAX_SDK)
                    }

                    is Release.Incompatibility.Platform -> {
                        writeStringField(INCOMPATIBILITY_TYPE, PLATFORM)
                    }

                    is Release.Incompatibility.Feature -> {
                        writeStringField(INCOMPATIBILITY_TYPE, INCOMPATIBILITY_FEATURE)
                        writeStringField(INCOMPATIBILITY_FEATURE, it.feature)
                    }
                }::class
            }
        }
    }
}


fun JsonParser.release(): Release {
    var selected = false
    var version = KEY_EMPTY
    var versionCode = 0L
    var added = 0L
    var size = 0L
    var minSdkVersion = 0
    var targetSdkVersion = 0
    var maxSdkVersion = 0
    var source = KEY_EMPTY
    var release = KEY_EMPTY
    var hash = KEY_EMPTY
    var hashType = KEY_EMPTY
    var signature = KEY_EMPTY
    var obbMain = KEY_EMPTY
    var obbMainHash = KEY_EMPTY
    var obbMainHashType = KEY_EMPTY
    var obbPatch = KEY_EMPTY
    var obbPatchHash = KEY_EMPTY
    var obbPatchHashType = KEY_EMPTY
    var permissions = emptyList<String>()
    var features = emptyList<String>()
    var platforms = emptyList<String>()
    var incompatibilities = emptyList<Release.Incompatibility>()
    forEachKey { key ->
        when {
            key.boolean(SELECTED) -> selected = valueAsBoolean
            key.string(VERSION) -> version = valueAsString
            key.number(VERSIONCODE) -> versionCode = valueAsLong
            key.number(ADDED) -> added = valueAsLong
            key.number(SIZE) -> size = valueAsLong
            key.number(MINSDKVERSION) -> minSdkVersion = valueAsInt
            key.number(TARGETSDKVERSION) -> targetSdkVersion = valueAsInt
            key.number(MAXSDKVERSION) -> maxSdkVersion = valueAsInt
            key.string(SOURCE) -> source = valueAsString
            key.string(RELEASE) -> release = valueAsString
            key.string(HASH) -> hash = valueAsString
            key.string(HASHTYPE) -> hashType = valueAsString
            key.string(SIGNATURE) -> signature = valueAsString
            key.string(OBBMAIN) -> obbMain = valueAsString
            key.string(OBBMAINHASH) -> obbMainHash = valueAsString
            key.string(OBBMAINHASHTYPE) -> obbMainHashType = valueAsString
            key.string(OBBPATCH) -> obbPatch = valueAsString
            key.string(OBBPATCHHASH) -> obbPatchHash = valueAsString
            key.string(OBBPATCHHASHTYPE) -> obbPatchHashType = valueAsString
            key.array(PERMISSIONS) -> permissions = collectNotNullStrings()
            key.array(FEATURES) -> features = collectNotNullStrings()
            key.array(PLATFORMS) -> platforms = collectNotNullStrings()
            key.array(INCOMPATIBILITIES) ->
                incompatibilities =
                    collectNotNull(JsonToken.START_OBJECT) {
                        var type = KEY_EMPTY
                        var feature = KEY_EMPTY
                        forEachKey {
                            when {
                                it.string(INCOMPATIBILITY_TYPE) -> type = valueAsString
                                it.string(INCOMPATIBILITY_FEATURE) -> feature = valueAsString
                                else -> skipChildren()
                            }
                        }
                        when (type) {
                            MIN_SDK -> Release.Incompatibility.MinSdk
                            MAX_SDK -> Release.Incompatibility.MaxSdk
                            PLATFORM -> Release.Incompatibility.Platform
                            INCOMPATIBILITY_FEATURE -> Release.Incompatibility.Feature(feature)
                            else -> null
                        }
                    }

            else -> skipChildren()
        }
    }
    return Release(
        selected = selected,
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
        permissions = permissions,
        features = features,
        platforms = platforms,
        incompatibilities = incompatibilities
    )
}

private const val KEY_EMPTY = ""
private const val SERIALVERSION = "serialVersion"
private const val SELECTED = "selected"
private const val VERSION = "version"
private const val VERSIONCODE = "versionCode"
private const val ADDED = "added"
private const val SIZE = "size"
private const val MINSDKVERSION = "minSdkVersion"
private const val TARGETSDKVERSION = "targetSdkVersion"
private const val MAXSDKVERSION = "maxSdkVersion"
private const val SOURCE = "source"
private const val RELEASE = "release"
private const val HASH = "hash"
private const val HASHTYPE = "hashType"
private const val SIGNATURE = "signature"
private const val OBBMAIN = "obbMain"
private const val OBBMAINHASH = "obbMainHash"
private const val OBBMAINHASHTYPE = "obbMainHashType"
private const val OBBPATCH = "obbPatch"
private const val OBBPATCHHASH = "obbPatchHash"
private const val OBBPATCHHASHTYPE = "obbPatchHashType"
private const val PERMISSIONS = "permissions"
private const val FEATURES = "features"
private const val PLATFORMS = "platforms"
private const val INCOMPATIBILITIES = "incompatibilities"
private const val INCOMPATIBILITY_TYPE = "type"
private const val INCOMPATIBILITY_FEATURE = "feature"
private const val MIN_SDK = "minSdk"
private const val MAX_SDK = "maxSdk"
private const val PLATFORM = "platform"
