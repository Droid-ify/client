package com.looker.droidify.sync

import com.looker.droidify.domain.model.Fingerprint
import com.looker.droidify.domain.model.Repo
import java.io.File

interface Parser<out T> {

    suspend fun parse(
        file: File,
        repo: Repo,
    ): Pair<Fingerprint, T>

}
