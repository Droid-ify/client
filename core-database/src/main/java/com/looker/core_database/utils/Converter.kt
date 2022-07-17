package com.looker.core_database.utils

import androidx.room.TypeConverter
import com.looker.core_database.model.Apk
import com.looker.core_database.model.Localized
import kotlinx.serialization.json.Json

object Converter {

	private val jsonBuilder = Json { ignoreUnknownKeys = true }

	@TypeConverter
	fun toStringList(byteArray: ByteArray): List<String> =
		String(byteArray).split(STRING_DELIMITER)

	@TypeConverter
	fun toApk(byteArray: ByteArray): List<Apk> =
		if (String(byteArray) == "") emptyList()
		else String(byteArray).split(STRING_DELIMITER).map { Apk.fromJson(jsonBuilder, it) }

	@TypeConverter
	fun toLocalized(byteArray: ByteArray): List<Localized> =
		if (String(byteArray) == "") emptyList()
		else String(byteArray).split(STRING_DELIMITER)
			.map { Localized.fromJson(jsonBuilder, String(byteArray)) }

	@TypeConverter
	fun listToArray(list: List<String>): ByteArray =
		list.joinToString(STRING_DELIMITER).toByteArray()

	@TypeConverter
	fun apkToArray(apks: List<Apk>): ByteArray =
		if (apks.isNotEmpty()) apks.joinToString(STRING_DELIMITER) { it.toJson() }.toByteArray()
		else "".toByteArray()

	@TypeConverter
	fun localizedToArray(localized: List<Localized>): ByteArray =
		if (localized.isNotEmpty())
			localized.joinToString(STRING_DELIMITER) { it.toJson() }.toByteArray()
		else "".toByteArray()
}