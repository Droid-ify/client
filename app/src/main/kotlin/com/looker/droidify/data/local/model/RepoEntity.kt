package com.looker.droidify.data.local.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.looker.droidify.domain.model.Authentication
import com.looker.droidify.domain.model.Fingerprint
import com.looker.droidify.domain.model.Repo
import com.looker.droidify.domain.model.VersionInfo
import com.looker.droidify.sync.v2.model.LocalizedIcon
import com.looker.droidify.sync.v2.model.LocalizedString
import com.looker.droidify.sync.v2.model.RepoV2
import com.looker.droidify.sync.v2.model.localizedValue

@Entity(tableName = "repository")
data class RepoEntity(
    val icon: LocalizedIcon?,
    val address: String,
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
)

fun RepoEntity.toRepo(
    locale: String,
    mirrors: List<String>,
    enabled: Boolean,
    authentication: Authentication?,
) = Repo(
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
