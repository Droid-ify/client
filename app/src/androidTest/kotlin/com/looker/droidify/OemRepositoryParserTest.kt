package com.looker.droidify

import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import androidx.test.platform.app.InstrumentationRegistry
import com.looker.droidify.index.OemRepositoryParser
import com.looker.droidify.sync.common.assets
import org.junit.Before
import org.junit.runner.RunWith
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import org.junit.Test

@RunWith(AndroidJUnit4::class)
@SmallTest
class OemRepositoryParserTest {

    private lateinit var context: Context

    @Before
    fun setup() {
        context = InstrumentationRegistry.getInstrumentation().targetContext
    }

    @Test
    fun parseFile() {
        val stream = assets("additional_repos.xml")
        val list = OemRepositoryParser.parse(stream)
        assertEquals(3, list.size)
        val listOfNames = list.map { it.name }
        assertContentEquals(listOfNames, listOf("SHIFT", "microG F-Droid repo", "IzzyOnDroid F-Droid Repo"))
    }
}
