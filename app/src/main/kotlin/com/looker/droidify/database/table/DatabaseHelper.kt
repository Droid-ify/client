package com.looker.droidify.database.table

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import androidx.core.database.sqlite.transaction
import com.looker.droidify.database.Database.RepositoryAdapter
import com.looker.droidify.database.Database.Schema
import com.looker.droidify.database.Database.jsonParse
import com.looker.droidify.database.Database.query
import com.looker.droidify.index.OemRepositoryParser
import com.looker.droidify.model.Repository
import com.looker.droidify.utility.common.extension.asSequence
import com.looker.droidify.utility.common.extension.firstOrNull
import com.looker.droidify.utility.serialization.repository

private const val DB_LEGACY_NAME = "droidify"

private const val DB_LEGACY_VERSION = 6

class DatabaseHelper(context: Context) :
    SQLiteOpenHelper(context, DB_LEGACY_NAME, null, DB_LEGACY_VERSION) {
    var created = false
        private set
    var updated = false
        private set

    override fun onCreate(db: SQLiteDatabase) {
        // Create all tables
        db.execSQL(Schema.Repository.formatCreateTable(Schema.Repository.name))
        db.execSQL(Schema.Product.formatCreateTable(Schema.Product.name))
        db.execSQL(Schema.Category.formatCreateTable(Schema.Category.name))

        // Add default repositories for new database
        db.addDefaultRepositories()
        db.addOemRepositories()
        this.created = true
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.removeRepositories()
        db.addNewlyAddedRepositories()
        db.addOemRepositories()
        this.updated = true
    }

    override fun onDowngrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // Handle database downgrades if needed
        onUpgrade(db, oldVersion, newVersion)
    }

    override fun onOpen(db: SQLiteDatabase) {
        // Handle memory tables and indexes
        db.execSQL("ATTACH DATABASE ':memory:' AS memory")
        handleTables(db, Schema.Installed, Schema.Lock)
        handleIndexes(
            db,
            Schema.Repository,
            Schema.Product,
            Schema.Category,
            Schema.Installed,
            Schema.Lock,
        )
        dropOldTables(db, Schema.Repository, Schema.Product, Schema.Category)
    }

    private fun SQLiteDatabase.addOemRepositories() {
        OemRepositoryParser
            .getSystemDefaultRepos()
            ?.forEach { repo -> RepositoryAdapter.put(repo, database = this) }
    }

    private fun SQLiteDatabase.addDefaultRepositories() {
        (Repository.defaultRepositories + Repository.newRepos())
            .sortedBy { it.name }
            .forEach { repo -> RepositoryAdapter.put(repo, database = this) }
    }

    private fun SQLiteDatabase.addNewlyAddedRepositories() {
        // Add only newly added repositories, checking for existing ones
        val existingRepos = query(
            Schema.Repository.name,
            columns = arrayOf(Schema.Repository.ROW_DATA),
            selection = null,
            signal = null,
        ).use { cursor ->
            cursor.asSequence().mapNotNull {
                val dataIndex = it.getColumnIndexOrThrow(Schema.Repository.ROW_DATA)
                val data = it.getBlob(dataIndex)

                try {
                    data.jsonParse { json -> json.repository() }.address
                } catch (_: Exception) {
                    null
                }
            }.toSet()
        }

        // Only add repositories that don't already exist
        val reposToAdd = Repository.newRepos().filter { repo ->
            repo.address !in existingRepos
        }

        if (reposToAdd.isNotEmpty()) {
            reposToAdd.forEach { repo ->
                RepositoryAdapter.put(repo, database = this)
            }
        }
    }

    private fun SQLiteDatabase.removeRepositories() {
        // Remove repositories that are in the toRemove list
        val reposToRemove = Repository.addressesToRemove()
        if (reposToRemove.isEmpty()) return

        // Get all repositories with their IDs and addresses
        val existingRepos = query(
            Schema.Repository.name,
            columns = arrayOf(Schema.Repository.ROW_ID, Schema.Repository.ROW_DATA),
            selection = null,
            signal = null,
        ).use { cursor ->
            cursor.asSequence().mapNotNull {
                val idIndex = it.getColumnIndexOrThrow(Schema.Repository.ROW_ID)
                val dataIndex = it.getColumnIndexOrThrow(Schema.Repository.ROW_DATA)
                val id = it.getLong(idIndex)
                val data = it.getBlob(dataIndex)

                try {
                    val repo = data.jsonParse { json -> json.repository() }
                    id to repo.address
                } catch (_: Exception) {
                    null
                }
            }.toMap()
        }

        // Find repositories to remove
        val reposToRemoveIds = existingRepos.filter { (_, address) ->
            address in reposToRemove
        }.keys

        if (reposToRemoveIds.isNotEmpty()) {
            transaction {
                reposToRemoveIds.forEach { repoId ->
                    // Directly update the database to mark repository as deleted
                    update(
                        Schema.Repository.name,
                        android.content.ContentValues().apply {
                            put(Schema.Repository.ROW_DELETED, 1)
                        },
                        "${Schema.Repository.ROW_ID} = ?",
                        arrayOf(repoId.toString()),
                    )
                }
            }
        }
    }

    private fun handleTables(db: SQLiteDatabase, vararg tables: Table): Boolean {
        val shouldRecreate = tables.any { table ->
            val sql = db.query(
                "${table.databasePrefix}sqlite_master",
                columns = arrayOf("sql"),
                selection = Pair("type = ? AND name = ?", arrayOf("table", table.innerName)),
            ).use { it.firstOrNull()?.getString(0) }.orEmpty()
            table.formatCreateTable(table.innerName) != sql
        }
        return shouldRecreate && run {
            val shouldVacuum = tables.map {
                db.execSQL("DROP TABLE IF EXISTS ${it.name}")
                db.execSQL(it.formatCreateTable(it.name))
                !it.memory
            }
            if (shouldVacuum.any { it } && !db.inTransaction()) {
                db.execSQL("VACUUM")
            }
            true
        }
    }

    private fun handleIndexes(db: SQLiteDatabase, vararg tables: Table) {
        val shouldVacuum = tables.map { table ->
            val sqls = db.query(
                "${table.databasePrefix}sqlite_master",
                columns = arrayOf("name", "sql"),
                selection = Pair("type = ? AND tbl_name = ?", arrayOf("index", table.innerName)),
            )
                .use { cursor ->
                    cursor.asSequence()
                        .mapNotNull { it.getString(1)?.let { sql -> Pair(it.getString(0), sql) } }
                        .toList()
                }
                .filter { !it.first.startsWith("sqlite_") }
            val createIndexes = table.createIndexPairFormatted?.let { listOf(it) }.orEmpty()
            createIndexes.map { it.first } != sqls.map { it.second } && run {
                for (name in sqls.map { it.first }) {
                    db.execSQL("DROP INDEX IF EXISTS $name")
                }
                for (createIndexPair in createIndexes) {
                    db.execSQL(createIndexPair.second)
                }
                !table.memory
            }
        }
        if (shouldVacuum.any { it } && !db.inTransaction()) {
            db.execSQL("VACUUM")
        }
    }

    private fun dropOldTables(db: SQLiteDatabase, vararg neededTables: Table) {
        val tables = db.query(
            "sqlite_master",
            columns = arrayOf("name"),
            selection = Pair("type = ?", arrayOf("table")),
        )
            .use { cursor -> cursor.asSequence().mapNotNull { it.getString(0) }.toList() }
            .filter { !it.startsWith("sqlite_") && !it.startsWith("android_") }
            .toSet() - neededTables.mapNotNull { if (it.memory) null else it.name }.toSet()
        if (tables.isNotEmpty()) {
            for (table in tables) {
                db.execSQL("DROP TABLE IF EXISTS $table")
            }
            if (!db.inTransaction()) {
                db.execSQL("VACUUM")
            }
        }
    }
}
