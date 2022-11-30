package com.looker.core.database

import androidx.room.TypeConverter
import com.looker.core.database.model.LocalizedEntity
import com.looker.core.database.model.PackageEntity
import com.looker.core.database.model.PermissionEntity
import kotlinx.serialization.json.Json

private val json = Json {
	ignoreUnknownKeys = true
	encodeDefaults = true
}

internal const val STRING_DELIMITER = "!@#$%^&*"

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
		localizedEntity.map {
			it.key + STRING_DELIMITER + json.encodeToString(LocalizedEntity.serializer(), it.value)
		}.joinToString(STRING_DELIMITER)

	@TypeConverter
	fun jsonToLocalized(jsonObject: String): Map<String, LocalizedEntity> =
		jsonObject.split(STRING_DELIMITER).associate { mapString ->
			val mapValues = mapString.split(STRING_DELIMITER)
			mapValues[0] to json.decodeFromString(LocalizedEntity.serializer(), mapValues[1])
		}

}

class PackageEntityConverter {

	@TypeConverter
	fun entityToString(packageEntity: PackageEntity): String =
		json.encodeToString(PackageEntity.serializer(), packageEntity)


	@TypeConverter
	fun stringToPackage(jsonString: String): PackageEntity =
		json.decodeFromString(PackageEntity.serializer(), jsonString)

	@TypeConverter
	fun entityListToString(packageEntity: List<PackageEntity>): String =
		packageEntity.joinToString(STRING_DELIMITER) {
			json.encodeToString(PackageEntity.serializer(), it)
		}

	@TypeConverter
	fun stringToPackageList(jsonString: String): List<PackageEntity> =
		jsonString.split(STRING_DELIMITER).map {
			json.decodeFromString(PackageEntity.serializer(), it)
		}

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