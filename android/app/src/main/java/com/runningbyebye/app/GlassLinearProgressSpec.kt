package com.runningbyebye.app

object GlassLinearProgressSpec {
    const val MAX_PROGRESS = 100
    const val PROGRESS_ANIMATION_MS = 520L
    const val SHEEN_ANIMATION_MS = 700L

    fun clampProgress(progress: Int): Int {
        return progress.coerceIn(0, MAX_PROGRESS)
    }

    fun fillFraction(progress: Int): Float {
        return clampProgress(progress) / MAX_PROGRESS.toFloat()
    }
}
