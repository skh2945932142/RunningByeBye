package com.runningbyebye.app

data class AppearancePreset(
    val id: String,
    val title: String,
    val backgroundStart: Int,
    val backgroundEnd: Int,
    val glowStart: Int,
    val glowEnd: Int,
    val accent: Int,
    val surfaceTint: Int,
)

object AppearancePresetCatalog {
    private val presets = listOf(
        AppearancePreset(
            id = "aurora",
            title = "极光",
            backgroundStart = 0xFF07111B.toInt(),
            backgroundEnd = 0xFF0D2733.toInt(),
            glowStart = 0xFF77D3FF.toInt(),
            glowEnd = 0xFF67E8B4.toInt(),
            accent = 0xFF77D3FF.toInt(),
            surfaceTint = 0xFFB8F0FF.toInt(),
        ),
        AppearancePreset(
            id = "midnight",
            title = "深夜",
            backgroundStart = 0xFF091017.toInt(),
            backgroundEnd = 0xFF132029.toInt(),
            glowStart = 0xFF55D3C0.toInt(),
            glowEnd = 0xFF76A9FF.toInt(),
            accent = 0xFF67E8B4.toInt(),
            surfaceTint = 0xFFB8FFF0.toInt(),
        ),
        AppearancePreset(
            id = "glacier",
            title = "冰川",
            backgroundStart = 0xFFEAF4FB.toInt(),
            backgroundEnd = 0xFFC6D9E8.toInt(),
            glowStart = 0xFF77BEEA.toInt(),
            glowEnd = 0xFF95D6F8.toInt(),
            accent = 0xFF0E5E89.toInt(),
            surfaceTint = 0xFFFFFFFF.toInt(),
        ),
        AppearancePreset(
            id = "graphite",
            title = "石墨",
            backgroundStart = 0xFF12171B.toInt(),
            backgroundEnd = 0xFF232B30.toInt(),
            glowStart = 0xFF85E3C1.toInt(),
            glowEnd = 0xFFB1D7E8.toInt(),
            accent = 0xFF85E3C1.toInt(),
            surfaceTint = 0xFFD7F5EB.toInt(),
        ),
        AppearancePreset(
            id = "sunset",
            title = "夕阳",
            backgroundStart = 0xFF2A1511.toInt(),
            backgroundEnd = 0xFF6B3324.toInt(),
            glowStart = 0xFFFFB36B.toInt(),
            glowEnd = 0xFFFF6F61.toInt(),
            accent = 0xFFFFB36B.toInt(),
            surfaceTint = 0xFFFFE0C2.toInt(),
        ),
        AppearancePreset(
            id = "sakura",
            title = "樱花",
            backgroundStart = 0xFFFFEEF4.toInt(),
            backgroundEnd = 0xFFE6F1F4.toInt(),
            glowStart = 0xFFFF8FB7.toInt(),
            glowEnd = 0xFF72C8D6.toInt(),
            accent = 0xFFC24572.toInt(),
            surfaceTint = 0xFFFFFFFF.toInt(),
        ),
        AppearancePreset(
            id = "night-contrast",
            title = "高对比",
            backgroundStart = 0xFF050708.toInt(),
            backgroundEnd = 0xFF11171A.toInt(),
            glowStart = 0xFFFFD84D.toInt(),
            glowEnd = 0xFF57E6FF.toInt(),
            accent = 0xFFFFD84D.toInt(),
            surfaceTint = 0xFFF8FBFF.toInt(),
        ),
    )

    fun all(): List<AppearancePreset> = presets

    fun defaultPreset(): AppearancePreset = presets.first()

    fun find(id: String): AppearancePreset {
        return presets.firstOrNull { it.id == id } ?: defaultPreset()
    }
}
