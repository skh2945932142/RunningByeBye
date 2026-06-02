package com.runningbyebye.app

object DashboardMotionSpec {
    fun clampProgress(progress: Int): Int {
        return progress.coerceIn(0, 100)
    }

    fun accentSweepDegrees(progress: Int): Float {
        return 54f + clampProgress(progress) / 100f * 240f
    }

    fun haloAlpha(active: Boolean, progress: Int): Float {
        val clamped = clampProgress(progress) / 100f
        val base = if (active) 0.36f else 0.16f
        val progressLift = if (active) 0.20f else 0.05f
        val cap = if (active) 0.62f else 0.26f
        return (base + clamped * progressLift).coerceAtMost(cap)
    }
}
