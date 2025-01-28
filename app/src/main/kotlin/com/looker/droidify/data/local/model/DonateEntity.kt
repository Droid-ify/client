package com.looker.droidify.data.local.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.ForeignKey.Companion.CASCADE
import androidx.room.Index
import androidx.room.PrimaryKey
import com.looker.droidify.domain.model.Donation
import com.looker.droidify.sync.v2.model.MetadataV2
import kotlinx.serialization.Serializable

@Entity(
    tableName = "donate",
    indices = [Index("appId")],
    foreignKeys = [
        ForeignKey(
            entity = AppEntity::class,
            childColumns = ["appId"],
            parentColumns = ["id"],
            onDelete = CASCADE,
        ),
    ],
)
data class DonateEntity(
    val type: DonateType,
    val appId: Int,
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
)

@Serializable
sealed interface DonateType {
    data class Bitcoin(val address: String) : DonateType
    data class Litecoin(val address: String) : DonateType
    data class Liberapay(val id: String) : DonateType
    data class OpenCollective(val id: String) : DonateType
    data class Flattr(val id: String) : DonateType
    data class Regular(val url: List<String>) : DonateType
}

fun MetadataV2.donateEntity(appId: Int): List<DonateEntity>? {
    return buildList {
        if (bitcoin != null) {
            add(DonateEntity(DonateType.Bitcoin(bitcoin), appId))
        }
        if (litecoin != null) {
            add(DonateEntity(DonateType.Litecoin(litecoin), appId))
        }
        if (liberapay != null) {
            add(DonateEntity(DonateType.Liberapay(liberapay), appId))
        }
        if (openCollective != null) {
            add(DonateEntity(DonateType.OpenCollective(openCollective), appId))
        }
        if (flattrID != null) {
            add(DonateEntity(DonateType.Flattr(flattrID), appId))
        }
        if (!donate.isNullOrEmpty()) {
            add(DonateEntity(DonateType.Regular(donate), appId))
        }
    }.ifEmpty { null }
}

fun List<DonateEntity>.toDonation(): Donation {
    var bitcoinAddress: String? = null
    var litecoinAddress: String? = null
    var liberapayId: String? = null
    var openCollectiveId: String? = null
    var flattrId: String? = null
    var regular: List<String>? = null
    for (entity in this) {
        val type = entity.type
        when (type) {
            is DonateType.Bitcoin -> bitcoinAddress = type.address
            is DonateType.Flattr -> flattrId = type.id
            is DonateType.Liberapay -> liberapayId = type.id
            is DonateType.Litecoin -> litecoinAddress = type.address
            is DonateType.OpenCollective -> openCollectiveId = type.id
            is DonateType.Regular -> regular = type.url
        }

    }
    return Donation(
        bitcoinAddress = bitcoinAddress,
        litecoinAddress = litecoinAddress,
        liberapayId = liberapayId,
        openCollectiveId = openCollectiveId,
        flattrId = flattrId,
        regularUrl = regular,
    )
}
