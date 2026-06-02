package com.runningbyebye.app

import org.junit.Assert.assertEquals
import org.junit.Test

class MileageFormatterTest {
    @Test
    fun formatsMileageWithThreeDecimalsByDefault() {
        assertEquals("0.000 km", MileageFormatter.formatKm(0.0))
        assertEquals("1.235 km", MileageFormatter.formatKm(1.23456))
    }

    @Test
    fun formatsMileageNumberForNotificationWithoutUnit() {
        assertEquals("2.346", MileageFormatter.formatKmValue(2.34567))
    }

    @Test
    fun formatsAnimatedMileageTargets() {
        assertEquals("0.420", MileageFormatter.formatKmValue(0.42))
        assertEquals("12.000", MileageFormatter.formatKmValue(12.0))
    }
}
