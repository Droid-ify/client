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
import com.looker.droidify.data.local.model.LocalizedAppIconEntity
import com.looker.droidify.data.local.model.VersionEntity
import com.looker.droidify.data.model.AppMinimal
import com.looker.droidify.data.model.FilePath
import com.looker.droidify.data.model.PackageName
import com.looker.droidify.datastore.model.SortOrder
import com.looker.droidify.sync.v2.model.DefaultName
import com.looker.droidify.sync.v2.model.Tag
import kotlinx.coroutines.flow.Flow

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

    suspend fun query(
        sortOrder: SortOrder,
        searchQuery: String? = null,
        repoId: Int? = null,
        categoriesToInclude: List<DefaultName>? = null,
        categoriesToExclude: List<DefaultName>? = null,
        antiFeaturesToInclude: List<Tag>? = null,
        antiFeaturesToExclude: List<Tag>? = null,
        locale: String,
    ): List<AppMinimal> = _rawQueryAppMinimal(
        searchQueryMinimal(
            sortOrder = sortOrder,
            searchQuery = searchQuery,
            repoId = repoId,
            categoriesToInclude = categoriesToInclude,
            categoriesToExclude = categoriesToExclude,
            antiFeaturesToInclude = antiFeaturesToInclude,
            antiFeaturesToExclude = antiFeaturesToExclude,
            locale = locale,
        ),
    ).map {
        AppMinimal(
            appId = it.appId.toLong(),
            packageName = PackageName(it.packageName),
            name = it.name,
            summary = it.summary,
            icon = FilePath(it.baseAddress, it.iconName),
            suggestedVersion = it.suggestedVersion ?: "",
        )
    }

    // Projection row for AppMinimal construction
    data class AppMinimalRow(
        val appId: Int,
        val packageName: String,
        val name: String,
        val summary: String?,
        val baseAddress: String,
        val iconName: String?,
        val suggestedVersion: String?,
    )

    @RawQuery
    suspend fun _rawQueryAppMinimal(query: SimpleSQLiteQuery): List<AppMinimalRow>

    // Build query for AppMinimal with localization and repo address
    private fun searchQueryMinimal(
        sortOrder: SortOrder,
        searchQuery: String?,
        repoId: Int?,
        categoriesToInclude: List<DefaultName>?,
        categoriesToExclude: List<DefaultName>?,
        antiFeaturesToInclude: List<Tag>?,
        antiFeaturesToExclude: List<Tag>?,
        locale: String,
    ): SimpleSQLiteQuery {
        logQuery(
            "sortOrder" to sortOrder,
            "searchQuery" to searchQuery,
            "repoId" to repoId,
            "categoriesToInclude" to categoriesToInclude,
            "categoriesToExclude" to categoriesToExclude,
            "antiFeaturesToInclude" to antiFeaturesToInclude,
            "antiFeaturesToExclude" to antiFeaturesToExclude,
            "locale" to locale,
        )
        val args = arrayListOf<Any?>()

        val query = buildString(2048) {
            append(
                """
                SELECT
                    app.id AS appId,
                    app.packageName AS packageName,
                    COALESCE(n_loc.name, n_en.name) AS name,
                    COALESCE(s_loc.summary, s_en.summary) AS summary,
                    repo.address AS baseAddress,
                    COALESCE(i_loc.icon_name, i_en.icon_name) AS iconName,
                    (
                        SELECT v.versionName FROM version v
                        WHERE v.appId = app.id
                        ORDER BY v.versionCode DESC
                        LIMIT 1
                    ) AS suggestedVersion
                FROM app
                JOIN repository AS repo ON app.repoId = repo.id
                LEFT JOIN localized_app_name AS n_loc ON n_loc.appId = app.id AND n_loc.locale = ?
                LEFT JOIN localized_app_name AS n_en ON n_en.appId = app.id AND n_en.locale = 'en-US'
                LEFT JOIN localized_app_summary AS s_loc ON s_loc.appId = app.id AND s_loc.locale = ?
                LEFT JOIN localized_app_summary AS s_en ON s_en.appId = app.id AND s_en.locale = 'en-US'
                LEFT JOIN localized_app_icon AS i_loc ON i_loc.appId = app.id AND i_loc.locale = ?
                LEFT JOIN localized_app_icon AS i_en ON i_en.appId = app.id AND i_en.locale = 'en-US'
                LEFT JOIN localized_app_description AS d_loc ON d_loc.appId = app.id AND d_loc.locale = ?
                LEFT JOIN localized_app_description AS d_en ON d_en.appId = app.id AND d_en.locale = 'en-US'
                """.trimIndent(),
            )
            // locale args repeated for each localized table
            args.add(locale)
            args.add(locale)
            args.add(locale)
            args.add(locale)

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
                        app.packageName LIKE ?
                        OR COALESCE(n_loc.name, n_en.name) LIKE ?
                        OR COALESCE(s_loc.summary, s_en.summary) LIKE ?
                        OR COALESCE(d_loc.description, d_en.description) LIKE ?
                    )
                    """.trimIndent(),
                )
                args.addAll(listOf(searchPattern, searchPattern, searchPattern, searchPattern))
            }

            append(" ORDER BY ")
            
            if (searchQuery != null) {
                val searchPattern = "%${searchQuery}%"
                append("(CASE WHEN COALESCE(n_loc.name, n_en.name) LIKE ? THEN 4 ELSE 0 END) + ")
                append("(CASE WHEN COALESCE(s_loc.summary, s_en.summary) LIKE ? THEN 3 ELSE 0 END) + ")
                append("(CASE WHEN app.packageName LIKE ? THEN 2 ELSE 0 END) + ")
                append("(CASE WHEN COALESCE(d_loc.description, d_en.description) LIKE ? THEN 1 ELSE 0 END) DESC, ")
                args.addAll(listOf(searchPattern, searchPattern, searchPattern, searchPattern))
            }

            when (sortOrder) {
                SortOrder.UPDATED -> append("app.lastUpdated DESC")
                SortOrder.ADDED -> append("app.added DESC")
                SortOrder.SIZE -> append("version.apk_size DESC")
                SortOrder.NAME -> Unit
            }
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
        app.lastUpdated DESC
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
        SELECT v.appId AS appId, MAX(v.versionName) AS versionName
        FROM version v
        GROUP BY appId
        """
    )
    suspend fun suggestedVersionNamesAll(): Map<Int, String>

    @Transaction
    @Query("SELECT * FROM app WHERE packageName = :packageName")
    fun queryAppEntity(packageName: String): Flow<List<AppEntityRelations>>

    @Query("SELECT COUNT(*) FROM app")
    suspend fun count(): Int

    @Query("DELETE FROM app WHERE id = :id")
    suspend fun delete(id: Int)

    @Query("DELETE FROM app WHERE repoId = :repoId")
    suspend fun deleteByRepoId(repoId: Int)

    @Query("SELECT name FROM localized_app_name WHERE appId = :id AND (locale = :locale OR locale = \'en-US\')")
    suspend fun name(id: Int, locale: String): String?

    @Query("SELECT summary FROM localized_app_summary WHERE appId = :id AND (locale = :locale OR locale = \'en-US\')")
    suspend fun summary(id: Int, locale: String): String?

    @Query("SELECT description FROM localized_app_description WHERE appId = :id AND (locale = :locale OR locale = \'en-US\')")
    suspend fun description(id: Int, locale: String): String?

    @Query("SELECT * FROM localized_app_icon WHERE appId = :id AND (locale = :locale OR locale = \'en-US\')")
    suspend fun icon(id: Int, locale: String): LocalizedAppIconEntity?
}
