package com.looker.core.database

import androidx.room.TypeConverter
import com.looker.core.database.model.AntiFeatureEntity
import com.looker.core.database.model.CategoryEntity
import com.looker.core.database.model.LocalizedList
import com.looker.core.database.model.LocalizedString
import com.looker.core.database.model.PackageEntity
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val json = Json {
    ignoreUnknownKeys = true
    encodeDefaults = true
}

internal const val STRING_DELIMITER = "!@#$%^&*"
private val stringListSerializer = ListSerializer(String.serializer())
private val localizedStringSerializer = MapSerializer(String.serializer(), String.serializer())
private val localizedListSerializer = MapSerializer(String.serializer(), stringListSerializer)
private val antiFeatureSerializer =
    MapSerializer(String.serializer(), AntiFeatureEntity.serializer())
private val categorySerializer = MapSerializer(String.serializer(), CategoryEntity.serializer())
private val packageListSerializer = ListSerializer(PackageEntity.serializer())

class CollectionConverter {

    @TypeConverter
    fun listToString(list: List<String>): ByteArray =
        list.joinToString(STRING_DELIMITER).toByteArray()

    @TypeConverter
    fun stringToList(byteArray: ByteArray): List<String> = String(byteArray).split(STRING_DELIMITER)

}

class LocalizedConverter {

    @TypeConverter
    fun localizedStringToJson(localizedEntity: LocalizedString): String =
        json.encodeToString(localizedStringSerializer, localizedEntity)

    @TypeConverter
    fun jsonToLocalizedString(jsonObject: String): LocalizedString =
        json.decodeFromString(localizedStringSerializer, jsonObject)

    @TypeConverter
    fun localizedListToJson(localizedEntity: LocalizedList): String =
        json.encodeToString(localizedListSerializer, localizedEntity)

    @TypeConverter
    fun jsonToLocalizedList(jsonObject: String): LocalizedList =
        json.decodeFromString(localizedListSerializer, jsonObject)

}

class PackageEntityConverter {

    @TypeConverter
    fun entityToString(packageEntity: PackageEntity): String =
        json.encodeToString(packageEntity)

    @TypeConverter
    fun stringToPackage(jsonString: String): PackageEntity =
        json.decodeFromString(jsonString)

    @TypeConverter
    fun entityListToString(packageEntity: List<PackageEntity>): String =
        json.encodeToString(packageListSerializer, packageEntity)

    @TypeConverter
    fun stringToPackageList(jsonString: String): List<PackageEntity> =
        json.decodeFromString(packageListSerializer, jsonString)

}

class RepoConverter {

    @TypeConverter
    fun antiFeaturesToString(map: Map<String, AntiFeatureEntity>): String =
        json.encodeToString(antiFeatureSerializer, map)

    @TypeConverter
    fun stringToAntiFeatures(string: String): Map<String, AntiFeatureEntity> =
        json.decodeFromString(antiFeatureSerializer, string)

    @TypeConverter
    fun categoryToString(map: Map<String, CategoryEntity>): String =
        json.encodeToString(categorySerializer, map)

    @TypeConverter
    fun stringToCategory(string: String): Map<String, CategoryEntity> =
        json.decodeFromString(categorySerializer, string)

}
