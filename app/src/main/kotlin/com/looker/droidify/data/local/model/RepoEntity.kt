package com.looker.droidify.data.local.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.looker.droidify.data.model.Authentication
import com.looker.droidify.data.model.FilePath
import com.looker.droidify.data.model.Fingerprint
import com.looker.droidify.data.model.Repo
import com.looker.droidify.data.model.VersionInfo
import com.looker.droidify.sync.v2.model.RepoV2

/**
 * `enabled` flag will be kept in datastore and will be updated there only
 * `deleted` is not needed as we will delete all required data when deleting repo or disabling it
 * */
@Entity(tableName = "repository")
data class RepoEntity(
    val address: String,
    val webBaseUrl: String?,
    val fingerprint: Fingerprint,
    val timestamp: Long?,
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
)

fun RepoV2.repoEntity(
    id: Int,
    fingerprint: Fingerprint,
) = RepoEntity(
    id = id,
    address = address,
    timestamp = timestamp,
    fingerprint = fingerprint,
    webBaseUrl = webBaseUrl,
)

fun RepoEntity.toRepo(
    name: String,
    description: String,
    icon: String?,
    mirrors: List<String>,
    enabled: Boolean,
    authentication: Authentication?,
) = Repo(
    icon = FilePath(address, icon),
    name = name,
    description = description,
    fingerprint = fingerprint,
    authentication = authentication,
    enabled = enabled,
    address = address,
    versionInfo = timestamp?.let { VersionInfo(timestamp = it, etag = null) },
    mirrors = mirrors,
    id = id,
)
