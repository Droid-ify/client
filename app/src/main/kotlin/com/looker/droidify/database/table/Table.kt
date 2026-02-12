package com.looker.droidify.database.table

import com.looker.droidify.database.QueryBuilder

interface Table {
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
        return buildString(128) {
            append("CREATE TABLE ")
            append(name)
            append(" (")
            append(QueryBuilder.trimQuery(createTable))
            append(")")
        }
    }

    val createIndexPairFormatted: Pair<String, String>?
        get() = createIndex?.let {
            Pair(
                "CREATE INDEX ${innerName}_index ON $innerName ($it)",
                "CREATE INDEX ${name}_index ON $innerName ($it)",
            )
        }
}
