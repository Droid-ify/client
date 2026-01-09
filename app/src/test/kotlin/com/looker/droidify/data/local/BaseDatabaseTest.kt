package com.looker.droidify.data.local

import android.content.Context
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.rules.TestRule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Base class for Room database tests.
 * Sets up an in-memory database for testing using Robolectric.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
abstract class BaseDatabaseTest {

    // Rule to make LiveData execute synchronously
    @get:Rule
    val instantTaskExecutorRule: TestRule = InstantTaskExecutorRule()

    // In-memory database for testing
    protected lateinit var database: DroidifyDatabase

    // Test dispatcher for coroutines
    protected val testDispatcher: TestDispatcher = StandardTestDispatcher()

    @Before
    fun setupDatabase() {
        // Set the main dispatcher to the test dispatcher
        Dispatchers.setMain(testDispatcher)

        // Get the application context from Robolectric
        val context = ApplicationProvider.getApplicationContext<Context>()

        // Create an in-memory database for testing
        database = Room.inMemoryDatabaseBuilder(
            context,
            DroidifyDatabase::class.java
        )
            .allowMainThreadQueries() // Allow queries on the main thread for testing
            .build()

        // Initialize DAOs in subclasses
        initDao()
    }

    /**
     * Hook method for subclasses to initialize their DAOs.
     * This is called after the database is created.
     */
    protected open fun initDao() {
        // No-op by default, subclasses can override
    }

    @After
    fun tearDown() {
        // Close the database
        database.close()

        // Reset the main dispatcher
        Dispatchers.resetMain()
    }

    /**
     * Helper function to run a test with the test dispatcher
     */
    fun runDbTest(block: suspend () -> Unit) = runTest(testDispatcher) {
        block()
    }
}
