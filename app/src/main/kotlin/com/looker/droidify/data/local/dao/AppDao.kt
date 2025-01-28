package com.looker.droidify.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import com.looker.droidify.data.local.model.AppEntity
import com.looker.droidify.data.local.model.AuthorEntity
import com.looker.droidify.data.local.model.DonateEntity
import com.looker.droidify.data.local.model.GraphicEntity
import com.looker.droidify.data.local.model.LinksEntity
import com.looker.droidify.data.local.model.ScreenshotEntity
import com.looker.droidify.data.local.model.appEntity
import com.looker.droidify.data.local.model.authorEntity
import com.looker.droidify.data.local.model.donateEntity
import com.looker.droidify.data.local.model.linkEntity
import com.looker.droidify.data.local.model.localizedGraphics
import com.looker.droidify.data.local.model.localizedScreenshots
import com.looker.droidify.sync.v2.model.MetadataV2
import kotlinx.coroutines.flow.Flow

@Dao
interface AppDao {

    @Query("SELECT * FROM app")
    fun stream(): Flow<List<AppEntity>>

    @Upsert
    suspend fun upsert(appEntity: AppEntity): Long

    @Query("SELECT id FROM author WHERE email = :email AND name = :name AND phone = :phone AND website = :website")
    suspend fun authorExists(email: String?, name: String?, phone: String?, website: String?): Int?

    @Upsert
    suspend fun upsertAuthor(authorEntity: AuthorEntity): Long

    @Upsert
    suspend fun upsertGraphics(graphicEntity: List<GraphicEntity>)

    @Upsert
    suspend fun upsertScreenshots(screenshotEntity: List<ScreenshotEntity>)

    @Upsert
    suspend fun upsertLinks(linksEntity: LinksEntity)

    @Upsert
    suspend fun upsertDonate(donateEntity: List<DonateEntity>)

    @Transaction
    suspend fun upsertMetadata(
        repoId: Int,
        packageName: String,
        metadata: MetadataV2,
    ) {
        val authorId = authorExists(
            email = metadata.authorEmail,
            name = metadata.authorName,
            phone = metadata.authorPhone,
            website = metadata.authorWebSite,
        ) ?: upsertAuthor(metadata.authorEntity()).toInt()
        val appId = upsert(metadata.appEntity(packageName, repoId, authorId)).toInt()
        upsertGraphics(metadata.localizedGraphics(appId))
        metadata.screenshots?.localizedScreenshots(appId)?.let { upsertScreenshots(it) }
        upsertLinks(metadata.linkEntity(appId))
        metadata.donateEntity(appId)?.let { upsertDonate(it) }
    }

    @Query("DELETE FROM app WHERE id = :id")
    suspend fun delete(id: Int)

}
