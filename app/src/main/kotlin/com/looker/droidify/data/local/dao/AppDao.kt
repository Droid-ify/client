package com.looker.droidify.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import com.looker.droidify.data.local.model.AppEntity
import com.looker.droidify.data.local.model.AppEntityRelations
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

@Dao
interface AppDao {

    @Query("SELECT * FROM app")
    fun stream(): Flow<List<AppEntity>>

    @Query("SELECT * FROM app WHERE packageName = :packageName")
    fun queryAppEntity(packageName: String): Flow<List<AppEntityRelations>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(appEntity: AppEntity): Long

    @Query("SELECT COUNT(*) FROM app")
    suspend fun count(): Int

    @Query("SELECT id FROM app WHERE packageName = :packageName")
    suspend fun getIdByPackageName(packageName: String): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAuthor(authorEntity: AuthorEntity): Long

    @Query("SELECT id FROM author WHERE email = :email AND name = :name AND website = :website")
    suspend fun getAuthorId(email: String?, name: String?, website: String?): Int

    @Upsert
    suspend fun upsertScreenshots(screenshotEntity: List<ScreenshotEntity>)

    @Upsert
    suspend fun upsertLinks(linksEntity: LinksEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGraphics(graphicEntity: List<GraphicEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDonate(donateEntity: List<DonateEntity>)

    @Transaction
    suspend fun upsertMetadata(
        repoId: Int,
        packageName: String,
        metadata: MetadataV2,
    ) {
        withContext(Dispatchers.IO) {
            val author = metadata.authorEntity()
            val authorId = insertAuthor(author).toInt().takeIf { it > 0 } ?: getAuthorId(
                email = author.email,
                name = author.name,
                website = author.website,
            )
            val appId = insert(
                appEntity = metadata.appEntity(
                    packageName = packageName,
                    repoId = repoId,
                    authorId = authorId,
                ),
            ).toInt().takeIf { it > 0 } ?: getIdByPackageName(packageName)
            upsertLinks(metadata.linkEntity(appId))
            metadata.screenshots?.localizedScreenshots(appId)?.let { upsertScreenshots(it) }
            metadata.localizedGraphics(appId)?.let { insertGraphics(it) }
            metadata.donateEntity(appId)?.let { insertDonate(it) }
        }
    }

    @Query("DELETE FROM app WHERE id = :id")
    suspend fun delete(id: Int)

}
