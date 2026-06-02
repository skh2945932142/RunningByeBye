package com.runningbyebye.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DashboardMotionSpecTest {
    @Test
    fun clampsProgressForHaloCalculations() {
        assertEquals(0, DashboardMotionSpec.clampProgress(-12))
        assertEquals(48, DashboardMotionSpec.clampProgress(48))
        assertEquals(100, DashboardMotionSpec.clampProgress(140))
    }

    @Test
    fun increasesSweepWithProgress() {
        val low = DashboardMotionSpec.accentSweepDegrees(0)
        val mid = DashboardMotionSpec.accentSweepDegrees(50)
        val high = DashboardMotionSpec.accentSweepDegrees(100)

        assertTrue(low < mid)
        assertTrue(mid < high)
        assertEquals(54f, low, 0.01f)
        assertEquals(294f, high, 0.01f)
    }

    @Test
    fun activeHaloIsMoreVisibleThanIdleHalo() {
        val idle = DashboardMotionSpec.haloAlpha(active = false, progress = 80)
        val active = DashboardMotionSpec.haloAlpha(active = true, progress = 80)

        assertTrue(idle < active)
        assertTrue(active <= 0.62f)
    }
}
