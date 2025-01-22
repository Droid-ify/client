package com.looker.core.database.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.ForeignKey.Companion.CASCADE
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "donations",
    foreignKeys = [
        ForeignKey(
            entity = AppEntity::class,
            parentColumns = ["id"],
            childColumns = ["appId"],
            onDelete = CASCADE,
        ),
        ForeignKey(
            entity = AuthorEntity::class,
            parentColumns = ["id"],
            childColumns = ["authorId"],
            onDelete = CASCADE,
        )
    ],
    indices = [Index("appId", "authorId")],
)
data class DonationEntity(
    val regularUrl: String,
    val liberapay: String,
    val openCollective: String,
    val bitcoin: String,
    val litecoin: String,
    val flattrID: String,
    val appId: Long,
    val authorId: Long,
    @PrimaryKey(autoGenerate = true)
    val id: Long = -1L,
)
