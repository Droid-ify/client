package com.looker.droidify.database

import android.os.Handler
import androidx.annotation.VisibleForTesting
import androidx.collection.ArrayMap
import androidx.paging.PagingSource
import com.looker.droidify.datastore.model.SortOrder
import com.looker.droidify.model.ProductItem
import com.looker.droidify.model.Repository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.ensureActive
import kotlin.math.min

fun createProductPagingSource(
    installed: Boolean,
    updates: Boolean,
    skipSignatureCheck: Boolean,
    searchQuery: String,
    section: ProductItem.Section,
    sortOrder: SortOrder,
    repositories: List<Repository>,
    emptyText: String,
    ioDispatcher: CoroutineDispatcher,
    mainHandler: Handler,
    lastAccessedKeyCallback: LastAccessedKeyCallback,
): PagingSource<Int, AppListRow> {
    val params = ProductPagingSource.ProductPagingSourceParams(
        queryParams = AppListQueryParamsImpl(
            installed = installed,
            updates = updates,
            skipSignatureCheck = skipSignatureCheck,
            searchQuery = searchQuery,
            section = section,
            sortOrder = sortOrder,
        ),
        repositories = repositories,
        emptyText = emptyText,
        ioDispatcher = ioDispatcher,
        mainHandler = mainHandler,
        lastAccessedKeyCallback = lastAccessedKeyCallback,
    )

    return ProductPagingSource(
        params = params,
    ) {
        Database.ProductAdapter.query(params.queryParams)
    }
}

private class AppListQueryParamsImpl(
    override val installed: Boolean,
    override val updates: Boolean,
    override val skipSignatureCheck: Boolean,
    override val searchQuery: String,
    override val section: ProductItem.Section,
    override val sortOrder: SortOrder,
) : AppListQueryParams

fun interface LastAccessedKeyCallback {
    fun invoke(key: Int)
}

@VisibleForTesting
class ProductPagingSource(
    params: ProductPagingSourceParams,
    cursorFactory: CursorFactory<ProductItemCursor>,
) : BaseCursorPagingSource<AppListRow, ProductItemCursor>(
    params = params,
    cursorFactory = cursorFactory,
) {

    class ProductPagingSourceParams(
        @JvmField
        val queryParams: AppListQueryParams,
        @JvmField
        val repositories: List<Repository>,
        @JvmField
        val emptyText: String,
        @JvmField
        val lastAccessedKeyCallback: LastAccessedKeyCallback,
        override val ioDispatcher: CoroutineDispatcher,
        override val mainHandler: Handler,
    ): Params

    private val repositoriesMap: ArrayMap<Long, Repository> = run {
        val repositories = params.repositories
        ArrayMap<Long, Repository>(repositories.size).apply {
            repositories.forEach {
                put(it.id, it)
            }
        }
    }

    private val emptyText: String = params.emptyText

    private val updates: Boolean = params.queryParams.updates

    private val lastAccessedKeyCallback: LastAccessedKeyCallback = params.lastAccessedKeyCallback

    override suspend fun loadImpl(
        params: LoadParams<Int>,
    ): LoadResult<Int, AppListRow> {
        if (params is LoadParams.Prepend) {
            return LoadResult.Page(
                data = emptyList(),
                prevKey = null,
                nextKey = 0,
            )
        }

        val offset = params.key?.also { lastAccessedKeyCallback.invoke(it) } ?: 0
        val limit = params.loadSize

        val items = createRows(offset, limit)

        val prevKey = if (offset == 0) null else offset - limit
        if (params is LoadParams.Refresh && items.isEmpty()) {
            return LoadResult.Page(
                data = listOf(EmptyListRow(emptyText)),
                prevKey = prevKey,
                nextKey = null,
            )
        }

        val nextKey = kotlin.run {
            val n = offset + limit
            if (n < cursor.count) n else null
        }

        return LoadResult.Page(
            data = items,
            prevKey = prevKey,
            nextKey = nextKey,
        )
    }

    override suspend fun createRows(offset: Int, limit: Int): List<ProductRow> = coroutineScope {
        val targetPos = offset - 1
        if (cursor.position != targetPos && !cursor.moveToPosition(targetPos)) {
            return@coroutineScope emptyList()
        }

        val products = ArrayList<ProductRow>(min(cursor.count - offset, limit))

        while (cursor.moveToNext()) {
            ensureActive()
            val product = cursor.readItem()
            products.add(
                createProductRow(
                    productItem = product,
                    repository = repositoriesMap[product.repoId],
                    updates = updates
                )
            )

            if (products.size >= limit) {
                break
            }
        }

        return@coroutineScope products
    }
}
