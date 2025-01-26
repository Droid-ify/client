package com.looker.droidify.data.local.model

import com.looker.droidify.domain.model.Author
import com.looker.droidify.sync.v2.model.MetadataV2

data class AuthorEntity(
    val email: String?,
    val name: String?,
    val phone: String?,
    val website: String?,
    val id: Int = -1,
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
