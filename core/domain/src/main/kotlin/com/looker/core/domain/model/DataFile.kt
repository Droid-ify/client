package com.looker.core.domain.model

interface DataFile {
    val name: String
    val hash: String
    val size: Long
}
