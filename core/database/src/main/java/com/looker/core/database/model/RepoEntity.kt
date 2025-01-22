package com.looker.core.database.model

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Relation
import com.looker.core.database.utils.localizedValue
import com.looker.core.domain.model.AntiFeature
import com.looker.core.domain.model.Authentication
import com.looker.core.domain.model.Category
import com.looker.core.domain.model.Fingerprint
import com.looker.core.domain.model.Repo
import com.looker.core.domain.model.VersionInfo

@Entity(tableName = "repos")
data class RepoEntity(
    val address: String,
    val enabled: Boolean = false,
    val username: String = "",
    val password: String = "",
    val name: LocalizedString = mapOf("en-US" to address),
    val description: LocalizedString = emptyMap(),
    val fingerprint: String = "",
    val etag: String = "",
    val timestamp: Long = -1L,
    val mirrors: List<String> = emptyList(),
    @PrimaryKey(autoGenerate = true)
    val id: Long = -1L,
)

data class RepoCategoryAntiFeatures(
    @Embedded
    val repo: RepoEntity,
    @Relation(parentColumn = "id", entityColumn = "repoId")
    val categories: List<CategoryEntity>,
    @Relation(parentColumn = "id", entityColumn = "repoId")
    val antiFeatures: List<AntiFeatureEntity>,
)

fun RepoEntity.update(repo: Repo) = copy(
    username = repo.authentication.username,
    password = repo.authentication.password,
    timestamp = repo.versionInfo.timestamp,
    enabled = repo.enabled,
    mirrors = repo.mirrors,
    fingerprint = repo.fingerprint?.value ?: ""
)

fun RepoEntity.toExternal(
    locale: String,
    categories: List<Category>,
    antiFeatures: List<AntiFeature>,
): Repo = Repo(
    id = id,
    enabled = enabled,
    address = address,
    name = name.localizedValue(locale) ?: "",
    description = description.localizedValue(locale) ?: "",
    fingerprint = if (fingerprint.isBlank()) null else Fingerprint(fingerprint),
    authentication = Authentication(username, password),
    versionInfo = VersionInfo(timestamp = timestamp, etag = etag),
    mirrors = mirrors,
    categories = categories,
    antiFeatures = antiFeatures,
)

fun List<RepoEntity>.toExternal(
    locale: String,
    categories: List<Category>,
    antiFeatures: List<AntiFeature>,
): List<Repo> =
    map { it.toExternal(locale, categories, antiFeatures) }
