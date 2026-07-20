package com.looker.droidify.data

import com.looker.droidify.data.encryption.EncryptionStorage
import com.looker.droidify.data.local.sql.DroidifyDb
import com.looker.droidify.data.model.Fingerprint
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

class RepoRepository(
    private val db: DroidifyDb,
    private val encryptionStorage: EncryptionStorage,
    private val dispatcher: CoroutineDispatcher,
) {

    suspend fun insert(
        address: String,
        fingerprint: Fingerprint? = null,
        username: String? = null,
        password: String? = null,
        enabled: Boolean = true,
    ): Long = withContext(dispatcher) {
        val key = if (username != null && password != null) {
            encryptionStorage.key.first()
        } else {
            null
        }
        db.transactionWithResult {
            db.repositoryQueries.insertRepo(
                address = address.removeSuffix("/"),
                webBaseUrl = null,
                fingerprint = fingerprint,
                etag = null,
                timestamp = null,
                enabled = enabled,
            )
            val repoId = db.repositoryQueries.lastInsertRowId().executeAsOne()
            if (key != null && username != null && password != null) {
                val (encrypted, iv) = key.encrypt(password)
                db.repositoryQueries.insertAuthentication(
                    password = encrypted,
                    username = username,
                    initializationVector = iv,
                    repoId = repoId,
                )
            }
            repoId
        }
    }

    suspend fun setEnabled(repoId: Long, enabled: Boolean) = withContext(dispatcher) {
        db.repositoryQueries.updateRepoEnabled(enabled, repoId)
    }

    suspend fun updateVersionInfo(
        repoId: Long,
        fingerprint: Fingerprint?,
        timestamp: Long?,
        etag: String?,
    ) = withContext(dispatcher) {
        db.repositoryQueries.updateRepoVersionInfo(
            fingerprint = fingerprint,
            etag = etag,
            timestamp = timestamp,
            id = repoId,
        )
    }

    suspend fun delete(repoId: Long) = withContext(dispatcher) {
        db.repositoryQueries.deleteRepo(repoId)
    }
}
