package com.runningbyebye.app

import java.util.Locale

object MileageFormatter {
    fun formatKm(mileage: Double): String {
        return "${formatKmValue(mileage)} km"
    }

    fun formatKmValue(mileage: Double): String {
        return String.format(Locale.US, "%.3f", mileage)
    }
}
