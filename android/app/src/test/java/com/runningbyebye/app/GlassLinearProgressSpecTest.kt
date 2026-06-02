package com.runningbyebye.app

import org.junit.Assert.assertEquals
import org.junit.Test

class GlassLinearProgressSpecTest {
    @Test
    fun clampsProgressToDashboardRange() {
        assertEquals(0, GlassLinearProgressSpec.clampProgress(-18))
        assertEquals(42, GlassLinearProgressSpec.clampProgress(42))
        assertEquals(100, GlassLinearProgressSpec.clampProgress(160))
    }

    @Test
    fun convertsProgressToFillFraction() {
        assertEquals(0f, GlassLinearProgressSpec.fillFraction(-1), 0.001f)
        assertEquals(0.37f, GlassLinearProgressSpec.fillFraction(37), 0.001f)
        assertEquals(1f, GlassLinearProgressSpec.fillFraction(120), 0.001f)
    }

    @Test
    fun exposesStableAnimationDuration() {
        assertEquals(520L, GlassLinearProgressSpec.PROGRESS_ANIMATION_MS)
    }
}
