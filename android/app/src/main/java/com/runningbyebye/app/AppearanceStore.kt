package com.runningbyebye.app

import android.content.Context
import android.content.SharedPreferences

class AppearanceStore(private val preferences: SharedPreferences) {
    fun load(): AppearanceConfig {
        return AppearanceConfig(
            presetId = preferences.getString(KEY_PRESET_ID, AppearanceConfig.DEFAULT_PRESET_ID).orDefault(AppearanceConfig.DEFAULT_PRESET_ID),
            customBackgroundUri = preferences.getString(KEY_CUSTOM_BACKGROUND_URI, "").orEmpty().trim(),
            blurStrength = preferences.getString(KEY_BLUR_STRENGTH, AppearanceConfig.DEFAULT_BLUR_STRENGTH.toString()).orDefaultInt(AppearanceConfig.DEFAULT_BLUR_STRENGTH),
            scrimStrength = preferences.getString(KEY_SCRIM_STRENGTH, AppearanceConfig.DEFAULT_SCRIM_STRENGTH.toString()).orDefaultInt(AppearanceConfig.DEFAULT_SCRIM_STRENGTH),
            glassStrength = preferences.getString(KEY_GLASS_STRENGTH, AppearanceConfig.DEFAULT_GLASS_STRENGTH.toString()).orDefaultInt(AppearanceConfig.DEFAULT_GLASS_STRENGTH),
        )
    }

    fun save(config: AppearanceConfig) {
        val normalized = normalize(config)
        preferences.edit()
            .putString(KEY_PRESET_ID, normalized.presetId)
            .putString(KEY_CUSTOM_BACKGROUND_URI, normalized.customBackgroundUri)
            .putString(KEY_BLUR_STRENGTH, normalized.blurStrength.toString())
            .putString(KEY_SCRIM_STRENGTH, normalized.scrimStrength.toString())
            .putString(KEY_GLASS_STRENGTH, normalized.glassStrength.toString())
            .apply()
    }

    private fun normalize(config: AppearanceConfig): AppearanceConfig {
        return AppearanceConfig(
            presetId = config.presetId.orDefault(AppearanceConfig.DEFAULT_PRESET_ID),
            customBackgroundUri = config.customBackgroundUri.trim(),
            blurStrength = config.blurStrength.clamp(),
            scrimStrength = config.scrimStrength.clamp(),
            glassStrength = config.glassStrength.clamp(),
        )
    }

    private fun Int.clamp(): Int {
        return coerceIn(AppearanceConfig.MIN_STRENGTH, AppearanceConfig.MAX_STRENGTH)
    }

    private fun String?.orDefault(defaultValue: String): String {
        val value = this?.trim().orEmpty()
        return value.ifEmpty { defaultValue }
    }

    private fun String?.orDefaultInt(defaultValue: Int): Int {
        return this?.trim()?.toIntOrNull()?.clamp() ?: defaultValue
    }

    companion object {
        private const val PREFERENCES_NAME = "appearance"
        private const val KEY_PRESET_ID = "preset_id"
        private const val KEY_CUSTOM_BACKGROUND_URI = "custom_background_uri"
        private const val KEY_BLUR_STRENGTH = "blur_strength"
        private const val KEY_SCRIM_STRENGTH = "scrim_strength"
        private const val KEY_GLASS_STRENGTH = "glass_strength"

        fun from(context: Context): AppearanceStore {
            return AppearanceStore(
                context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE),
            )
        }
    }
}
