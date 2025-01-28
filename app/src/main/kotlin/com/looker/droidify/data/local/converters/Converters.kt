package com.looker.droidify.data.local.converters

import androidx.room.TypeConverter
import com.looker.droidify.data.local.model.DonateType
import com.looker.droidify.sync.common.JsonParser
import com.looker.droidify.sync.v2.model.FileV2
import com.looker.droidify.sync.v2.model.LocalizedIcon
import com.looker.droidify.sync.v2.model.LocalizedString
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer

private val localizedStringSerializer =
    MapSerializer(String.serializer(), String.serializer())
private val stringListSerializer = ListSerializer(String.serializer())
private val localizedIconSerializer =
    MapSerializer(String.serializer(), FileV2.serializer())
private val mapOfLocalizedStringsSerializer =
    MapSerializer(String.serializer(), localizedStringSerializer)

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
    fun fromLocalizedIcon(value: LocalizedIcon?): String? {
        return value?.let { JsonParser.encodeToString(localizedIconSerializer, it) }
    }

    @TypeConverter
    fun toLocalizedIcon(value: String?): LocalizedIcon? {
        return value?.let { JsonParser.decodeFromString(localizedIconSerializer, it) }
    }

    @TypeConverter
    fun fromLocalizedList(value: Map<String, LocalizedString>): String {
        return JsonParser.encodeToString(mapOfLocalizedStringsSerializer, value)
    }

    @TypeConverter
    fun toLocalizedList(value: String): Map<String, LocalizedString> {
        return JsonParser.decodeFromString(mapOfLocalizedStringsSerializer, value)
    }

    @TypeConverter
    fun fromStringList(value: List<String>): String {
        return JsonParser.encodeToString(stringListSerializer, value)
    }

    @TypeConverter
    fun toStringList(value: String): List<String> {
        return JsonParser.decodeFromString(stringListSerializer, value)
    }

    @TypeConverter
    fun fromDonateType(value: DonateType): String {
        return JsonParser.encodeToString(DonateType.serializer(), value)
    }

    @TypeConverter
    fun toDonateType(value: String): DonateType {
        return JsonParser.decodeFromString(DonateType.serializer(), value)
    }
}
