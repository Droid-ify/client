package com.looker.core.database.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.looker.core.database.utils.localizedValue
import com.looker.core.domain.model.AntiFeature
import com.looker.core.domain.model.Authentication
import com.looker.core.domain.model.Category
import com.looker.core.domain.model.Fingerprint
import com.looker.core.domain.model.Repo
import com.looker.core.domain.model.VersionInfo
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
    timestamp = repo.versionInfo.timestamp,
    enabled = repo.enabled,
    mirrors = repo.mirrors,
    fingerprint = repo.fingerprint?.value ?: ""
)

fun RepoEntity.toExternal(locale: String): Repo = Repo(
    id = id!!,
    enabled = enabled,
    address = address,
    name = name.localizedValue(locale) ?: "",
    description = description.localizedValue(locale) ?: "",
    fingerprint = if (fingerprint.isBlank()) null else Fingerprint(fingerprint),
    authentication = Authentication(username, password),
    versionInfo = VersionInfo(timestamp = timestamp, etag = etag),
    mirrors = mirrors,
    categories = categories.values.toCategoryList(locale),
    antiFeatures = antiFeatures.values.toAntiFeatureList(locale)
)

fun List<RepoEntity>.toExternal(locale: String): List<Repo> =
    map { it.toExternal(locale) }

@Serializable
data class CategoryEntity(
    val icon: LocalizedString,
    val name: LocalizedString,
    val description: LocalizedString
)

private fun CategoryEntity.toCategory(locale: String) =
    Category(
        name = name.localizedValue(locale) ?: "",
        icon = icon.localizedValue(locale) ?: "",
        description = description.localizedValue(locale) ?: ""
    )

fun Collection<CategoryEntity>.toCategoryList(locale: String): List<Category> =
    map { it.toCategory(locale) }

@Serializable
data class AntiFeatureEntity(
    val icon: LocalizedString,
    val name: LocalizedString,
    val description: LocalizedString
)

private fun AntiFeatureEntity.toAntiFeature(locale: String) =
    AntiFeature(
        name = name.localizedValue(locale) ?: "",
        icon = icon.localizedValue(locale) ?: "",
        description = description.localizedValue(locale) ?: ""
    )

fun Collection<AntiFeatureEntity>.toAntiFeatureList(locale: String): List<AntiFeature> =
    map { it.toAntiFeature(locale) }
