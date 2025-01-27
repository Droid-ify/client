package com.looker.droidify.data.local.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.ForeignKey.Companion.CASCADE
import androidx.room.Index
import androidx.room.PrimaryKey
import com.looker.droidify.domain.model.Donation
import com.looker.droidify.sync.v2.model.MetadataV2

@Entity(
    tableName = "donate",
    indices = [Index("appId")],
    foreignKeys = [
        ForeignKey(
            entity = AppEntity::class,
            childColumns = ["appId"],
            parentColumns = ["id"],
            onDelete = CASCADE,
        )
    ]
)
data class DonateEntity(
    val bitcoinAddress: String?,
    val litecoinAddress: String?,
    val liberapayId: String?,
    val openCollectiveId: String?,
    val flattrId: String?,
    val customUrl: List<String>?,
    val appId: Int,
    @PrimaryKey(autoGenerate = true)
    val id: Int = -1,
)

fun MetadataV2.donateEntity(appId: Int) = DonateEntity(
    appId = appId,
    bitcoinAddress = bitcoin,
    litecoinAddress = litecoin,
    liberapayId = liberapay,
    openCollectiveId = openCollective,
    flattrId = flattrID,
    customUrl = donate,
)

fun DonateEntity.toDonation() = Donation(
    bitcoinAddress = bitcoinAddress,
    litecoinAddress = litecoinAddress,
    liberapayId = liberapayId,
    openCollectiveId = openCollectiveId,
    flattrId = flattrId,
    regularUrl = customUrl,
)
