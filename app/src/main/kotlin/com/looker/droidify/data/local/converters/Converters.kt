package com.looker.droidify.data.local.converters

import androidx.room.TypeConverter
import com.looker.droidify.sync.JsonParser
import com.looker.droidify.sync.v2.model.LocalizedString
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer

private val localizedStringSerializer = MapSerializer(String.serializer(), String.serializer())
private val stringListSerializer = ListSerializer(String.serializer())

object Converters {

    @TypeConverter
    fun fromLocalizedString(value: LocalizedString): String {
        return JsonParser.encodeToString(localizedStringSerializer, value)
    }

    @TypeConverter
    fun toLocalizedString(value: String): LocalizedString {
        return JsonParser.decodeFromString(localizedStringSerializer, value)
    }

    @TypeConverter
    fun fromStringList(value: List<String>): String {
        return JsonParser.encodeToString(stringListSerializer, value)
    }

    @TypeConverter
    fun toStringList(value: String): List<String> {
        return JsonParser.decodeFromString(stringListSerializer, value)
    }
}
