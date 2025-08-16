package com.looker.droidify.data.local.dao

import androidx.room.Dao
import androidx.room.MapInfo
import androidx.room.Query
import androidx.room.RawQuery
import androidx.room.Transaction
import androidx.sqlite.db.SimpleSQLiteQuery
import com.looker.droidify.data.local.model.AntiFeatureAppRelation
import com.looker.droidify.data.local.model.AppEntity
import com.looker.droidify.data.local.model.AppEntityRelations
import com.looker.droidify.data.local.model.CategoryAppRelation
import com.looker.droidify.data.local.model.VersionEntity
import com.looker.droidify.datastore.model.SortOrder
import com.looker.droidify.sync.v2.model.DefaultName
import com.looker.droidify.sync.v2.model.Tag
import kotlinx.coroutines.flow.Flow

// DTO for batch fetching suggested version names per app
 data class VersionNameRow(
     val appId: Int,
     val versionName: String,
 )

@Dao
interface AppDao {

    @RawQuery(
        observedEntities = [
            AppEntity::class,
            VersionEntity::class,
            CategoryAppRelation::class,
            AntiFeatureAppRelation::class,
        ],
    )
    fun _rawStreamAppEntities(query: SimpleSQLiteQuery): Flow<List<AppEntity>>

    @RawQuery
    suspend fun _rawQueryAppEntities(query: SimpleSQLiteQuery): List<AppEntity>

    fun stream(
        sortOrder: SortOrder,
        searchQuery: String? = null,
        repoId: Int? = null,
        categoriesToInclude: List<DefaultName>? = null,
        categoriesToExclude: List<DefaultName>? = null,
        antiFeaturesToInclude: List<Tag>? = null,
        antiFeaturesToExclude: List<Tag>? = null,
    ): Flow<List<AppEntity>> = _rawStreamAppEntities(
        searchQuery(
            sortOrder = sortOrder,
            searchQuery = searchQuery,
            repoId = repoId,
            categoriesToInclude = categoriesToInclude,
            categoriesToExclude = categoriesToExclude,
            antiFeaturesToInclude = antiFeaturesToInclude,
            antiFeaturesToExclude = antiFeaturesToExclude,
        ),
    )

    suspend fun query(
        sortOrder: SortOrder,
        searchQuery: String? = null,
        repoId: Int? = null,
        categoriesToInclude: List<DefaultName>? = null,
        categoriesToExclude: List<DefaultName>? = null,
        antiFeaturesToInclude: List<Tag>? = null,
        antiFeaturesToExclude: List<Tag>? = null,
    ): List<AppEntity> = _rawQueryAppEntities(
        searchQuery(
            sortOrder = sortOrder,
            searchQuery = searchQuery,
            repoId = repoId,
            categoriesToInclude = categoriesToInclude,
            categoriesToExclude = categoriesToExclude,
            antiFeaturesToInclude = antiFeaturesToInclude,
            antiFeaturesToExclude = antiFeaturesToExclude,
        ),
    )

