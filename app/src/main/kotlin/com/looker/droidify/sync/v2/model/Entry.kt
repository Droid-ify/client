package com.looker.droidify.sync.v2.model

/*
* Entry and EntryFile are licensed under the GPL 3.0 to FDroid Organization.
* */

import kotlinx.serialization.Serializable

@Serializable
data class Entry(
    val timestamp: Long,
    val version: Long,
    val index: EntryFile,
    val diffs: Map<Long, EntryFile>
) {

    fun getDiff(timestamp: Long?): EntryFile? {
        return if (this.timestamp == timestamp) null
        else diffs[timestamp] ?: index
    }

}

@Serializable
data class EntryFile(
    val name: String,
    val sha256: String,
    val size: Long,
    val numPackages: Long,
)
