package com.looker.core.domain

import com.looker.core.domain.newer.App
import com.looker.core.domain.newer.Repo

/**
 * Expected Architecture: https://excalidraw.com/#json=6JIt5NYdqesMGza45l5eE,uyCOnQUx2ET8sVsmtJivjg
 *
 * Current Issue: When downloading entry.jar we need to re-call the synchronizer,
 * which this arch doesn't allow.
 */
interface Syncable {

    val synchronizer: Synchronizer

    val parser: Parser

}

interface Parser {

    suspend fun parsedRepo(): Repo

    suspend fun parsedApps(): List<App>

}

interface Synchronizer {
    suspend fun downloadData()
}
