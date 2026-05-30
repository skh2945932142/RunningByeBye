package com.runningbyebye.app

import android.content.Context
import android.content.SharedPreferences

data class SavedRunOptions(
    val openID: String,
    val fieldCode: String,
    val pace: String,
    val intervalSeconds: String,
)

class RunOptionsStore(private val preferences: SharedPreferences) {
    fun load(): SavedRunOptions {
        return SavedRunOptions(
            openID = preferences.getString(KEY_OPEN_ID, "").orEmpty(),
            fieldCode = preferences.getString(KEY_FIELD_CODE, DEFAULT_FIELD_CODE).orDefault(DEFAULT_FIELD_CODE),
            pace = preferences.getString(KEY_PACE, DEFAULT_PACE).orDefault(DEFAULT_PACE),
            intervalSeconds = preferences.getString(KEY_INTERVAL_SECONDS, DEFAULT_INTERVAL_SECONDS).orDefault(DEFAULT_INTERVAL_SECONDS),
        )
    }

    fun save(options: SavedRunOptions) {
        val normalized = normalize(options)
        preferences.edit()
            .putString(KEY_OPEN_ID, normalized.openID)
            .putString(KEY_FIELD_CODE, normalized.fieldCode)
            .putString(KEY_PACE, normalized.pace)
            .putString(KEY_INTERVAL_SECONDS, normalized.intervalSeconds)
            .apply()
    }

    private fun normalize(options: SavedRunOptions): SavedRunOptions {
        return SavedRunOptions(
            openID = options.openID.trim(),
            fieldCode = options.fieldCode.orDefault(DEFAULT_FIELD_CODE),
            pace = options.pace.orDefault(DEFAULT_PACE),
            intervalSeconds = options.intervalSeconds.orDefault(DEFAULT_INTERVAL_SECONDS),
        )
    }

    private fun String?.orDefault(defaultValue: String): String {
        val value = this?.trim().orEmpty()
        return value.ifEmpty { defaultValue }
    }

    companion object {
        private const val PREFERENCES_NAME = "run_options"
        private const val KEY_OPEN_ID = "open_id"
        private const val KEY_FIELD_CODE = "field_code"
        private const val KEY_PACE = "pace"
        private const val KEY_INTERVAL_SECONDS = "interval_seconds"

        const val DEFAULT_FIELD_CODE = "T1001"
        const val DEFAULT_PACE = "0"
        const val DEFAULT_INTERVAL_SECONDS = "20"

        fun from(context: Context): RunOptionsStore {
            return RunOptionsStore(
                context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE),
            )
        }
    }
}
