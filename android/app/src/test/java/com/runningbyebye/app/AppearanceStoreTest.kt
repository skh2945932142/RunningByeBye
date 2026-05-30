package com.runningbyebye.app

import org.junit.Assert.assertEquals
import org.junit.Test

class AppearanceStoreTest {
    @Test
    fun emptyStoreReturnsDefaults() {
        val store = AppearanceStore(TestSharedPreferences())

        val config = store.load()

        assertEquals(AppearanceConfig(), config)
    }

    @Test
    fun savedAppearanceCanBeLoadedAgain() {
        val prefs = TestSharedPreferences()
        val store = AppearanceStore(prefs)

        store.save(
            AppearanceConfig(
                presetId = "midnight",
                customBackgroundUri = "content://media/external/images/media/42",
                blurStrength = 88,
                scrimStrength = 64,
                glassStrength = 91,
            ),
        )

        assertEquals(
            AppearanceConfig(
                presetId = "midnight",
                customBackgroundUri = "content://media/external/images/media/42",
                blurStrength = 88,
                scrimStrength = 64,
                glassStrength = 91,
            ),
            AppearanceStore(prefs).load(),
        )
    }

    @Test
    fun blankValuesAreNormalizedBeforeSaving() {
        val store = AppearanceStore(TestSharedPreferences())

        store.save(
            AppearanceConfig(
                presetId = "   ",
                customBackgroundUri = "   ",
                blurStrength = -20,
                scrimStrength = 255,
                glassStrength = -1,
            ),
        )

        assertEquals(
            AppearanceConfig(
                presetId = AppearanceConfig.DEFAULT_PRESET_ID,
                customBackgroundUri = "",
                blurStrength = AppearanceConfig.MIN_STRENGTH,
                scrimStrength = AppearanceConfig.MAX_STRENGTH,
                glassStrength = AppearanceConfig.MIN_STRENGTH,
            ),
            store.load(),
        )
    }
}
