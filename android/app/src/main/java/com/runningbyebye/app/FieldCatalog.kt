package com.runningbyebye.app

import org.json.JSONArray
import org.json.JSONObject
import java.io.InputStream
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

data class FieldPoint(
    val longitude: Double,
    val latitude: Double,
)

data class FieldOption(
    val code: String,
    val name: String,
)

data class FieldSummary(
    val option: FieldOption,
    val points: List<FieldPoint>,
    val pointCount: Int,
    val distanceKm: Double,
)

object FieldCatalog {
    const val POINTS_ASSET_ROOT = "points"

    val options = listOf(
        FieldOption("T1001", "风华运动场"),
        FieldOption("T1005", "太极运动场"),
        FieldOption("T1014", "宁静苑"),
    )

    fun indexOf(code: String): Int {
        return options.indexOfFirst { it.code == code }.takeIf { it >= 0 } ?: 0
    }

    fun optionAt(index: Int): FieldOption {
        return options.getOrElse(index) { options.first() }
    }
}

object FieldPointParser {
    fun parse(json: String): List<FieldPoint> {
        val root = JSONObject(json)
        val data = root.optJSONArray("data") ?: JSONArray()
        return buildList {
            for (index in 0 until data.length()) {
                val item = data.optJSONObject(index) ?: continue
                val longitude = item.optString("longitude").toDoubleOrNull() ?: continue
                val latitude = item.optString("latitude").toDoubleOrNull() ?: continue
                add(FieldPoint(longitude = longitude, latitude = latitude))
            }
        }
    }

    fun summarize(option: FieldOption, openAsset: () -> InputStream?): FieldSummary {
        val points = try {
            openAsset()?.use { stream ->
                parse(stream.bufferedReader().readText())
            } ?: emptyList()
        } catch (_: Exception) {
            emptyList()
        }
        return FieldSummary(
            option = option,
            points = points.sampled(maxPoints = 220),
            pointCount = points.size,
            distanceKm = estimateDistanceKm(points),
        )
    }

    fun estimateDistanceKm(points: List<FieldPoint>): Double {
        if (points.size < 2) {
            return 0.0
        }
        return points.zipWithNext().sumOf { (from, to) -> haversineKm(from, to) }
    }

    private fun List<FieldPoint>.sampled(maxPoints: Int): List<FieldPoint> {
        if (size <= maxPoints) {
            return this
        }
        val step = size / maxPoints.toDouble()
        return List(maxPoints) { index -> this[(index * step).toInt().coerceAtMost(lastIndex)] }
    }

    private fun haversineKm(from: FieldPoint, to: FieldPoint): Double {
        val earthRadiusKm = 6371.0
        val dLat = Math.toRadians(to.latitude - from.latitude)
        val dLon = Math.toRadians(to.longitude - from.longitude)
        val lat1 = Math.toRadians(from.latitude)
        val lat2 = Math.toRadians(to.latitude)
        val a = sin(dLat / 2) * sin(dLat / 2) +
            cos(lat1) * cos(lat2) * sin(dLon / 2) * sin(dLon / 2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return earthRadiusKm * c
    }
}

class ProgressMilestoneTracker(
    private val milestones: List<Int> = listOf(25, 50, 75, 100),
) {
    private val triggered = mutableSetOf<Int>()

    fun hit(percent: Int): Int? {
        val milestone = milestones.firstOrNull { percent >= it && triggered.add(it) }
        return milestone
    }

    fun reset() {
        triggered.clear()
    }
}
