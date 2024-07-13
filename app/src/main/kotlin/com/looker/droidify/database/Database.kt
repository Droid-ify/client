package com.looker.droidify.database

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.os.CancellationSignal
import androidx.core.database.sqlite.transaction
import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.core.JsonParser
import com.looker.core.common.extension.Json
import com.looker.core.common.extension.asSequence
import com.looker.core.common.extension.firstOrNull
import com.looker.core.common.extension.parseDictionary
import com.looker.core.common.extension.writeDictionary
import com.looker.core.common.log
import com.looker.core.datastore.model.SortOrder
import com.looker.droidify.model.InstalledItem
import com.looker.droidify.model.Product
import com.looker.droidify.model.ProductItem
import com.looker.droidify.model.Repository
import com.looker.droidify.BuildConfig
import com.looker.droidify.utility.serialization.product
import com.looker.droidify.utility.serialization.productItem
import com.looker.droidify.utility.serialization.repository
import com.looker.droidify.utility.serialization.serialize
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import kotlin.collections.component1
import kotlin.collections.component2
import kotlin.collections.set

object Database {
    fun init(context: Context): Boolean {
        val helper = Helper(context)
        db = helper.writableDatabase
        if (helper.created) {
            for (repository in Repository.defaultRepositories.sortedBy { it.name }) {
                RepositoryAdapter.put(repository)
            }
        }
        RepositoryAdapter.removeDuplicates()
        return helper.created || helper.updated
    }

    private lateinit var db: SQLiteDatabase

    private interface Table {
        val memory: Boolean
        val innerName: String
        val createTable: String
        val createIndex: String?
            get() = null

        val databasePrefix: String
            get() = if (memory) "memory." else ""

        val name: String
            get() = "$databasePrefix$innerName"

        fun formatCreateTable(name: String): String {
            return "CREATE TABLE $name (${QueryBuilder.trimQuery(createTable)})"
        }

        val createIndexPairFormatted: Pair<String, String>?
            get() = createIndex?.let {
                Pair(
                    "CREATE INDEX ${innerName}_index ON $innerName ($it)",
                    "CREATE INDEX ${name}_index ON $innerName ($it)"
                )
            }
    }

    private object Schema {
        object Repository : Table {
            const val ROW_ID = "_id"
            const val ROW_ENABLED = "enabled"
            const val ROW_DELETED = "deleted"
            const val ROW_DATA = "data"

            override val memory = false
            override val innerName = "repository"
            override val createTable = """
        $ROW_ID INTEGER PRIMARY KEY AUTOINCREMENT,
        $ROW_ENABLED INTEGER NOT NULL,
        $ROW_DELETED INTEGER NOT NULL,
        $ROW_DATA BLOB NOT NULL
      """
        }

        object Product : Table {
            const val ROW_REPOSITORY_ID = "repository_id"
            const val ROW_PACKAGE_NAME = "package_name"
            const val ROW_NAME = "name"
            const val ROW_SUMMARY = "summary"
            const val ROW_DESCRIPTION = "description"
            const val ROW_ADDED = "added"
            const val ROW_UPDATED = "updated"
            const val ROW_VERSION_CODE = "version_code"
            const val ROW_SIGNATURES = "signatures"
            const val ROW_COMPATIBLE = "compatible"
            const val ROW_DATA = "data"
            const val ROW_DATA_ITEM = "data_item"

            override val memory = false
            override val innerName = "product"
            override val createTable = """
        $ROW_REPOSITORY_ID INTEGER NOT NULL,
        $ROW_PACKAGE_NAME TEXT NOT NULL,
        $ROW_NAME TEXT NOT NULL,
        $ROW_SUMMARY TEXT NOT NULL,
        $ROW_DESCRIPTION TEXT NOT NULL,
        $ROW_ADDED INTEGER NOT NULL,
        $ROW_UPDATED INTEGER NOT NULL,
        $ROW_VERSION_CODE INTEGER NOT NULL,
        $ROW_SIGNATURES TEXT NOT NULL,
        $ROW_COMPATIBLE INTEGER NOT NULL,
        $ROW_DATA BLOB NOT NULL,
        $ROW_DATA_ITEM BLOB NOT NULL,
        PRIMARY KEY ($ROW_REPOSITORY_ID, $ROW_PACKAGE_NAME)
      """
            override val createIndex = ROW_PACKAGE_NAME
        }

