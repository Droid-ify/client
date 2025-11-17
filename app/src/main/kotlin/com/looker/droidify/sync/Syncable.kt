package com.looker.droidify.sync

import com.looker.droidify.data.model.Repo

interface Syncable<T> {

    val parser: Parser<T>

    suspend fun sync(repo: Repo, block: (SyncState) -> Unit)
}
