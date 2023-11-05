package com.looker.core.domain

import com.looker.core.domain.newer.App
import com.looker.core.domain.newer.Repo

interface Syncable {

    val repo: Repo

    suspend fun getApps(): List<App>

    suspend fun getUpdatedRepo(): Repo

}
