package com.looker.droidify.sync

import com.looker.droidify.domain.model.Fingerprint
import com.looker.droidify.domain.model.Repo

/**
 * Expected Architecture: [https://excalidraw.com/#json=JqpGunWTJONjq-ecDNiPg,j9t0X4coeNvIG7B33GTq6A]
 *
 * Current Issue: When downloading entry.jar we need to re-call the synchronizer,
 * which this arch doesn't allow.
 */
interface Syncable<T> {

    val parser: Parser<T>

    suspend fun sync(
        repo: Repo,
    ): Pair<Fingerprint, com.looker.droidify.sync.v2.model.IndexV2?>

}
