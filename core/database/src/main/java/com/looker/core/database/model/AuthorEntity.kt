package com.looker.core.database.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "author")
data class AuthorEntity(
    val name: String,
    val email: String,
    val web: String,
    @PrimaryKey(autoGenerate = true)
    val id: Long = -1L,
)
