package com.looker.droidify.database

import android.os.Handler
import androidx.paging.PagingSource
import com.looker.droidify.datastore.model.SortOrder
import com.looker.droidify.model.ProductItem
import com.looker.droidify.model.Repository
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.kotlin.mock

class ProductPagingSourceTest {

    private class TestProductCursor(private val products: List<ProductItem>) : ProductItemCursor {
        override var position = -1
            private set

        var closed = false
            private set

        override val count: Int
            get() = products.size

        override fun readItem(): ProductItem {
            return products[position]
        }

        override fun moveToPosition(position: Int): Boolean {
            return if (-1 <= position && position < products.size) {
                this.position = position
                true
            } else {
                false
            }
        }

        override fun moveToNext(): Boolean {
            val newPos = position + 1
            return if (newPos < products.size) {
                position = newPos
                true
            } else {
                false
            }
        }

        override fun close() {
            closed = true
        }

        override fun registerOnInvalidatedCallback(handler: Handler, callback: () -> Unit) {
            // ignore
        }
    }

    private class TestQueryParams(
        override val installed: Boolean,
        override val updates: Boolean,
        override val skipSignatureCheck: Boolean,
        override val searchQuery: String,
        override val section: ProductItem.Section,
        override val sortOrder: SortOrder
    ) : AppListQueryParams

    private fun createPagingSource(
        products: List<ProductItem> = emptyList(),
        repositories: List<Repository> = emptyList(),
        productCursorFactory: CursorFactory<ProductItemCursor> = CursorFactory {
            TestProductCursor(
                products
            )
        }
    ): ProductPagingSource {
        val queryParams = TestQueryParams(
            installed = false,
            updates = false,
            skipSignatureCheck = false,
            searchQuery = "",
            section = ProductItem.Section.All,
            sortOrder = SortOrder.NAME,
        )
        val params = ProductPagingSource.ProductPagingSourceParams(
            queryParams = queryParams,
            repositories = repositories,
            emptyText = "No apps found",
            ioDispatcher = testDispatcher,
            mainHandler = mock()
        )

        return ProductPagingSource(params, productCursorFactory)
    }

    @Test
    fun `load returns page when items are loaded`() = runTest(testDispatcher) {
        val products = listOf(
            ProductItem(
                repoId = 1,
                packageName = "com.example.app1",
                name = "App 1",
                summary = "Summary 1",
                icon = "icon1.png",
                metadataIcon = "",
                version = "1.0",
                installedVersion = "",
                compatible = true,
                canUpdate = false,
                matchRank = 0,
            )
        )
        val repositories = listOf(
            Repository.defaultRepository(
                address = "https://repo1.com",
                name = "Repo 1",
                description = "",
                enabled = true,
                fingerprint = ""
            ).copy(id = 1)
        )
        val pagingSource = createPagingSource(products = products, repositories = repositories)

        val expected = PagingSource.LoadResult.Page(
            data = listOf(
                createProductRow(
                    updates = false,
                    productItem = products[0],
                    repository = repositories[0],
                )
            ),
            prevKey = null,
            nextKey = null,
        )

        val actual = pagingSource.load(
            PagingSource.LoadParams.Refresh(
                key = 0,
                loadSize = 20,
                placeholdersEnabled = false
            )
        )

        assertEquals(expected, actual)
    }

    @Test
    fun `load returns empty page when no items are loaded`() = runTest(testDispatcher) {
        val pagingSource = createPagingSource()

        val expected = PagingSource.LoadResult.Page(
            data = listOf(EmptyListRow("No apps found")),
            prevKey = null,
            nextKey = null,
        )

        val actual = pagingSource.load(
            PagingSource.LoadParams.Refresh(
                key = 0,
                loadSize = 20,
                placeholdersEnabled = false
            )
        )

        assertEquals(expected, actual)
    }

    @Test
    fun `load returns second page when append is called`() = runTest(testDispatcher) {
        val products = (1..40).map {
            ProductItem(
                repoId = 1,
                packageName = "com.example.app$it",
                name = "App $it",
                summary = "Summary $it",
                icon = "icon$it.png",
                metadataIcon = "",
                version = "1.0",
                installedVersion = "",
                compatible = true,
                canUpdate = false,
                matchRank = 0,
            )
        }
        val repositories = listOf(
            Repository.defaultRepository(
                address = "https://repo1.com",
                name = "Repo 1",
                description = "",
                enabled = true,
                fingerprint = ""
            ).copy(id = 1)
        )
        val pagingSource = createPagingSource(products = products, repositories = repositories)

        val firstPage = pagingSource.load(
            PagingSource.LoadParams.Refresh(
                key = 0,
                loadSize = 20,
                placeholdersEnabled = false
            )
        ) as PagingSource.LoadResult.Page

        val expectedSecondPage = PagingSource.LoadResult.Page(
            data = products.subList(20, 40).map {
                createProductRow(
                    updates = false,
                    it,
                    repositories[0],
                )
            },
            prevKey = 0,
            nextKey = null,
        )

        val actualSecondPage = pagingSource.load(
            PagingSource.LoadParams.Append(
                key = firstPage.nextKey!!,
                loadSize = 20,
                placeholdersEnabled = false
            )
        )

        assertEquals(expectedSecondPage, actualSecondPage)
    }

    @Test
    fun cursorIsClosedOnInvalidate() {
        val cursor = TestProductCursor(emptyList())
        val pagingSource = createPagingSource(productCursorFactory = { cursor })
        pagingSource.invalidate()
        assertTrue(cursor.closed)
    }

    companion object {
        val testDispatcher = StandardTestDispatcher()
    }
}
