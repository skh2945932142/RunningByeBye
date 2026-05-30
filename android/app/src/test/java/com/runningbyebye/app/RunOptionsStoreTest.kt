package com.runningbyebye.app

import org.junit.Assert.assertEquals
import org.junit.Test

class RunOptionsStoreTest {
    @Test
    fun emptyStoreReturnsDefaults() {
        val store = RunOptionsStore(TestSharedPreferences())

        val options = store.load()

        assertEquals("", options.openID)
        assertEquals("T1001", options.fieldCode)
        assertEquals("0", options.pace)
        assertEquals("20", options.intervalSeconds)
    }

    @Test
    fun savedOptionsCanBeLoadedAgain() {
        val prefs = TestSharedPreferences()
        val store = RunOptionsStore(prefs)

        store.save(
            SavedRunOptions(
                openID = "openid-123",
                fieldCode = "T1014",
                pace = "6.5",
                intervalSeconds = "20",
            ),
        )

        assertEquals(
            SavedRunOptions(
                openID = "openid-123",
                fieldCode = "T1014",
                pace = "6.5",
                intervalSeconds = "20",
            ),
            RunOptionsStore(prefs).load(),
        )
    }

    @Test
    fun blankValuesAreNormalizedBeforeSaving() {
        val store = RunOptionsStore(TestSharedPreferences())

        store.save(
            SavedRunOptions(
                openID = "  openid-456  ",
                fieldCode = "",
                pace = "",
                intervalSeconds = "",
            ),
        )

        assertEquals(
            SavedRunOptions(
                openID = "openid-456",
                fieldCode = "T1001",
                pace = "0",
                intervalSeconds = "20",
            ),
            store.load(),
        )
    }
}
