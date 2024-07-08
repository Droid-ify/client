package com.looker.sync.fdroid.v1

import com.looker.core.domain.Parser
import com.looker.core.domain.Syncable
import com.looker.core.domain.model.App
import com.looker.core.domain.model.Repo
import com.looker.network.Downloader
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.fdroid.index.IndexConverter
import org.fdroid.index.v1.IndexV1
import java.io.File
import java.util.Date

class V1Syncable(
    override val downloader: Downloader,
    private val dispatcher: CoroutineDispatcher,
) : Syncable<IndexV1> {
    override val parser: Parser<IndexV1>
        get() = V1Parser(dispatcher)

    override suspend fun sync(repo: Repo): Pair<Repo, List<App>> = withContext(Dispatchers.IO) {
        val jar = downloadIndex(repo)
        val (fingerprint, indexV1) = parser.parse(jar, repo)
        val indexV2 = IndexConverter().toIndexV2(indexV1)
        TODO("Not yet implemented")
    }

    private suspend fun downloadIndex(repo: Repo): File = withContext(Dispatchers.IO) {
        val tempFile = File.createTempFile(repo.name, "-v1")
        downloader.downloadToFile(
            url = repo.indexUrl("index-v1.jar"),
            target = tempFile,
            headers = {
                if (repo.shouldAuthenticate) {
                    authentication(
                        repo.authentication.username,
                        repo.authentication.password
                    )
                }
                if (repo.versionInfo.timestamp > 0L) {
                    ifModifiedSince(Date(repo.versionInfo.timestamp))
                }
            }
        )
        tempFile
    }
}

fun Repo.indexUrl(parameter: String): String =
    buildString {
        append(address.removeSuffix("/"))
        append("/")
        append(parameter.removePrefix("/"))
    }
