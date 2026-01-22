package com.looker.droidify.database

import android.os.Handler
import androidx.annotation.VisibleForTesting
import androidx.paging.PagingSource
import androidx.paging.PagingState
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext

@VisibleForTesting
fun interface CursorFactory<CursorType : DbCursor<*>> {
    fun create(): CursorType
}

abstract class BaseCursorPagingSource<ItemType : Any, CursorType : DbCursor<*>>(
    params: Params,
    cursorFactory: CursorFactory<CursorType>
) : PagingSource<Int, ItemType>() {

    interface Params {
        val mainHandler: Handler
        val ioDispatcher: CoroutineDispatcher
    }

    private val ioDispatcher: CoroutineDispatcher = params.ioDispatcher

    protected val cursor: CursorType by lazy {
        cursorFactory.create().also { cursor ->
            cursor.registerOnInvalidatedCallback(params.mainHandler) {
                invalidate()
            }
        }
    }

    init {
        registerInvalidatedCallback {
            cursor.close()
        }
    }

    abstract suspend fun createRows(offset: Int, limit: Int): List<ItemType>

    final override suspend fun load(params: LoadParams<Int>): LoadResult<Int, ItemType> =
        withContext(ioDispatcher) {
            loadImpl(params)
        }

    protected open suspend fun loadImpl(params: LoadParams<Int>): LoadResult<Int, ItemType> {
        if (params is LoadParams.Prepend) {
            return LoadResult.Page(
                data = emptyList(),
                prevKey = null,
                nextKey = 0
            )
        }

        val offset = params.key ?: 0
        val limit = params.loadSize

        val items = if (params is LoadParams.Refresh) {
            createRows(0, offset + limit)
        } else {
            createRows(offset, limit)
        }

        val nextKey = run {
            val n = offset + limit
            if (n < cursor.count) n else null
        }

        return LoadResult.Page(
            data = items,
            prevKey = if (offset == 0) null else offset - limit,
            nextKey = nextKey
        )
    }

    override fun getRefreshKey(state: PagingState<Int, ItemType>): Int? = state.anchorPosition
}
