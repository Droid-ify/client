package com.looker.droidify.sync

import com.looker.droidify.data.model.Fingerprint
import com.looker.droidify.sync.v2.model.IndexV2

sealed interface SyncState {
    val repoId: Int

    sealed interface IndexDownload : SyncState {
        data class Progress(override val repoId: Int, val progress: Int) : IndexDownload
        data class Success(override val repoId: Int) : IndexDownload
        data class Failure(override val repoId: Int, val error: Throwable) : IndexDownload
    }

    sealed interface JarParsing : SyncState {
        data class Success(override val repoId: Int, val fingerprint: Fingerprint) : JarParsing
        data class Failure(override val repoId: Int, val error: Throwable) : JarParsing
    }

    sealed interface JsonParsing : SyncState {
        data class Success(
            override val repoId: Int,
            val fingerprint: Fingerprint,
            val index: IndexV2?,
        ) : JsonParsing
        data class Failure(override val repoId: Int, val error: Throwable) : JsonParsing
    }
}