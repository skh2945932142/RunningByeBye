package com.runningbyebye.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RunInputValidatorTest {
    @Test
    fun blankPaceUsesRandomPace() {
        val result = RunInputValidator.resolvePace("")

        assertTrue(result.isValid)
        assertEquals("0", result.value)
    }

    @Test
    fun positiveDecimalPaceIsAccepted() {
        val result = RunInputValidator.resolvePace("6.5")

        assertTrue(result.isValid)
        assertEquals("6.5", result.value)
    }

    @Test
    fun invalidPaceIsRejected() {
        val result = RunInputValidator.resolvePace("fast")

        assertFalse(result.isValid)
        assertEquals("配速请输入 0 或大于 0 的数字", result.errorMessage)
    }

    @Test
    fun blankIntervalUsesDefaultTwentySeconds() {
        val result = RunInputValidator.resolveIntervalSeconds("")

        assertTrue(result.isValid)
        assertEquals("20", result.value)
    }

    @Test
    fun zeroIntervalUsesDefaultTwentySeconds() {
        val result = RunInputValidator.resolveIntervalSeconds("0")

        assertTrue(result.isValid)
        assertEquals("20", result.value)
    }

    @Test
    fun intervalBelowFiveSecondsIsRejected() {
        val result = RunInputValidator.resolveIntervalSeconds("4")

        assertFalse(result.isValid)
        assertEquals("发包周期不能低于 5 秒", result.errorMessage)
    }
}
