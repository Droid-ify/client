package com.looker.droidify.data.model

import com.looker.droidify.network.DataSize

interface DataFile {
    val name: String
    val hash: String
    val size: DataSize
}
