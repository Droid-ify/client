package com.looker.core.domain

import com.looker.core.domain.model.App
import com.looker.core.domain.model.Repo
import com.looker.network.Downloader
import java.io.File

/**
 * Expected Architecture: [https://excalidraw.com/#json=JqpGunWTJONjq-ecDNiPg,j9t0X4coeNvIG7B33GTq6A]
 *
 * Current Issue: When downloading entry.jar we need to re-call the synchronizer,
 * which this arch doesn't allow.
 */
interface Syncable<T> {

    val downloader: Downloader

    val parser: Parser<T>

    suspend fun sync(): Pair<Repo, List<App>>

}

interface Parser<out T> {

    suspend fun parse(downloadedFile: File): T

}
