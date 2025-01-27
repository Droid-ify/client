package com.looker.droidify.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import com.looker.droidify.data.local.model.AntiFeatureEntity
import com.looker.droidify.data.local.model.MirrorEntity
import com.looker.droidify.data.local.model.RepoEntity
import com.looker.droidify.data.local.model.antiFeatureEntity
import com.looker.droidify.data.local.model.mirrorEntity
import com.looker.droidify.data.local.model.repoEntity
import com.looker.droidify.domain.model.Fingerprint
import com.looker.droidify.sync.v2.model.RepoV2
import com.looker.droidify.utility.common.log
import kotlinx.coroutines.flow.Flow

@Dao
interface RepoDao {

    @Query("SELECT * FROM repository")
    fun stream(): Flow<List<RepoEntity>>

    @Upsert
    suspend fun upsert(repoEntity: RepoEntity): Long

    @Upsert
    suspend fun upsertAntiFeatures(antiFeatures: List<AntiFeatureEntity>)

    @Upsert
    suspend fun upsertMirror(mirrors: List<MirrorEntity>)

    @Transaction
    suspend fun upsertRepo(
        fingerprint: Fingerprint,
        repo: RepoV2,
        username: String? = null,
        password: String? = null,
        id: Int = 0,
    ): Int {
        val repoId = id
        upsert(
            repo.repoEntity(
                id = id,
                fingerprint = fingerprint,
                username = username,
                password = password,
            ),
        ).toInt()
        log("Repo ID: $repoId")
        val antiFeatures = repo.antiFeatures.map { (tag, feature) ->
            feature.antiFeatureEntity(tag)
        }
        upsertAntiFeatures(antiFeatures)
        // TODO: Add category
        val mirrors = repo.mirrors.map { it.mirrorEntity(repoId) }
        upsertMirror(mirrors)
        return repoId
    }

    @Query("DELETE FROM repository WHERE id = :id")
    suspend fun delete(id: Int)

}
