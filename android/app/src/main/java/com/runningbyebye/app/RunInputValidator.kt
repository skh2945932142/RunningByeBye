package com.runningbyebye.app

data class ValidationResult(
    val isValid: Boolean,
    val value: String,
    val errorMessage: String? = null
)

object RunInputValidator {
    private const val DEFAULT_PACE = "0"
    private const val DEFAULT_INTERVAL_SECONDS = "20"
    private const val MIN_INTERVAL_SECONDS = 5

    fun resolvePace(rawValue: String?): ValidationResult {
        val value = rawValue?.trim().orEmpty()
        if (value.isEmpty()) {
            return ValidationResult(true, DEFAULT_PACE)
        }

        val pace = value.toDoubleOrNull()
        if (pace == null || pace < 0.0) {
            return ValidationResult(false, "", "配速请输入 0 或大于 0 的数字")
        }

        return ValidationResult(true, value)
    }

    fun resolveIntervalSeconds(rawValue: String?): ValidationResult {
        val value = rawValue?.trim().orEmpty()
        if (value.isEmpty()) {
            return ValidationResult(true, DEFAULT_INTERVAL_SECONDS)
        }

        val interval = value.toIntOrNull()
        if (interval == null) {
            return ValidationResult(false, "", "发包周期请输入整数秒")
        }
        if (interval == 0) {
            return ValidationResult(true, DEFAULT_INTERVAL_SECONDS)
        }
        if (interval < MIN_INTERVAL_SECONDS) {
            return ValidationResult(false, "", "发包周期不能低于 5 秒")
        }

        return ValidationResult(true, value)
    }
}
