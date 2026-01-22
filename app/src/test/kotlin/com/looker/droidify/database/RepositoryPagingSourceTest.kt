package com.looker.droidify.database

import android.os.Handler
import androidx.paging.PagingSource
import com.looker.droidify.model.Repository
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.kotlin.mock

class RepositoryPagingSourceTest {

    private class TestRepositoryCursor(private val repositories: List<Repository>) : RepositoryCursor {
        override var position = -1
            private set

        var closed = false
            private set

        override val count: Int
            get() = repositories.size

        override fun readItem(): Repository {
            return repositories[position]
        }

        override fun moveToPosition(position: Int): Boolean {
            return if (-1 <= position && position < repositories.size) {
                this.position = position
                true
            } else {
                false
            }
        }

        override fun moveToNext(): Boolean {
            val newPos = position + 1
            return if (newPos < repositories.size) {
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

    private fun createPagingSource(
        repositories: List<Repository> = emptyList(),
        repositoryCursorFactory: CursorFactory<RepositoryCursor> = CursorFactory {
            TestRepositoryCursor(
                repositories
            )
        }
    ): RepositoryPagingSource {
        return RepositoryPagingSource(
            params = RepositoryPagingSource.Params(
                ioDispatcher = testDispatcher,
                mainHandler = mock()
            ),
            repositoryCursorFactory
        )
    }

    @Test
    fun `load returns page when items are loaded`() = runTest(testDispatcher) {
        val repositories = listOf(
            Repository.defaultRepository(
                address = "https://repo1.com",
                name = "Repo 1",
                description = "",
                enabled = true,
                fingerprint = ""
            ).copy(id = 1)
        )
        val pagingSource = createPagingSource(repositories = repositories)

        val expected = PagingSource.LoadResult.Page(
            data = repositories,
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
            data = emptyList(),
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
    fun cursorIsClosedOnInvalidate() {
        val cursor = TestRepositoryCursor(emptyList())
        val pagingSource = createPagingSource(repositoryCursorFactory = { cursor })
        pagingSource.invalidate()
        assertTrue(cursor.closed)
    }

    companion object {
        val testDispatcher = StandardTestDispatcher()
    }
}
