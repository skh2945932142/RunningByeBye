package com.runningbyebye.app

object AppearanceTone {
    fun prefersDarkStatusBarIcons(backgroundColor: Int): Boolean {
        val r = ((backgroundColor shr 16) and 0xFF) / 255.0
        val g = ((backgroundColor shr 8) and 0xFF) / 255.0
        val b = (backgroundColor and 0xFF) / 255.0

        val luminance = 0.2126 * r + 0.7152 * g + 0.0722 * b
        return luminance >= 0.62
    }
}
