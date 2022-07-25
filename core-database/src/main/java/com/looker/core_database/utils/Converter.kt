package com.looker.core_database.utils

import androidx.room.TypeConverter
import com.looker.core_database.model.Apk
import com.looker.core_database.model.Localized
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

object Converter {

	@OptIn(ExperimentalSerializationApi::class)
	private val jsonBuilder = Json {
		ignoreUnknownKeys = true
		encodeDefaults = true
		explicitNulls = false
	}

	@TypeConverter
	fun toStringList(byteArray: ByteArray): List<String> =
		String(byteArray).split(STRING_DELIMITER)

	@TypeConverter
	fun toSingleApk(byteArray: ByteArray): Apk =
		jsonBuilder.decodeFromString(String(byteArray))

	@TypeConverter
	fun toApk(byteArray: ByteArray): List<Apk> =
		if (String(byteArray) == "") emptyList()
		else String(byteArray).split(STRING_DELIMITER).map { Apk.fromJson(jsonBuilder, it) }

	@TypeConverter
	fun toLocalized(byteArray: ByteArray): Map<String, Localized> =
		if (String(byteArray) == "") emptyMap()
		else jsonBuilder.decodeFromString(String(byteArray))

	@TypeConverter
	fun listToArray(list: List<String>): ByteArray =
		list.joinToString(STRING_DELIMITER).toByteArray()

	@TypeConverter
	fun singleApkToArray(apk: Apk): ByteArray =
		apk.toJson().toByteArray()

	@TypeConverter
	fun apkToArray(apks: List<Apk>): ByteArray =
		if (apks.isNotEmpty()) apks.joinToString(STRING_DELIMITER) { it.toJson() }.toByteArray()
		else "".toByteArray()

	@TypeConverter
	fun localizedToArray(localized: Map<String, Localized>): ByteArray =
		if (localized.isNotEmpty()) jsonBuilder.encodeToString(localized).toByteArray()
		else "".toByteArray()
}