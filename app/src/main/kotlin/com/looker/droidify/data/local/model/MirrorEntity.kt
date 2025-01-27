package com.looker.droidify.data.local.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.ForeignKey.Companion.CASCADE
import androidx.room.Index
import androidx.room.PrimaryKey
import com.looker.droidify.sync.v2.model.MirrorV2

@Entity(
    tableName = "mirror",
    indices = [Index("repoId")],
    foreignKeys = [
        ForeignKey(
            entity = RepoEntity::class,
            childColumns = ["repoId"],
            parentColumns = ["id"],
            onDelete = CASCADE,
        ),
    ]
)
data class MirrorEntity(
    val url: String,
    val countryCode: String?,
    val isPrimary: Boolean,
    val repoId: Int,
    @PrimaryKey(autoGenerate = true)
    val id: Int = -1,
)

fun MirrorV2.mirrorEntity(repoId: Int) = MirrorEntity(
    url = url,
    countryCode = countryCode,
    isPrimary = isPrimary == true,
    repoId = repoId,
)
