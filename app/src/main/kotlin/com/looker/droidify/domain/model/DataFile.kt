package com.looker.droidify.domain.model

interface DataFile {
    val name: String
    val hash: String
    val size: Long
}
