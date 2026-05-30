package com.runningbyebye.app

data class AppearanceConfig(
    val presetId: String = DEFAULT_PRESET_ID,
    val customBackgroundUri: String = "",
    val blurStrength: Int = DEFAULT_BLUR_STRENGTH,
    val scrimStrength: Int = DEFAULT_SCRIM_STRENGTH,
    val glassStrength: Int = DEFAULT_GLASS_STRENGTH,
) {
    companion object {
        const val DEFAULT_PRESET_ID = "aurora"
        const val DEFAULT_BLUR_STRENGTH = 72
        const val DEFAULT_SCRIM_STRENGTH = 56
        const val DEFAULT_GLASS_STRENGTH = 76

        const val MIN_STRENGTH = 0
        const val MAX_STRENGTH = 100
    }
}
