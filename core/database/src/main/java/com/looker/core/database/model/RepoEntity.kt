package com.looker.core.database.model

import androidx.core.os.LocaleListCompat
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.looker.core.database.utils.localizedValue
import com.looker.core.model.newer.*
import kotlinx.serialization.Serializable

@Entity(tableName = "repos")
data class RepoEntity(
	@PrimaryKey(autoGenerate = true)
	val id: Long? = null,
	val enabled: Boolean,
	val fingerprint: String,
	val etag: String,
	val username: String,
	val password: String,
	val address: String,
	val mirrors: List<String>,
	val name: LocalizedString,
	val description: LocalizedString,
	val antiFeatures: Map<String, AntiFeatureEntity>,
	val categories: Map<String, CategoryEntity>,
	val timestamp: Long
)

fun RepoEntity.update(repo: Repo) = copy(
	username = repo.authentication.username,
	password = repo.authentication.password,
	etag = repo.versionInfo.etag,
	timestamp = repo.versionInfo.timestamp,
	enabled = repo.enabled,
	mirrors = repo.mirrors,
	fingerprint = repo.fingerprint
)

fun RepoEntity.toExternal(locale: LocaleListCompat): Repo = Repo(
	id = id!!,
	enabled = enabled,
	address = address,
	name = name.localizedValue(locale) ?: "",
	description = description.localizedValue(locale) ?: "",
	fingerprint = fingerprint,
	authentication = Authentication(username, password),
	versionInfo = VersionInfo(etag = etag, timestamp = timestamp),
	mirrors = mirrors,
	categories = categories.values.toExternal(locale),
	antiFeatures = antiFeatures.values.toExternal(locale)
)

fun List<RepoEntity>.toExternal(locale: LocaleListCompat): List<Repo> =
	map { it.toExternal(locale) }

@Serializable
data class CategoryEntity(
	val icon: LocalizedString,
	val name: LocalizedString,
	val description: LocalizedString
)

private fun CategoryEntity.toExternal(locale: LocaleListCompat) =
	Category(
		name = name.localizedValue(locale) ?: "",
		icon = icon.localizedValue(locale) ?: "",
		description = description.localizedValue(locale) ?: ""
	)

fun Collection<CategoryEntity>.toExternal(locale: LocaleListCompat): List<Category> =
	map { it.toExternal(locale) }

@Serializable
data class AntiFeatureEntity(
	val icon: LocalizedString,
	val name: LocalizedString,
	val description: LocalizedString
)

private fun AntiFeatureEntity.toAntiFeature(locale: LocaleListCompat) =
	AntiFeature(
		name = name.localizedValue(locale) ?: "",
		icon = icon.localizedValue(locale) ?: "",
		description = description.localizedValue(locale) ?: ""
	)

fun Collection<AntiFeatureEntity>.toExternal(locale: LocaleListCompat): List<AntiFeature> =
	map { it.toAntiFeature(locale) }