package com.looker.droidify.sync

import com.looker.droidify.domain.model.Fingerprint
import com.looker.droidify.domain.model.Repo
import com.looker.droidify.sync.v2.model.IndexV2

interface Syncable<T> {

    val parser: Parser<T>

    suspend fun sync(
        repo: Repo,
    ): Pair<Fingerprint, IndexV2?>

}
