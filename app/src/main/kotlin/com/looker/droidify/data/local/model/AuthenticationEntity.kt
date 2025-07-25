package com.looker.droidify.data.local.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.ForeignKey.Companion.CASCADE
import androidx.room.PrimaryKey
import com.looker.droidify.data.encryption.Encrypted
import com.looker.droidify.data.encryption.Key
import com.looker.droidify.domain.model.Authentication

@Entity(
    tableName = "authentication",
    foreignKeys = [
        ForeignKey(
            entity = RepoEntity::class,
            childColumns = ["repoId"],
            parentColumns = ["id"],
            onDelete = CASCADE,
        ),
    ],
)
class AuthenticationEntity(
    val password: Encrypted,
    val username: String,
    val initializationVector: ByteArray,
    @PrimaryKey
    val repoId: Int,
)

fun AuthenticationEntity.toAuthentication(key: Key) = Authentication(
    password = password.decrypt(key, initializationVector),
    username = username,
)
