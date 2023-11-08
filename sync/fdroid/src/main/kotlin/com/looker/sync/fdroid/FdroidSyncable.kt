package com.looker.sync.fdroid

import com.looker.core.domain.Syncable
import com.looker.core.domain.newer.App
import com.looker.core.domain.newer.Repo

class FdroidSyncable(override val repo: Repo) : Syncable {

    override suspend fun getApps(): List<App> = emptyList()

    override suspend fun getUpdatedRepo(): Repo = repo

}
