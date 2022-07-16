package com.looker.core_database.utils

import androidx.room.TypeConverter
import com.looker.core_database.model.Apk
import com.looker.core_database.model.Author
import com.looker.core_database.model.Donate
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
	fun toAuthor(byteArray: ByteArray): Author = Author.fromJson(jsonBuilder, String(byteArray))

	@TypeConverter
	fun toLocalized(byteArray: ByteArray): Localized =
		Localized.fromJson(jsonBuilder, String(byteArray))

	@TypeConverter
	fun toDonates(byteArray: ByteArray): List<Donate> =
		if (String(byteArray) == "") emptyList()
		else String(byteArray).split("|").map { Donate.fromJson(jsonBuilder, it) }

	@TypeConverter
	fun listToArray(list: List<String>): ByteArray =
		list.joinToString(STRING_DELIMITER).toByteArray()

	@TypeConverter
	fun apkToArray(apks: List<Apk>): ByteArray =
		if (apks.isNotEmpty()) apks.joinToString(STRING_DELIMITER) { it.toJson() }.toByteArray()
		else "".toByteArray()

	@TypeConverter
	fun authorToArray(author: Author): ByteArray = author.toJson().toByteArray()

	@TypeConverter
	fun localizedToArray(localized: Localized): ByteArray = localized.toJson().toByteArray()

	@TypeConverter
	fun donateToArray(donates: List<Donate>): ByteArray =
		if (donates.isNotEmpty()) donates.joinToString("|") { it.toJson() }.toByteArray()
		else "".toByteArray()
}