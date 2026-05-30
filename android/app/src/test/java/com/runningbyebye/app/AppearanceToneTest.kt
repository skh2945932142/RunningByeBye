package com.runningbyebye.app

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AppearanceToneTest {
    @Test
    fun lightBackgroundPrefersDarkStatusBarIcons() {
        assertTrue(AppearanceTone.prefersDarkStatusBarIcons(0xFFF3F8FA.toInt()))
    }

    @Test
    fun darkBackgroundPrefersLightStatusBarIcons() {
        assertFalse(AppearanceTone.prefersDarkStatusBarIcons(0xFF091018.toInt()))
    }
}
