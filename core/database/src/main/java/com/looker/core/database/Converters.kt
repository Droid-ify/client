package com.looker.core.database

import androidx.room.TypeConverter
import com.looker.core.database.model.LocalizedEntity
import com.looker.core.database.model.PackageEntity
import com.looker.core.database.model.PermissionEntity
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val json = Json {
	ignoreUnknownKeys = true
	encodeDefaults = true
}

internal const val STRING_DELIMITER = "!@#$%^&*"
private val localizedSerializer = MapSerializer(String.serializer(), LocalizedEntity.serializer())
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
	fun localizedToJson(localizedEntity: Map<String, LocalizedEntity>): String =
		json.encodeToString(localizedSerializer, localizedEntity)

	@TypeConverter
	fun jsonToLocalized(jsonObject: String): Map<String, LocalizedEntity> =
		json.decodeFromString(localizedSerializer, jsonObject)

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

class PermissionConverter {

	@TypeConverter
	fun stringToPermission(string: String): List<PermissionEntity> =
		string.split(STRING_DELIMITER).map {
			json.decodeFromString(PermissionEntity.serializer(), it)
		}

	@TypeConverter
	fun permissionToString(permissionEntity: List<PermissionEntity>): String =
		permissionEntity.joinToString(STRING_DELIMITER) {
			json.encodeToString(PermissionEntity.serializer(), it)
		}

}