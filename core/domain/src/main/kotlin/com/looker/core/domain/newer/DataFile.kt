package com.looker.core.domain.newer

interface DataFile {
    val name: String
    val hash: String
    val size: Long
}
