package com.looker.droidify.sync.v2.model

data class AuthorEntity(
    val email: String,
    val name: String,
    val phone: String,
    val website: String,
    val id: Int = 0,
)
