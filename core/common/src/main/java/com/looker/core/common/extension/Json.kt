package com.looker.core.common.extension

import com.fasterxml.jackson.core.JsonFactory
import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.core.JsonParseException
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.core.JsonToken

object Json {
    val factory = JsonFactory()
}

interface KeyToken {
    val key: String
    val token: JsonToken

    fun number(key: String): Boolean = this.key == key && this.token.isNumeric
    fun string(key: String): Boolean = this.key == key && this.token == JsonToken.VALUE_STRING
    fun boolean(key: String): Boolean = this.key == key && this.token.isBoolean
    fun dictionary(key: String): Boolean = this.key == key && this.token == JsonToken.START_OBJECT
    fun array(key: String): Boolean = this.key == key && this.token == JsonToken.START_ARRAY
}

fun JsonParser.illegal(): Nothing {
    throw JsonParseException(this, "Illegal state")
}

fun JsonParser.forEachKey(callback: JsonParser.(KeyToken) -> Unit) {
    var passKey = ""
    var passToken = JsonToken.NOT_AVAILABLE
    val keyToken = object : KeyToken {
        override val key: String
            get() = passKey
        override val token: JsonToken
            get() = passToken
    }
    while (true) {
        val token = nextToken()
        if (token == JsonToken.FIELD_NAME) {
            passKey = currentName()
            passToken = nextToken()
            callback(keyToken)
        } else if (token == JsonToken.END_OBJECT) {
            break
        } else {
            illegal()
        }
    }
}

fun JsonParser.forEach(requiredToken: JsonToken, callback: JsonParser.() -> Unit) {
    while (true) {
        val token = nextToken()
        if (token == JsonToken.END_ARRAY) {
            break
        } else if (token == requiredToken) {
            callback()
        } else if (token.isStructStart) {
            skipChildren()
        }
    }
}

fun <T> JsonParser.collectNotNull(
    requiredToken: JsonToken,
    callback: JsonParser.() -> T?
): List<T> {
    val list = mutableListOf<T>()
    forEach(requiredToken) {
        val result = callback()
        if (result != null) {
            list += result
        }
    }
    return list
}

fun JsonParser.collectNotNullStrings(): List<String> {
    return collectNotNull(JsonToken.VALUE_STRING) { valueAsString }
}

fun JsonParser.collectDistinctNotEmptyStrings(): List<String> {
    return collectNotNullStrings().asSequence().filter { it.isNotEmpty() }.distinct().toList()
}

fun <T> JsonParser.parseDictionary(callback: JsonParser.() -> T): T {
    if (nextToken() == JsonToken.START_OBJECT) {
        val result = callback()
        if (nextToken() != null) {
            illegal()
        }
        return result
    } else {
        illegal()
    }
}

inline fun JsonGenerator.writeDictionary(callback: JsonGenerator.() -> Unit) {
    writeStartObject()
    callback()
    writeEndObject()
}

inline fun JsonGenerator.writeArray(fieldName: String, callback: JsonGenerator.() -> Unit) {
    writeArrayFieldStart(fieldName)
    callback()
    writeEndArray()
}
