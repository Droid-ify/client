package com.looker.droidify.sync.v2.model

data class DonateEntity(
    val bitcoinAddress: String,
    val litecoinAddress: String,
    val liberapayId: String,
    val openCollectiveId: String,
    val flattrId: String,
    val customUrl: List<String>,
    val id: Int = -1,
)
