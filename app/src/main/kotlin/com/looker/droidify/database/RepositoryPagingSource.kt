package com.looker.droidify.database

import android.os.Handler
import androidx.annotation.VisibleForTesting
import androidx.paging.PagingSource
import com.looker.droidify.model.Repository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.ensureActive
import kotlin.math.min

fun createRepositoryPagingSource(
    mainHandler: Handler,
    ioDispatcher: CoroutineDispatcher,
): PagingSource<Int, Repository> {
    return RepositoryPagingSource(
        params = RepositoryPagingSource.Params(ioDispatcher, mainHandler),
        cursorFactory = {
            Database.RepositoryAdapter.query(null)
        }
    )
}

@VisibleForTesting
class RepositoryPagingSource(
    params: Params,
    cursorFactory: CursorFactory<RepositoryCursor>
) : BaseCursorPagingSource<Repository, RepositoryCursor>(params, cursorFactory) {

    class Params(
        override val ioDispatcher: CoroutineDispatcher,
        override val mainHandler: Handler,
    ): BaseCursorPagingSource.Params

    override suspend fun createRows(offset: Int, limit: Int): List<Repository> = coroutineScope {
        val targetPos = offset - 1
        if (cursor.position != targetPos && !cursor.moveToPosition(targetPos)) {
            return@coroutineScope emptyList()
        }

        val items = ArrayList<Repository>(min(cursor.count - offset, limit))

        while (cursor.moveToNext()) {
            ensureActive()
            items += cursor.readItem()
            if (items.size >= limit) {
                break
            }
        }

        return@coroutineScope items
    }
}