    private fun searchQuery(
        sortOrder: SortOrder,
        searchQuery: String?,
        repoId: Int?,
        categoriesToInclude: List<DefaultName>?,
        categoriesToExclude: List<DefaultName>?,
        antiFeaturesToInclude: List<Tag>?,
        antiFeaturesToExclude: List<Tag>?,
    ): SimpleSQLiteQuery {
        val args = arrayListOf<Any?>()

        val query = buildString(1024) {
            append("SELECT DISTINCT app.* FROM app")
            if (sortOrder == SortOrder.SIZE) {
                append(" LEFT JOIN version ON app.id = version.appId")
            }
            if (categoriesToInclude != null || categoriesToExclude != null) {
                append(" LEFT JOIN category_app_relation ON app.id = category_app_relation.id")
            }
            if (antiFeaturesToExclude != null || antiFeaturesToInclude != null) {
                append(" LEFT JOIN anti_features_app_relation ON app.id = anti_features_app_relation.appId")
            }
            append(" WHERE 1")

            if (repoId != null) {
                append(" AND app.repoId = ?")
                args.add(repoId)
            }

            if (categoriesToInclude != null) {
                append(" AND category_app_relation.defaultName IN (")
                append(categoriesToInclude.joinToString(", ") { "?" })
                append(")")
                args.addAll(categoriesToInclude)
            }

            if (categoriesToExclude != null) {
                append(" AND category_app_relation.defaultName NOT IN (")
                append(categoriesToExclude.joinToString(", ") { "?" })
                append(")")
                args.addAll(categoriesToExclude)
            }

            if (antiFeaturesToInclude != null) {
                append(" AND anti_features_app_relation.tag IN (")
                append(antiFeaturesToInclude.joinToString(", ") { "?" })
                append(")")
                args.addAll(antiFeaturesToInclude)
            }

            if (antiFeaturesToExclude != null) {
                append(" AND anti_features_app_relation.tag NOT IN (")
                append(antiFeaturesToExclude.joinToString(", ") { "?" })
                append(")")
                args.addAll(antiFeaturesToExclude)
            }

            if (searchQuery != null) {
                val searchPattern = "%${searchQuery}%"
                append(
                    """
                    AND (
                        app.name LIKE ?
                        OR app.summary LIKE ?
                        OR app.packageName LIKE ?
                        OR app.description LIKE ?
                    )""",
                )
                args.addAll(listOf(searchPattern, searchPattern, searchPattern, searchPattern))
            }

            append(" ORDER BY ")

            // Weighting: name > summary > packageName > description
            if (searchQuery != null) {
                val searchPattern = "%${searchQuery}%"
                append("(CASE WHEN app.name LIKE ? THEN 4 ELSE 0 END) + ")
                append("(CASE WHEN app.summary LIKE ? THEN 3 ELSE 0 END) + ")
                append("(CASE WHEN app.packageName LIKE ? THEN 2 ELSE 0 END) + ")
                append("(CASE WHEN app.description LIKE ? THEN 1 ELSE 0 END) DESC, ")
                args.addAll(listOf(searchPattern, searchPattern, searchPattern, searchPattern))
            }

            when (sortOrder) {
                SortOrder.UPDATED -> append("app.lastUpdated DESC, ")
                SortOrder.ADDED -> append("app.added DESC, ")
                SortOrder.SIZE -> append("version.apk_size DESC, ")
                SortOrder.NAME -> Unit
            }
            append("app.name COLLATE LOCALIZED ASC")
        }

        return SimpleSQLiteQuery(query, args.toTypedArray())
    }

    @Query(
        """
        SELECT app.*
        FROM app
        LEFT JOIN installed
        ON app.packageName = installed.packageName
        LEFT JOIN version
        ON version.appId = app.id
        WHERE installed.packageName IS NOT NULL
        ORDER BY
        CASE WHEN version.versionCode > installed.versionCode THEN 1 ELSE 2 END,
        app.lastUpdated DESC,
        app.name COLLATE LOCALIZED ASC
        """,
    )
    fun installedStream(): Flow<List<AppEntity>>

    @Query("SELECT versionCode FROM version WHERE appId = :appId ORDER BY versionCode DESC LIMIT 1")
    suspend fun suggestedVersionCode(appId: Int): Long

    @Query("SELECT versionName FROM version WHERE appId = :appId ORDER BY versionCode DESC LIMIT 1")
    suspend fun suggestedVersionName(appId: Int): String

    // Batch fetch suggested (max versionCode) versionName for multiple appIds
    @MapInfo(keyColumn = "appId", valueColumn = "versionName")
    @Query(
        """
        SELECT v.appId AS appId, v.versionName AS versionName
        FROM version v
        JOIN (
          SELECT appId, MAX(versionCode) AS maxCode
          FROM version
          WHERE appId IN (:appIds)
          GROUP BY appId
        ) mv ON v.appId = mv.appId AND v.versionCode = mv.maxCode
        """
    )
    suspend fun suggestedVersionNames(appIds: List<Int>): Map<Int, String>

    @Transaction
    @Query("SELECT * FROM app WHERE packageName = :packageName")
    fun queryAppEntity(packageName: String): Flow<List<AppEntityRelations>>

    @Query("SELECT COUNT(*) FROM app")
    suspend fun count(): Int

    @Query("DELETE FROM app WHERE id = :id")
    suspend fun delete(id: Int)

    @Query("DELETE FROM app WHERE repoId = :repoId")
    suspend fun deleteByRepoId(repoId: Int)
}
