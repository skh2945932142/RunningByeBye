package com.runningbyebye.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AppearancePresetCatalogTest {
    @Test
    fun defaultPresetIsAurora() {
        assertEquals("aurora", AppearancePresetCatalog.defaultPreset().id)
    }

    @Test
    fun unknownPresetFallsBackToDefault() {
        assertEquals(
            AppearancePresetCatalog.defaultPreset(),
            AppearancePresetCatalog.find("does-not-exist"),
        )
    }

    @Test
    fun presetCatalogContainsTheExpectedCoreOptions() {
        val ids = AppearancePresetCatalog.all().map { it.id }

        assertTrue(
            ids.containsAll(
                listOf(
                    "aurora",
                    "midnight",
                    "glacier",
                    "graphite",
                    "sunset",
                    "sakura",
                    "night-contrast",
                ),
            ),
        )
    }
}