        object Category : Table {
            const val ROW_REPOSITORY_ID = "repository_id"
            const val ROW_PACKAGE_NAME = "package_name"
            const val ROW_NAME = "name"

            override val memory = false
            override val innerName = "category"
            override val createTable = """
        $ROW_REPOSITORY_ID INTEGER NOT NULL,
        $ROW_PACKAGE_NAME TEXT NOT NULL,
        $ROW_NAME TEXT NOT NULL,
        PRIMARY KEY ($ROW_REPOSITORY_ID, $ROW_PACKAGE_NAME, $ROW_NAME)
      """
            override val createIndex = "$ROW_PACKAGE_NAME, $ROW_NAME"
        }

        object Installed : Table {
            const val ROW_PACKAGE_NAME = "package_name"
            const val ROW_VERSION = "version"
            const val ROW_VERSION_CODE = "version_code"
            const val ROW_SIGNATURE = "signature"

            override val memory = true
            override val innerName = "installed"
            override val createTable = """
        $ROW_PACKAGE_NAME TEXT PRIMARY KEY,
        $ROW_VERSION TEXT NOT NULL,
        $ROW_VERSION_CODE INTEGER NOT NULL,
        $ROW_SIGNATURE TEXT NOT NULL
      """
        }

        object Lock : Table {
            const val ROW_PACKAGE_NAME = "package_name"
            const val ROW_VERSION_CODE = "version_code"

            override val memory = true
            override val innerName = "lock"
            override val createTable = """
        $ROW_PACKAGE_NAME TEXT PRIMARY KEY,
        $ROW_VERSION_CODE INTEGER NOT NULL
      """
        }

        object Synthetic {
            const val ROW_CAN_UPDATE = "can_update"
            const val ROW_MATCH_RANK = "match_rank"
        }
    }

    private class Helper(context: Context) : SQLiteOpenHelper(context, "droidify", null, 4) {
        var created = false
            private set
        var updated = false
            private set

        override fun onCreate(db: SQLiteDatabase) = Unit
        override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) =
            onVersionChange(db)

