package com.looker.droidify.data.local.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import com.looker.droidify.domain.model.Authentication

@Entity(
    tableName = "authentication",
    foreignKeys = [
        ForeignKey(
            entity = RepoEntity::class,
            childColumns = ["repoId"],
            parentColumns = ["id"],
//            Handles in dao
//            onDelete = CASCADE,
        ),
    ],
)
data class AuthenticationEntity(
    val password: String,
    val username: String,
    @PrimaryKey
    val repoId: Int,
)

fun AuthenticationEntity.toAuthentication() = Authentication(
    username = username,
    password = password,
)
