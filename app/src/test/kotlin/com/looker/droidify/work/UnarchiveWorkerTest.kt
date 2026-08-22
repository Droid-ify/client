package com.looker.droidify.work

import com.looker.droidify.model.Release
import com.looker.droidify.network.DataSize
import com.looker.droidify.service.DownloadService
import io.mockk.mockk
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.yield
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class UnarchiveWorkerTest {

    private val release = mockk<Release>()

    @Test
    fun `waits through non-terminal states for requested package`() = runTest {
        val packageName = "com.example.app"
        val states = MutableStateFlow(DownloadService.DownloadState())
        val result = async { states.waitForCompletion(packageName) }

        yield()
        assertFalse(result.isCompleted)

        states.value = DownloadService.DownloadState(
            DownloadService.State.Connecting(packageName),
        )
        yield()
        assertFalse(result.isCompleted)

        states.value = DownloadService.DownloadState(
            DownloadService.State.Downloading(packageName, DataSize(1), DataSize(2)),
        )
        yield()
        assertFalse(result.isCompleted)

        val success = DownloadService.DownloadState(
            DownloadService.State.Success(packageName, release),
        )
        states.value = success

        assertEquals(success, result.await())
    }

    @Test
    fun `returns cached success without another emission`() = runTest {
        val packageName = "com.example.app"
        val success = DownloadService.DownloadState(
            DownloadService.State.Success(packageName, release),
        )
        val states = MutableStateFlow(success)

        assertEquals(success, states.waitForCompletion(packageName))
    }

    @Test
    fun `ignores terminal states for other packages`() = runTest {
        val packageName = "com.example.app"
        val states = MutableStateFlow(
            DownloadService.DownloadState(
                DownloadService.State.Success("com.example.other", release),
            ),
        )
        val result = async { states.waitForCompletion(packageName) }

        yield()
        assertFalse(result.isCompleted)

        val success = DownloadService.DownloadState(
            DownloadService.State.Success(packageName, release),
        )
        states.value = success

        assertEquals(success, result.await())
    }

    @Test
    fun `returns error for requested package`() = runTest {
        val packageName = "com.example.app"
        val error = DownloadService.DownloadState(
            DownloadService.State.Error(packageName),
        )
        val states = MutableStateFlow(error)

        assertEquals(error, states.waitForCompletion(packageName))
    }

    @Test
    fun `returns cancellation for requested package`() = runTest {
        val packageName = "com.example.app"
        val cancellation = DownloadService.DownloadState(
            DownloadService.State.Cancel(packageName),
        )
        val states = MutableStateFlow(cancellation)

        assertEquals(cancellation, states.waitForCompletion(packageName))
    }
}