        override fun onDowngrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) =
            onVersionChange(db)

        private fun onVersionChange(db: SQLiteDatabase) {
            handleTables(db, true, Schema.Product, Schema.Category)
            addRepos(db, Repository.newlyAdded)
            this.updated = true
        }

        override fun onOpen(db: SQLiteDatabase) {
            val create = handleTables(db, false, Schema.Repository)
            val updated = handleTables(db, create, Schema.Product, Schema.Category)
            db.execSQL("ATTACH DATABASE ':memory:' AS memory")
            handleTables(db, false, Schema.Installed, Schema.Lock)
            handleIndexes(
                db,
                Schema.Repository,
                Schema.Product,
                Schema.Category,
                Schema.Installed,
                Schema.Lock
            )
            dropOldTables(db, Schema.Repository, Schema.Product, Schema.Category)
            this.created = this.created || create
            this.updated = this.updated || create || updated
        }
    }

    private fun handleTables(db: SQLiteDatabase, recreate: Boolean, vararg tables: Table): Boolean {
        val shouldRecreate = recreate || tables.any { table ->
            val sql = db.query(
                "${table.databasePrefix}sqlite_master",
                columns = arrayOf("sql"),
                selection = Pair("type = ? AND name = ?", arrayOf("table", table.innerName))
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

    private fun addRepos(db: SQLiteDatabase, repos: List<Repository>) {
        if (BuildConfig.DEBUG) {
            log("Add Repos: $repos", "RepositoryAdapter")
        }
        if (repos.isEmpty()) return
        db.transaction {
            repos.forEach {
                RepositoryAdapter.put(it, database = this)
            }
        }
    }

    private fun handleIndexes(db: SQLiteDatabase, vararg tables: Table) {
        val shouldVacuum = tables.map { table ->
            val sqls = db.query(
                "${table.databasePrefix}sqlite_master",
                columns = arrayOf("name", "sql"),
                selection = Pair("type = ? AND tbl_name = ?", arrayOf("index", table.innerName))
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
            selection = Pair("type = ?", arrayOf("table"))
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

    sealed class Subject {
        data object Repositories : Subject()
        data class Repository(val id: Long) : Subject()
        data object Products : Subject()
    }

    private val observers = mutableMapOf<Subject, MutableSet<() -> Unit>>()

    private fun dataObservable(subject: Subject): (Boolean, () -> Unit) -> Unit =
        { register, observer ->
            synchronized(observers) {
                val set = observers[subject] ?: run {
                    val set = mutableSetOf<() -> Unit>()
                    observers[subject] = set
                    set
                }
                if (register) {
                    set += observer
                } else {
                    set -= observer
                }
            }
        }

    fun flowCollection(subject: Subject): Flow<Unit> = callbackFlow {
        val callback: () -> Unit = { trySend(Unit) }
        val dataObservable = dataObservable(subject)
        dataObservable(true, callback)

        awaitClose { dataObservable(false, callback) }
    }.flowOn(Dispatchers.IO)

    private fun notifyChanged(vararg subjects: Subject) {
        synchronized(observers) {
            subjects.asSequence().mapNotNull { observers[it] }.flatten().forEach { it() }
        }
    }

    private fun SQLiteDatabase.insertOrReplace(
        replace: Boolean,
        table: String,
        contentValues: ContentValues
    ): Long {
        return if (replace) {
            replace(table, null, contentValues)
        } else {
            insert(
                table,
                null,
                contentValues
            )
        }
    }

    private fun SQLiteDatabase.query(
        table: String,
        columns: Array<String>? = null,
        selection: Pair<String, Array<String>>? = null,
        orderBy: String? = null,
        signal: CancellationSignal? = null
    ): Cursor {
        return query(
            false,
            table,
            columns,
            selection?.first,
            selection?.second,
            null,
            null,
            orderBy,
            null,
            signal
        )
    }

    private fun Cursor.observable(subject: Subject): ObservableCursor {
        return ObservableCursor(this, dataObservable(subject))
    }

    fun <T> ByteArray.jsonParse(callback: (JsonParser) -> T): T {
        return Json.factory.createParser(this).use { it.parseDictionary(callback) }
    }

    fun jsonGenerate(callback: (JsonGenerator) -> Unit): ByteArray {
        val outputStream = ByteArrayOutputStream()
        Json.factory.createGenerator(outputStream).use { it.writeDictionary(callback) }
        return outputStream.toByteArray()
    }

    object RepositoryAdapter {
        internal fun putWithoutNotification(
            repository: Repository,
            shouldReplace: Boolean,
            database: SQLiteDatabase
        ): Long {
            return database.insertOrReplace(
                shouldReplace,
                Schema.Repository.name,
                ContentValues().apply {
                    if (shouldReplace) {
                        put(Schema.Repository.ROW_ID, repository.id)
                    }
                    put(Schema.Repository.ROW_ENABLED, if (repository.enabled) 1 else 0)
                    put(Schema.Repository.ROW_DELETED, 0)
                    put(Schema.Repository.ROW_DATA, jsonGenerate(repository::serialize))
                }
            )
        }

        fun put(repository: Repository, database: SQLiteDatabase = db): Repository {
            val shouldReplace = repository.id >= 0L
            val newId = putWithoutNotification(repository, shouldReplace, database)
            val id = if (shouldReplace) repository.id else newId
            notifyChanged(Subject.Repositories, Subject.Repository(id), Subject.Products)
            return if (newId != repository.id) repository.copy(id = newId) else repository
        }

        fun removeDuplicates() {
            db.transaction {
                val all = getAll()
                val different = all.distinctBy { it.address }
                val duplicates = all - different.toSet()
                duplicates.forEach {
                    markAsDeleted(it.id)
                }
            }
        }

        fun getStream(id: Long): Flow<Repository?> = flowOf(Unit)
            .onCompletion { if (it == null) emitAll(flowCollection(Subject.Repositories)) }
            .map { get(id) }
            .flowOn(Dispatchers.IO)

        fun get(id: Long): Repository? {
            return db.query(
                Schema.Repository.name,
                selection = Pair(
                    "${Schema.Repository.ROW_ID} = ? AND ${Schema.Repository.ROW_DELETED} == 0",
                    arrayOf(id.toString())
                )
            ).use { it.firstOrNull()?.let(::transform) }
        }

        fun getAllStream(): Flow<List<Repository>> = flowOf(Unit)
            .onCompletion { if (it == null) emitAll(flowCollection(Subject.Repositories)) }
            .map { getAll() }
            .flowOn(Dispatchers.IO)

        fun getEnabledStream(): Flow<List<Repository>> = flowOf(Unit)
            .onCompletion { if (it == null) emitAll(flowCollection(Subject.Repositories)) }
            .map { getEnabled() }
            .flowOn(Dispatchers.IO)

        private suspend fun getEnabled(): List<Repository> = withContext(Dispatchers.IO) {
            db.query(
                Schema.Repository.name,
                selection = Pair(
                    "${Schema.Repository.ROW_ENABLED} != 0 AND " +
                        "${Schema.Repository.ROW_DELETED} == 0",
                    emptyArray()
                ),
                signal = null
            ).use { it.asSequence().map(::transform).toList() }
        }

        fun getAll(): List<Repository> {
            return db.query(
                Schema.Repository.name,
                selection = Pair("${Schema.Repository.ROW_DELETED} == 0", emptyArray()),
                signal = null
            ).use { it.asSequence().map(::transform).toList() }
        }

        fun getAllRemovedStream(): Flow<Map<Long, Boolean>> = flowOf(Unit)
            .onCompletion { if (it == null) emitAll(flowCollection(Subject.Repositories)) }
            .map { getAllDisabledDeleted() }
            .flowOn(Dispatchers.IO)

        private fun getAllDisabledDeleted(): Map<Long, Boolean> {
            return db.query(
                Schema.Repository.name,
                columns = arrayOf(Schema.Repository.ROW_ID, Schema.Repository.ROW_DELETED),
                selection = Pair(
                    "${Schema.Repository.ROW_ENABLED} == 0 OR " +
                        "${Schema.Repository.ROW_DELETED} != 0",
                    emptyArray()
                ),
                signal = null
            ).use { parentCursor ->
                parentCursor.asSequence().associate {
                    val idIndex = it.getColumnIndexOrThrow(Schema.Repository.ROW_ID)
                    val isDeletedIndex = it.getColumnIndexOrThrow(Schema.Repository.ROW_DELETED)
                    it.getLong(idIndex) to (it.getInt(isDeletedIndex) != 0)
                }
            }
        }

        fun markAsDeleted(id: Long) {
            db.update(
                Schema.Repository.name,
                ContentValues().apply {
                    put(Schema.Repository.ROW_DELETED, 1)
                },
                "${Schema.Repository.ROW_ID} = ?",
                arrayOf(id.toString())
            )
            notifyChanged(Subject.Repositories, Subject.Repository(id), Subject.Products)
        }

        fun cleanup(removedRepos: Map<Long, Boolean>) {
            val result = removedRepos.map { (id, isDeleted) ->
                val idsString = id.toString()
                val productsCount = db.delete(
                    Schema.Product.name,
                    "${Schema.Product.ROW_REPOSITORY_ID} IN ($idsString)",
                    null
                )
                val categoriesCount = db.delete(
                    Schema.Category.name,
                    "${Schema.Category.ROW_REPOSITORY_ID} IN ($idsString)",
                    null
                )
                if (isDeleted) {
                    db.delete(
                        Schema.Repository.name,
                        "${Schema.Repository.ROW_ID} IN ($id)",
                        null
                    )
                }
                productsCount != 0 || categoriesCount != 0
            }
            if (result.any { it }) {
                notifyChanged(Subject.Products)
            }
        }

        fun importRepos(list: List<Repository>) {
            db.transaction {
                val currentAddresses = getAll().map { it.address }
                val newRepos = list
                    .filter { it.address !in currentAddresses }
                newRepos.forEach { put(it) }
                removeDuplicates()
            }
        }

        fun query(signal: CancellationSignal?): Cursor {
            return db.query(
                Schema.Repository.name,
                selection = Pair("${Schema.Repository.ROW_DELETED} == 0", emptyArray()),
                orderBy = "${Schema.Repository.ROW_ENABLED} DESC",
                signal = signal
            ).observable(Subject.Repositories)
        }

        fun transform(cursor: Cursor): Repository {
            return cursor.getBlob(cursor.getColumnIndexOrThrow(Schema.Repository.ROW_DATA))
                .jsonParse {
                    it.repository().apply {
                        this.id =
                            cursor.getLong(cursor.getColumnIndexOrThrow(Schema.Repository.ROW_ID))
                    }
                }
        }
    }

    object ProductAdapter {

        fun getStream(packageName: String): Flow<List<Product>> = flowOf(Unit)
            .onCompletion { if (it == null) emitAll(flowCollection(Subject.Products)) }
            .map { get(packageName, null) }
            .flowOn(Dispatchers.IO)

        suspend fun getUpdates(): List<ProductItem> = withContext(Dispatchers.IO) {
            query(
                installed = true,
                updates = true,
                searchQuery = "",
                section = ProductItem.Section.All,
                order = SortOrder.NAME,
                signal = null
            ).use {
                it.asSequence()
                    .map(ProductAdapter::transformItem)
                    .toList()
            }
        }

        fun getUpdatesStream(): Flow<List<ProductItem>> = flowOf(Unit)
            .onCompletion { if (it == null) emitAll(flowCollection(Subject.Products)) }
            // Crashes due to immediate retrieval of data?
            .onEach { delay(50) }
            .map { getUpdates() }
            .flowOn(Dispatchers.IO)

        fun get(packageName: String, signal: CancellationSignal?): List<Product> {
            return db.query(
                Schema.Product.name,
                columns = arrayOf(
                    Schema.Product.ROW_REPOSITORY_ID,
                    Schema.Product.ROW_DESCRIPTION,
                    Schema.Product.ROW_DATA
                ),
                selection = Pair("${Schema.Product.ROW_PACKAGE_NAME} = ?", arrayOf(packageName)),
                signal = signal
            ).use { it.asSequence().map(::transform).toList() }
        }

        fun getCountStream(repositoryId: Long): Flow<Int> = flowOf(Unit)
            .onCompletion { if (it == null) emitAll(flowCollection(Subject.Products)) }
            .map { getCount(repositoryId) }
            .flowOn(Dispatchers.IO)

        private fun getCount(repositoryId: Long): Int {
            return db.query(
                Schema.Product.name,
                columns = arrayOf("COUNT (*)"),
                selection = Pair(
                    "${Schema.Product.ROW_REPOSITORY_ID} = ?",
                    arrayOf(repositoryId.toString())
                )
            ).use { it.firstOrNull()?.getInt(0) ?: 0 }
        }

        fun query(
            installed: Boolean,
            updates: Boolean,
            searchQuery: String,
            section: ProductItem.Section,
            order: SortOrder,
            signal: CancellationSignal?
        ): Cursor {
            val builder = QueryBuilder()

            val signatureMatches = """installed.${Schema.Installed.ROW_SIGNATURE} IS NOT NULL AND
        product.${Schema.Product.ROW_SIGNATURES} LIKE ('%.' || installed.${Schema.Installed.ROW_SIGNATURE} || '.%') AND
        product.${Schema.Product.ROW_SIGNATURES} != ''"""

            builder += """SELECT product.rowid AS _id, product.${Schema.Product.ROW_REPOSITORY_ID},
        product.${Schema.Product.ROW_PACKAGE_NAME}, product.${Schema.Product.ROW_NAME},
        product.${Schema.Product.ROW_SUMMARY}, installed.${Schema.Installed.ROW_VERSION},
        (COALESCE(lock.${Schema.Lock.ROW_VERSION_CODE}, -1) NOT IN (0, product.${Schema.Product.ROW_VERSION_CODE}) AND
        product.${Schema.Product.ROW_COMPATIBLE} != 0 AND product.${Schema.Product.ROW_VERSION_CODE} >
        COALESCE(installed.${Schema.Installed.ROW_VERSION_CODE}, 0xffffffff) AND $signatureMatches)
        AS ${Schema.Synthetic.ROW_CAN_UPDATE}, product.${Schema.Product.ROW_COMPATIBLE},
        product.${Schema.Product.ROW_DATA_ITEM},"""

            if (searchQuery.isNotEmpty()) {
                builder += """(((product.${Schema.Product.ROW_NAME} LIKE ? OR
          product.${Schema.Product.ROW_SUMMARY} LIKE ?) * 7) |
          ((product.${Schema.Product.ROW_PACKAGE_NAME} LIKE ?) * 3) |
          (product.${Schema.Product.ROW_DESCRIPTION} LIKE ?)) AS ${Schema.Synthetic.ROW_MATCH_RANK},"""
                builder %= List(4) { "%$searchQuery%" }
            } else {
                builder += "0 AS ${Schema.Synthetic.ROW_MATCH_RANK},"
            }

            builder += """MAX((product.${Schema.Product.ROW_COMPATIBLE} AND
        (installed.${Schema.Installed.ROW_SIGNATURE} IS NULL OR $signatureMatches)) ||
        PRINTF('%016X', product.${Schema.Product.ROW_VERSION_CODE})) FROM ${Schema.Product.name} AS product"""
            builder += """JOIN ${Schema.Repository.name} AS repository
        ON product.${Schema.Product.ROW_REPOSITORY_ID} = repository.${Schema.Repository.ROW_ID}"""
            builder += """LEFT JOIN ${Schema.Lock.name} AS lock
        ON product.${Schema.Product.ROW_PACKAGE_NAME} = lock.${Schema.Lock.ROW_PACKAGE_NAME}"""

            if (!installed && !updates) {
                builder += "LEFT"
            }
            builder += """JOIN ${Schema.Installed.name} AS installed
        ON product.${Schema.Product.ROW_PACKAGE_NAME} = installed.${Schema.Installed.ROW_PACKAGE_NAME}"""

            if (section is ProductItem.Section.Category) {
                builder += """JOIN ${Schema.Category.name} AS category
          ON product.${Schema.Product.ROW_PACKAGE_NAME} = category.${Schema.Product.ROW_PACKAGE_NAME}"""
            }

            builder += """WHERE repository.${Schema.Repository.ROW_ENABLED} != 0 AND
        repository.${Schema.Repository.ROW_DELETED} == 0"""

            if (section is ProductItem.Section.Category) {
                builder += "AND category.${Schema.Category.ROW_NAME} = ?"
                builder %= section.name
            } else if (section is ProductItem.Section.Repository) {
                builder += "AND product.${Schema.Product.ROW_REPOSITORY_ID} = ?"
                builder %= section.id.toString()
            }

            if (searchQuery.isNotEmpty()) {
                builder += """AND ${Schema.Synthetic.ROW_MATCH_RANK} > 0"""
            }

            builder += "GROUP BY product.${Schema.Product.ROW_PACKAGE_NAME} HAVING 1"

            if (updates) {
                builder += "AND ${Schema.Synthetic.ROW_CAN_UPDATE}"
            }
            builder += "ORDER BY"

            if (searchQuery.isNotEmpty()) {
                builder += """${Schema.Synthetic.ROW_MATCH_RANK} DESC,"""
            }

            when (order) {
                SortOrder.UPDATED -> builder += "product.${Schema.Product.ROW_UPDATED} DESC,"
                SortOrder.ADDED -> builder += "product.${Schema.Product.ROW_ADDED} DESC,"
                SortOrder.NAME -> Unit
            }::class
            builder += "product.${Schema.Product.ROW_NAME} COLLATE LOCALIZED ASC"

            return builder.query(db, signal).observable(Subject.Products)
        }

        private fun transform(cursor: Cursor): Product {
            return cursor.getBlob(cursor.getColumnIndexOrThrow(Schema.Product.ROW_DATA))
                .jsonParse {
                    it.product().apply {
                        this.repositoryId = cursor
                            .getLong(cursor.getColumnIndexOrThrow(Schema.Product.ROW_REPOSITORY_ID))
                        this.description = cursor
                            .getString(cursor.getColumnIndexOrThrow(Schema.Product.ROW_DESCRIPTION))
                    }
                }
        }

        fun transformItem(cursor: Cursor): ProductItem {
            return cursor.getBlob(cursor.getColumnIndexOrThrow(Schema.Product.ROW_DATA_ITEM))
                .jsonParse {
                    it.productItem().apply {
                        this.repositoryId = cursor
                            .getLong(cursor.getColumnIndexOrThrow(Schema.Product.ROW_REPOSITORY_ID))
                        this.packageName = cursor
                            .getString(cursor.getColumnIndexOrThrow(Schema.Product.ROW_PACKAGE_NAME))
                        this.name = cursor
                            .getString(cursor.getColumnIndexOrThrow(Schema.Product.ROW_NAME))
                        this.summary = cursor
                            .getString(cursor.getColumnIndexOrThrow(Schema.Product.ROW_SUMMARY))
                        this.installedVersion = cursor
                            .getString(cursor.getColumnIndexOrThrow(Schema.Installed.ROW_VERSION))
                            .orEmpty()
                        this.compatible = cursor
                            .getInt(cursor.getColumnIndexOrThrow(Schema.Product.ROW_COMPATIBLE)) != 0
                        this.canUpdate = cursor
                            .getInt(cursor.getColumnIndexOrThrow(Schema.Synthetic.ROW_CAN_UPDATE)) != 0
                        this.matchRank = cursor
                            .getInt(cursor.getColumnIndexOrThrow(Schema.Synthetic.ROW_MATCH_RANK))
                    }
                }
        }
    }

    object CategoryAdapter {

        fun getAllStream(): Flow<Set<String>> = flowOf(Unit)
            .onCompletion { if (it == null) emitAll(flowCollection(Subject.Products)) }
            .map { getAll() }
            .flowOn(Dispatchers.IO)

        private suspend fun getAll(): Set<String> = withContext(Dispatchers.IO) {
            val builder = QueryBuilder()

            builder += """SELECT DISTINCT category.${Schema.Category.ROW_NAME}
        FROM ${Schema.Category.name} AS category
        JOIN ${Schema.Repository.name} AS repository
        ON category.${Schema.Category.ROW_REPOSITORY_ID} = repository.${Schema.Repository.ROW_ID}
        WHERE repository.${Schema.Repository.ROW_ENABLED} != 0 AND
        repository.${Schema.Repository.ROW_DELETED} == 0"""

            builder.query(db, null).use { cursor ->
                cursor.asSequence().map {
                    it.getString(it.getColumnIndexOrThrow(Schema.Category.ROW_NAME))
                }.toSet()
            }
        }
    }

    object InstalledAdapter {

        fun getStream(packageName: String): Flow<InstalledItem?> = flowOf(Unit)
            .onCompletion { if (it == null) emitAll(flowCollection(Subject.Products)) }
            .map { get(packageName, null) }
            .flowOn(Dispatchers.IO)

        fun get(packageName: String, signal: CancellationSignal?): InstalledItem? {
            return db.query(
                Schema.Installed.name,
                columns = arrayOf(
                    Schema.Installed.ROW_PACKAGE_NAME,
                    Schema.Installed.ROW_VERSION,
                    Schema.Installed.ROW_VERSION_CODE,
                    Schema.Installed.ROW_SIGNATURE
                ),
                selection = Pair("${Schema.Installed.ROW_PACKAGE_NAME} = ?", arrayOf(packageName)),
                signal = signal
            ).use { it.firstOrNull()?.let(::transform) }
        }

        private fun put(installedItem: InstalledItem, notify: Boolean) {
            db.insertOrReplace(
                true,
                Schema.Installed.name,
                ContentValues().apply {
                    put(Schema.Installed.ROW_PACKAGE_NAME, installedItem.packageName)
                    put(Schema.Installed.ROW_VERSION, installedItem.version)
                    put(Schema.Installed.ROW_VERSION_CODE, installedItem.versionCode)
                    put(Schema.Installed.ROW_SIGNATURE, installedItem.signature)
                }
            )
            if (notify) {
                notifyChanged(Subject.Products)
            }
        }

        fun put(installedItem: InstalledItem) = put(installedItem, true)

        fun putAll(installedItems: List<InstalledItem>) {
            db.transaction {
                db.delete(Schema.Installed.name, null, null)
                installedItems.forEach { put(it, false) }
            }
        }

        fun delete(packageName: String) {
            val count = db.delete(
                Schema.Installed.name,
                "${Schema.Installed.ROW_PACKAGE_NAME} = ?",
                arrayOf(packageName)
            )
            if (count > 0) {
                notifyChanged(Subject.Products)
            }
        }

        private fun transform(cursor: Cursor): InstalledItem {
            return InstalledItem(
                cursor.getString(cursor.getColumnIndexOrThrow(Schema.Installed.ROW_PACKAGE_NAME)),
                cursor.getString(cursor.getColumnIndexOrThrow(Schema.Installed.ROW_VERSION)),
                cursor.getLong(cursor.getColumnIndexOrThrow(Schema.Installed.ROW_VERSION_CODE)),
                cursor.getString(cursor.getColumnIndexOrThrow(Schema.Installed.ROW_SIGNATURE))
            )
        }
    }

    object LockAdapter {
        private fun put(lock: Pair<String, Long>, notify: Boolean) {
            db.insertOrReplace(
                true,
                Schema.Lock.name,
                ContentValues().apply {
                    put(Schema.Lock.ROW_PACKAGE_NAME, lock.first)
                    put(Schema.Lock.ROW_VERSION_CODE, lock.second)
                }
            )
            if (notify) {
                notifyChanged(Subject.Products)
            }
        }

        fun put(lock: Pair<String, Long>) = put(lock, true)

        fun putAll(locks: List<Pair<String, Long>>) {
            db.transaction {
                db.delete(Schema.Lock.name, null, null)
                locks.forEach { put(it, false) }
            }
        }

        fun delete(packageName: String) {
            db.delete(Schema.Lock.name, "${Schema.Lock.ROW_PACKAGE_NAME} = ?", arrayOf(packageName))
            notifyChanged(Subject.Products)
        }
    }

    object UpdaterAdapter {
        private val Table.temporaryName: String
            get() = "${name}_temporary"

        fun createTemporaryTable() {
            db.execSQL("DROP TABLE IF EXISTS ${Schema.Product.temporaryName}")
            db.execSQL("DROP TABLE IF EXISTS ${Schema.Category.temporaryName}")
            db.execSQL(Schema.Product.formatCreateTable(Schema.Product.temporaryName))
            db.execSQL(Schema.Category.formatCreateTable(Schema.Category.temporaryName))
        }

        fun putTemporary(products: List<Product>) {
            db.transaction {
                for (product in products) {
                    // Format signatures like ".signature1.signature2." for easier select
                    val signatures = product.signatures.joinToString { ".$it" }
                        .let { if (it.isNotEmpty()) "$it." else "" }
                    db.insertOrReplace(
                        true,
                        Schema.Product.temporaryName,
                        ContentValues().apply {
                            put(Schema.Product.ROW_REPOSITORY_ID, product.repositoryId)
                            put(Schema.Product.ROW_PACKAGE_NAME, product.packageName)
                            put(Schema.Product.ROW_NAME, product.name)
                            put(Schema.Product.ROW_SUMMARY, product.summary)
                            put(Schema.Product.ROW_DESCRIPTION, product.description)
                            put(Schema.Product.ROW_ADDED, product.added)
                            put(Schema.Product.ROW_UPDATED, product.updated)
                            put(Schema.Product.ROW_VERSION_CODE, product.versionCode)
                            put(Schema.Product.ROW_SIGNATURES, signatures)
                            put(Schema.Product.ROW_COMPATIBLE, if (product.compatible) 1 else 0)
                            put(Schema.Product.ROW_DATA, jsonGenerate(product::serialize))
                            put(
                                Schema.Product.ROW_DATA_ITEM,
                                jsonGenerate(product.item()::serialize)
                            )
                        }
                    )
                    for (category in product.categories) {
                        db.insertOrReplace(
                            true,
                            Schema.Category.temporaryName,
                            ContentValues().apply {
                                put(Schema.Category.ROW_REPOSITORY_ID, product.repositoryId)
                                put(Schema.Category.ROW_PACKAGE_NAME, product.packageName)
                                put(Schema.Category.ROW_NAME, category)
                            }
                        )
                    }
                }
            }
        }

        fun finishTemporary(repository: Repository, success: Boolean) {
            if (success) {
                db.transaction {
                    db.delete(
                        Schema.Product.name,
                        "${Schema.Product.ROW_REPOSITORY_ID} = ?",
                        arrayOf(repository.id.toString())
                    )
                    db.delete(
                        Schema.Category.name,
                        "${Schema.Category.ROW_REPOSITORY_ID} = ?",
                        arrayOf(repository.id.toString())
                    )
                    db.execSQL(
                        "INSERT INTO ${Schema.Product.name} SELECT * " +
                            "FROM ${Schema.Product.temporaryName}"
                    )
                    db.execSQL(
                        "INSERT INTO ${Schema.Category.name} SELECT * " +
                            "FROM ${Schema.Category.temporaryName}"
                    )
                    RepositoryAdapter.putWithoutNotification(repository, true, db)
                    db.execSQL("DROP TABLE IF EXISTS ${Schema.Product.temporaryName}")
                    db.execSQL("DROP TABLE IF EXISTS ${Schema.Category.temporaryName}")
                }
                notifyChanged(
                    Subject.Repositories,
                    Subject.Repository(repository.id),
                    Subject.Products
                )
            } else {
                db.execSQL("DROP TABLE IF EXISTS ${Schema.Product.temporaryName}")
                db.execSQL("DROP TABLE IF EXISTS ${Schema.Category.temporaryName}")
            }
        }
    }
}
