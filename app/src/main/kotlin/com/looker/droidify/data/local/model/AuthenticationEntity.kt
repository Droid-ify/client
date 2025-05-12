package com.looker.droidify.data.local.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import com.looker.droidify.data.encryption.Encrypted

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
    val password: Encrypted,
    val username: String,
    val initializationVector: String,
    @PrimaryKey
    val repoId: Int,
)
