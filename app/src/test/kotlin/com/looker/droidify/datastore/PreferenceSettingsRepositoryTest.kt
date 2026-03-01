package com.looker.droidify.datastore

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import app.cash.turbine.test
import com.looker.droidify.datastore.model.AutoSync
import com.looker.droidify.datastore.model.SortOrder
import com.looker.droidify.datastore.model.Theme
import com.looker.droidify.utility.common.Exporter
import io.mockk.mockk
import java.io.File
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class PreferenceSettingsRepositoryTest {

    @get:Rule
    val tmpFolder = TemporaryFolder()

    private val testDispatcher = UnconfinedTestDispatcher()
    private val testScope = TestScope(testDispatcher)

    private lateinit var repository: PreferenceSettingsRepository
    private lateinit var exporter: Exporter<Settings>

    @Before
    fun setup() {
        exporter = mockk(relaxed = true)
        val dataStore = PreferenceDataStoreFactory.create(
            scope = testScope,
            produceFile = { File(tmpFolder.newFolder(), "test_settings.preferences_pb") }
        )
        repository = PreferenceSettingsRepository(dataStore, exporter)
    }

    @Test
    fun `getInitial returns default settings`() = runTest {
        val settings = repository.getInitial()
        assertEquals("system", settings.language)
        assertEquals(Theme.SYSTEM, settings.theme)
        assertEquals(AutoSync.WIFI_ONLY, settings.autoSync)
        assertEquals(SortOrder.UPDATED, settings.sortOrder)
        assertFalse(settings.dynamicTheme)
    }

    @Test
    fun `setTheme updates theme setting`() = runTest {
        repository.setTheme(Theme.DARK)
        val settings = repository.getInitial()
        assertEquals(Theme.DARK, settings.theme)
    }

    @Test
    fun `setLanguage updates language`() = runTest {
        repository.setLanguage("de")
        val settings = repository.getInitial()
        assertEquals("de", settings.language)
    }

    @Test
    fun `toggleFavourites adds and removes package`() = runTest {
        repository.toggleFavourites("com.example.app")
        var settings = repository.getInitial()
        assertTrue(settings.favouriteApps.contains("com.example.app"))

        repository.toggleFavourites("com.example.app")
        settings = repository.getInitial()
        assertFalse(settings.favouriteApps.contains("com.example.app"))
    }

    @Test
    fun `setRepoEnabled manages enabled repos`() = runTest {
        repository.setRepoEnabled(1, true)
        repository.setRepoEnabled(2, true)
        assertTrue(repository.isRepoEnabled(1))
        assertTrue(repository.isRepoEnabled(2))

        repository.setRepoEnabled(1, false)
        assertFalse(repository.isRepoEnabled(1))
        assertTrue(repository.isRepoEnabled(2))
    }

    @Test
    fun `getEnabledRepoIds emits updated ids`() = runTest {
        repository.setRepoEnabled(5, true)
        repository.setRepoEnabled(10, true)

        repository.getEnabledRepoIds().test {
            val ids = awaitItem()
            assertTrue(ids.contains(5))
            assertTrue(ids.contains(10))
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `setAutoSync updates auto sync setting`() = runTest {
        repository.setAutoSync(AutoSync.NEVER)
        val settings = repository.getInitial()
        assertEquals(AutoSync.NEVER, settings.autoSync)
    }

    @Test
    fun `setSortOrder updates sort order`() = runTest {
        repository.setSortOrder(SortOrder.NAME)
        val settings = repository.getInitial()
        assertEquals(SortOrder.NAME, settings.sortOrder)
    }

    @Test
    fun `setHomeScreenSwiping updates swiping setting`() = runTest {
        repository.setHomeScreenSwiping(false)
        val settings = repository.getInitial()
        assertFalse(settings.homeScreenSwiping)
    }

    @Test
    fun `setDeleteApkOnInstall updates setting`() = runTest {
        repository.setDeleteApkOnInstall(true)
        val settings = repository.getInitial()
        assertTrue(settings.deleteApkOnInstall)
    }
}
