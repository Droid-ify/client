package com.looker.droidify.data.local.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.looker.droidify.sync.v2.model.LocalizedIcon
import com.looker.droidify.sync.v2.model.LocalizedString
import com.looker.droidify.sync.v2.model.RepoV2

@Entity(tableName = "repository")
data class RepoEntity(
    val icon: LocalizedIcon?,
    val address: String,
    val name: LocalizedString,
    val description: LocalizedString,
    val timestamp: Long,
    @PrimaryKey(autoGenerate = true)
    val id: Int = -1,
)

fun RepoV2.repoEntity() = RepoEntity(
    icon = icon,
    address = address,
    name = name,
    description = description,
    timestamp = timestamp,
)
