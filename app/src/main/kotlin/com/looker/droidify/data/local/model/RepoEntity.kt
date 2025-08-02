package com.looker.droidify.data.local.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.looker.droidify.data.model.Authentication
import com.looker.droidify.data.model.FilePath
import com.looker.droidify.data.model.Fingerprint
import com.looker.droidify.data.model.Repo
import com.looker.droidify.data.model.VersionInfo
import com.looker.droidify.sync.v2.model.LocalizedIcon
import com.looker.droidify.sync.v2.model.LocalizedString
import com.looker.droidify.sync.v2.model.RepoV2
import com.looker.droidify.sync.v2.model.localizedValue

/**
 * `enabled` flag will be kept in datastore and will be updated there only
 * `deleted` is not needed as we will delete all required data when deleting repo or disabling it
 * */
@Entity(tableName = "repository")
data class RepoEntity(
    val icon: LocalizedIcon?,
    val address: String,
    val webBaseUrl: String?,
    val name: LocalizedString,
    val description: LocalizedString,
    val fingerprint: Fingerprint,
    val timestamp: Long,
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
)

fun RepoV2.repoEntity(
    id: Int,
    fingerprint: Fingerprint,
) = RepoEntity(
    id = id,
    icon = icon,
    address = address,
    name = name,
    description = description,
    timestamp = timestamp,
    fingerprint = fingerprint,
    webBaseUrl = webBaseUrl,
)

fun RepoEntity.toRepo(
    locale: String,
    mirrors: List<String>,
    enabled: Boolean,
    authentication: Authentication?,
) = Repo(
    icon = FilePath(address, icon?.localizedValue(locale)?.name),
    name = name.localizedValue(locale) ?: "Unknown",
    description = description.localizedValue(locale) ?: "Unknown",
    fingerprint = fingerprint,
    authentication = authentication,
    enabled = enabled,
    address = address,
    versionInfo = VersionInfo(timestamp = timestamp, etag = null),
    mirrors = mirrors,
    id = id,
)
