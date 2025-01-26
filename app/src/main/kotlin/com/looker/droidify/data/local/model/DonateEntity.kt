package com.looker.droidify.data.local.model

import com.looker.droidify.domain.model.Donation
import com.looker.droidify.sync.v2.model.MetadataV2

data class DonateEntity(
    val bitcoinAddress: String?,
    val litecoinAddress: String?,
    val liberapayId: String?,
    val openCollectiveId: String?,
    val flattrId: String?,
    val customUrl: List<String>?,
    val id: Int = -1,
)

fun MetadataV2.donateEntity() = DonateEntity(
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
