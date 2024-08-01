package com.looker.sync.fdroid

import com.looker.core.domain.model.Fingerprint
import com.looker.core.domain.model.Repo
import java.io.File

interface Parser<out T> {

    suspend fun parse(
        file: File,
        repo: Repo,
    ): Pair<Fingerprint, T>

}
