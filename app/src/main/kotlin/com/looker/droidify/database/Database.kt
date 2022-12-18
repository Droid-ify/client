package com.looker.droidify.database

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.os.CancellationSignal
import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.core.JsonParser
import com.looker.core.common.file.Json
import com.looker.core.common.file.parseDictionary
import com.looker.core.common.file.writeDictionary
import com.looker.core.datastore.model.SortOrder
import com.looker.core.model.InstalledItem
import com.looker.core.model.Product
import com.looker.core.model.ProductItem
import com.looker.core.model.Repository
import com.looker.droidify.utility.extension.android.asSequence
import com.looker.droidify.utility.extension.android.firstOrNull
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOn
import java.io.ByteArrayOutputStream

object Database {
	fun init(context: Context): Boolean {
		val helper = Helper(context)
		db = helper.writableDatabase
		if (helper.created) {
			for (repository in Repository.defaultRepositories) {
				RepositoryAdapter.put(repository)
			}
		}
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

	private class Helper(context: Context) : SQLiteOpenHelper(context, "droidify", null, 1) {
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
		val shouldRecreate = recreate || tables.any {
			val sql = db.query(
				"${it.databasePrefix}sqlite_master", columns = arrayOf("sql"),
				selection = Pair("type = ? AND name = ?", arrayOf("table", it.innerName))
			)
				.use { it.firstOrNull()?.getString(0) }.orEmpty()
			it.formatCreateTable(it.innerName) != sql
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
		val shouldVacuum = tables.map {
			val sqls = db.query(
				"${it.databasePrefix}sqlite_master", columns = arrayOf("name", "sql"),
				selection = Pair("type = ? AND tbl_name = ?", arrayOf("index", it.innerName))
			)
				.use {
					it.asSequence()
						.mapNotNull { it.getString(1)?.let { sql -> Pair(it.getString(0), sql) } }
						.toList()
				}
				.filter { !it.first.startsWith("sqlite_") }
			val createIndexes = it.createIndexPairFormatted?.let { listOf(it) }.orEmpty()
			createIndexes.map { it.first } != sqls.map { it.second } && run {
				for (name in sqls.map { it.first }) {
					db.execSQL("DROP INDEX IF EXISTS $name")
				}
				for (createIndexPair in createIndexes) {
					db.execSQL(createIndexPair.second)
				}
				!it.memory
			}
		}
		if (shouldVacuum.any { it } && !db.inTransaction()) {
			db.execSQL("VACUUM")
		}
	}

	private fun dropOldTables(db: SQLiteDatabase, vararg neededTables: Table) {
		val tables = db.query(
			"sqlite_master", columns = arrayOf("name"),
			selection = Pair("type = ?", arrayOf("table"))
		)
			.use { it.asSequence().mapNotNull { it.getString(0) }.toList() }
			.filter { !it.startsWith("sqlite_") && !it.startsWith("android_") }
			.toSet() - neededTables.mapNotNull { if (it.memory) null else it.name }
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
		object Repositories : Subject()
		data class Repository(val id: Long) : Subject()
		object Products : Subject()
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
		contentValues: ContentValues,
	): Long {
		return if (replace) replace(table, null, contentValues) else insert(
			table,
			null,
			contentValues
		)
	}

	private fun SQLiteDatabase.query(
		table: String, columns: Array<String>? = null,
		selection: Pair<String, Array<String>>? = null, orderBy: String? = null,
		signal: CancellationSignal? = null,
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
		// Done in put
		internal fun putWithoutNotification(repository: Repository, shouldReplace: Boolean): Long {
			return db.insertOrReplace(shouldReplace, Schema.Repository.name, ContentValues().apply {
				if (shouldReplace) {
					put(Schema.Repository.ROW_ID, repository.id)
				}
				put(Schema.Repository.ROW_ENABLED, if (repository.enabled) 1 else 0)
				put(Schema.Repository.ROW_DELETED, 0)
				put(Schema.Repository.ROW_DATA, jsonGenerate(repository::serialize))
			})
		}

		// Done
		fun put(repository: Repository): Repository {
			val shouldReplace = repository.id >= 0L
			val newId = putWithoutNotification(repository, shouldReplace)
			val id = if (shouldReplace) repository.id else newId
			notifyChanged(Subject.Repositories, Subject.Repository(id), Subject.Products)
			return if (newId != repository.id) repository.copy(id = newId) else repository
		}

		// Done
		fun get(id: Long): Repository? {
			return db.query(
				Schema.Repository.name,
				selection = Pair(
					"${Schema.Repository.ROW_ID} = ? AND ${Schema.Repository.ROW_DELETED} == 0",
					arrayOf(id.toString())
				)
			)
				.use { it.firstOrNull()?.let(::transform) }
		}

		// Done
		// MAYBE signal has to be considered
		fun getAll(signal: CancellationSignal?): List<Repository> {
			return db.query(
				Schema.Repository.name,
				selection = Pair("${Schema.Repository.ROW_DELETED} == 0", emptyArray()),
				signal = signal
			).use { it.asSequence().map(::transform).toList() }
		}

		// Done Pair<Long,Int> instead
		// MAYBE signal has to be considered
		fun getAllDisabledDeleted(signal: CancellationSignal?): Set<Pair<Long, Boolean>> {
			return db.query(
				Schema.Repository.name,
				columns = arrayOf(Schema.Repository.ROW_ID, Schema.Repository.ROW_DELETED),
				selection = Pair(
					"${Schema.Repository.ROW_ENABLED} == 0 OR ${Schema.Repository.ROW_DELETED} != 0",
					emptyArray()
				),
				signal = signal
			).use {
				it.asSequence().map {
					Pair(
						it.getLong(it.getColumnIndex(Schema.Repository.ROW_ID)),
						it.getInt(it.getColumnIndex(Schema.Repository.ROW_DELETED)) != 0
					)
				}.toSet()
			}
		}

		// Done
		fun markAsDeleted(id: Long) {
			db.update(Schema.Repository.name, ContentValues().apply {
				put(Schema.Repository.ROW_DELETED, 1)
			}, "${Schema.Repository.ROW_ID} = ?", arrayOf(id.toString()))
			notifyChanged(Subject.Repositories, Subject.Repository(id), Subject.Products)
		}

		// Done
		fun cleanup(pairs: Set<Pair<Long, Boolean>>) {
			val result = pairs.windowed(10, 10, true).map {
				val idsString = it.joinToString(separator = ", ") { it.first.toString() }
				val productsCount = db.delete(
					Schema.Product.name,
					"${Schema.Product.ROW_REPOSITORY_ID} IN ($idsString)", null
				)
				val categoriesCount = db.delete(
					Schema.Category.name,
					"${Schema.Category.ROW_REPOSITORY_ID} IN ($idsString)", null
				)
				val deleteIdsString = it.asSequence().filter { it.second }
					.joinToString(separator = ", ") { it.first.toString() }
				if (deleteIdsString.isNotEmpty()) {
					db.delete(
						Schema.Repository.name,
						"${Schema.Repository.ROW_ID} IN ($deleteIdsString)",
						null
					)
				}
				productsCount != 0 || categoriesCount != 0
			}
			if (result.any { it }) {
				notifyChanged(Subject.Products)
			}
		}

		// get the cursor in the specific table. Unnecessary with Room
		fun query(signal: CancellationSignal?): Cursor {
			return db.query(
				Schema.Repository.name,
				selection = Pair("${Schema.Repository.ROW_DELETED} == 0", emptyArray()),
				signal = signal
			).observable(Subject.Repositories)
		}

		// Unnecessary with Room
		fun transform(cursor: Cursor): Repository {
			return cursor.getBlob(cursor.getColumnIndex(Schema.Repository.ROW_DATA))
				.jsonParse {
					Repository.deserialize(it).apply {
						this.id = cursor.getLong(cursor.getColumnIndex(Schema.Repository.ROW_ID))
					}
				}
		}
	}

	object ProductAdapter {
		// Done
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

		// Done
		fun getCount(repositoryId: Long): Int {
			return db.query(
				Schema.Product.name, columns = arrayOf("COUNT (*)"),
				selection = Pair(
					"${Schema.Product.ROW_REPOSITORY_ID} = ?",
					arrayOf(repositoryId.toString())
				)
			)
				.use { it.firstOrNull()?.getInt(0) ?: 0 }
		}

		// Complex left to wiring phase
		fun query(
			installed: Boolean, updates: Boolean, searchQuery: String,
			section: ProductItem.Section, order: SortOrder, signal: CancellationSignal?,
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

		// Unnecessary with Room
		private fun transform(cursor: Cursor): Product {
			return cursor.getBlob(cursor.getColumnIndex(Schema.Product.ROW_DATA))
				.jsonParse {
					Product.deserialize(it).apply {
						this.repositoryId = cursor
							.getLong(cursor.getColumnIndex(Schema.Product.ROW_REPOSITORY_ID))
						this.description = cursor
							.getString(cursor.getColumnIndex(Schema.Product.ROW_DESCRIPTION))
					}
				}
		}

		// Unnecessary with Room
		fun transformItem(cursor: Cursor): ProductItem {
			return cursor.getBlob(cursor.getColumnIndex(Schema.Product.ROW_DATA_ITEM))
				.jsonParse {
					ProductItem.deserialize(it).apply {
						this.repositoryId = cursor
							.getLong(cursor.getColumnIndex(Schema.Product.ROW_REPOSITORY_ID))
						this.packageName = cursor
							.getString(cursor.getColumnIndex(Schema.Product.ROW_PACKAGE_NAME))
						this.name = cursor
							.getString(cursor.getColumnIndex(Schema.Product.ROW_NAME))
						this.summary = cursor
							.getString(cursor.getColumnIndex(Schema.Product.ROW_SUMMARY))
						this.installedVersion = cursor
							.getString(cursor.getColumnIndex(Schema.Installed.ROW_VERSION))
							.orEmpty()
						this.compatible = cursor
							.getInt(cursor.getColumnIndex(Schema.Product.ROW_COMPATIBLE)) != 0
						this.canUpdate = cursor
							.getInt(cursor.getColumnIndex(Schema.Synthetic.ROW_CAN_UPDATE)) != 0
						this.matchRank = cursor
							.getInt(cursor.getColumnIndex(Schema.Synthetic.ROW_MATCH_RANK))
					}
				}
		}
	}

	object CategoryAdapter {
		// Done
		fun getAll(signal: CancellationSignal?): Set<String> {
			val builder = QueryBuilder()

			builder += """SELECT DISTINCT category.${Schema.Category.ROW_NAME}
        FROM ${Schema.Category.name} AS category
        JOIN ${Schema.Repository.name} AS repository
        ON category.${Schema.Category.ROW_REPOSITORY_ID} = repository.${Schema.Repository.ROW_ID}
        WHERE repository.${Schema.Repository.ROW_ENABLED} != 0 AND
        repository.${Schema.Repository.ROW_DELETED} == 0"""

			return builder.query(db, signal).use {
				it.asSequence()
					.map { it.getString(it.getColumnIndex(Schema.Category.ROW_NAME)) }.toSet()
			}
		}
	}

	object InstalledAdapter {
		// Done
		fun get(packageName: String, signal: CancellationSignal?): InstalledItem? {
			return db.query(
				Schema.Installed.name,
				columns = arrayOf(
					Schema.Installed.ROW_PACKAGE_NAME, Schema.Installed.ROW_VERSION,
					Schema.Installed.ROW_VERSION_CODE, Schema.Installed.ROW_SIGNATURE
				),
				selection = Pair("${Schema.Installed.ROW_PACKAGE_NAME} = ?", arrayOf(packageName)),
				signal = signal
			).use { it.firstOrNull()?.let(::transform) }
		}

		// Done in insert
		private fun put(installedItem: InstalledItem, notify: Boolean) {
			db.insertOrReplace(true, Schema.Installed.name, ContentValues().apply {
				put(Schema.Installed.ROW_PACKAGE_NAME, installedItem.packageName)
				put(Schema.Installed.ROW_VERSION, installedItem.version)
				put(Schema.Installed.ROW_VERSION_CODE, installedItem.versionCode)
				put(Schema.Installed.ROW_SIGNATURE, installedItem.signature)
			})
			if (notify) {
				notifyChanged(Subject.Products)
			}
		}

		// Done in insert
		fun put(installedItem: InstalledItem) = put(installedItem, true)

		// Done in insert
		fun putAll(installedItems: List<InstalledItem>) {
			db.beginTransaction()
			try {
				db.delete(Schema.Installed.name, null, null)
				installedItems.forEach { put(it, false) }
				db.setTransactionSuccessful()
			} finally {
				db.endTransaction()
			}
		}

		// Done
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

		// Unnecessary with Room
		private fun transform(cursor: Cursor): InstalledItem {
			return InstalledItem(
				cursor.getString(cursor.getColumnIndex(Schema.Installed.ROW_PACKAGE_NAME)),
				cursor.getString(cursor.getColumnIndex(Schema.Installed.ROW_VERSION)),
				cursor.getLong(cursor.getColumnIndex(Schema.Installed.ROW_VERSION_CODE)),
				cursor.getString(cursor.getColumnIndex(Schema.Installed.ROW_SIGNATURE))
			)
		}
	}

	object LockAdapter {
		// Done in insert (Lock object instead of pair)
		private fun put(lock: Pair<String, Long>, notify: Boolean) {
			db.insertOrReplace(true, Schema.Lock.name, ContentValues().apply {
				put(Schema.Lock.ROW_PACKAGE_NAME, lock.first)
				put(Schema.Lock.ROW_VERSION_CODE, lock.second)
			})
			if (notify) {
				notifyChanged(Subject.Products)
			}
		}

		// Done in insert (Lock object instead of pair)
		fun put(lock: Pair<String, Long>) = put(lock, true)

		// Done in insert (Lock object instead of pair)
		fun putAll(locks: List<Pair<String, Long>>) {
			db.beginTransaction()
			try {
				db.delete(Schema.Lock.name, null, null)
				locks.forEach { put(it, false) }
				db.setTransactionSuccessful()
			} finally {
				db.endTransaction()
			}
		}

		// Done
		fun delete(packageName: String) {
			db.delete(Schema.Lock.name, "${Schema.Lock.ROW_PACKAGE_NAME} = ?", arrayOf(packageName))
			notifyChanged(Subject.Products)
		}
	}

	// TODO add temporary tables
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
			db.beginTransaction()
			try {
				for (product in products) {
					// Format signatures like ".signature1.signature2." for easier select
					val signatures = product.signatures.joinToString { ".$it" }
						.let { if (it.isNotEmpty()) "$it." else "" }
					db.insertOrReplace(true, Schema.Product.temporaryName, ContentValues().apply {
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
						put(Schema.Product.ROW_DATA_ITEM, jsonGenerate(product.item()::serialize))
					})
					for (category in product.categories) {
						db.insertOrReplace(
							true,
							Schema.Category.temporaryName,
							ContentValues().apply {
								put(Schema.Category.ROW_REPOSITORY_ID, product.repositoryId)
								put(Schema.Category.ROW_PACKAGE_NAME, product.packageName)
								put(Schema.Category.ROW_NAME, category)
							})
					}
				}
				db.setTransactionSuccessful()
			} finally {
				db.endTransaction()
			}
		}

		fun finishTemporary(repository: Repository, success: Boolean) {
			if (success) {
				db.beginTransaction()
				try {
					db.delete(
						Schema.Product.name, "${Schema.Product.ROW_REPOSITORY_ID} = ?",
						arrayOf(repository.id.toString())
					)
					db.delete(
						Schema.Category.name, "${Schema.Category.ROW_REPOSITORY_ID} = ?",
						arrayOf(repository.id.toString())
					)
					db.execSQL("INSERT INTO ${Schema.Product.name} SELECT * FROM ${Schema.Product.temporaryName}")
					db.execSQL("INSERT INTO ${Schema.Category.name} SELECT * FROM ${Schema.Category.temporaryName}")
					RepositoryAdapter.putWithoutNotification(repository, true)
					db.execSQL("DROP TABLE IF EXISTS ${Schema.Product.temporaryName}")
					db.execSQL("DROP TABLE IF EXISTS ${Schema.Category.temporaryName}")
					db.setTransactionSuccessful()
				} finally {
					db.endTransaction()
				}
				if (success) {
					notifyChanged(
						Subject.Repositories,
						Subject.Repository(repository.id),
						Subject.Products
					)
				}
			} else {
				db.execSQL("DROP TABLE IF EXISTS ${Schema.Product.temporaryName}")
				db.execSQL("DROP TABLE IF EXISTS ${Schema.Category.temporaryName}")
			}
		}
	}
}
