package com.runningbyebye.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FieldCatalogTest {
    @Test
    fun resolvesKnownAndUnknownFieldIndexes() {
        assertEquals(1, FieldCatalog.indexOf("T1005"))
        assertEquals(0, FieldCatalog.indexOf("unknown"))
        assertEquals("T1001", FieldCatalog.optionAt(99).code)
    }

    @Test
    fun parsesPointJsonAndIgnoresInvalidRows() {
        val points = FieldPointParser.parse(
            """
            {
              "data": [
                {"longitude": "106.1", "latitude": "29.1"},
                {"longitude": "", "latitude": "29.2"},
                {"longitude": "106.3", "latitude": "bad"}
              ]
            }
            """.trimIndent(),
        )

        assertEquals(1, points.size)
        assertEquals(106.1, points.first().longitude, 0.0001)
        assertEquals(29.1, points.first().latitude, 0.0001)
    }

    @Test
    fun estimatesDistanceForPoints() {
        val distance = FieldPointParser.estimateDistanceKm(
            listOf(
                FieldPoint(longitude = 106.0, latitude = 29.0),
                FieldPoint(longitude = 106.001, latitude = 29.0),
            ),
        )

        assertTrue(distance > 0.09)
        assertTrue(distance < 0.11)
    }

    @Test
    fun summarizesMissingAssetAsEmptyField() {
        val summary = FieldPointParser.summarize(FieldCatalog.options.first()) { null }

        assertEquals(0, summary.pointCount)
        assertEquals(0.0, summary.distanceKm, 0.0)
    }

    @Test
    fun summarizesOriginalPointCountEvenWhenTrackIsSampled() {
        val points = (0 until 300).joinToString(",") { index ->
            """{"longitude": "${106.0 + index * 0.00001}", "latitude": "29.0"}"""
        }
        val summary = FieldPointParser.summarize(FieldCatalog.options.first()) {
            """{"data": [$points]}""".byteInputStream()
        }

        assertEquals(300, summary.pointCount)
        assertEquals(220, summary.points.size)
    }
}
