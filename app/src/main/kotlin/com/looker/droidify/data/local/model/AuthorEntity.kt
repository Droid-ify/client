package com.looker.droidify.data.local.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.looker.droidify.domain.model.Author
import com.looker.droidify.sync.v2.model.MetadataV2

@Entity(tableName = "author")
data class AuthorEntity(
    val email: String?,
    val name: String?,
    val phone: String?,
    val website: String?,
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
)

fun MetadataV2.authorEntity() = AuthorEntity(
    email = authorEmail,
    name = authorName,
    phone = authorPhone,
    website = authorWebSite,
)

fun AuthorEntity.toAuthor() = Author(
    email = email,
    name = name,
    phone = phone,
    web = website,
    id = id,
)
