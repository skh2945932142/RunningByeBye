package com.runningbyebye.app

import android.content.Context
import android.content.res.ColorStateList
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.RenderEffect
import android.graphics.Shader
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.LayerDrawable
import android.net.Uri
import android.os.Build
import android.view.View
import android.view.Window
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.core.view.WindowInsetsControllerCompat
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.progressindicator.CircularProgressIndicator
import com.google.android.material.textfield.TextInputLayout

data class AppearanceTargets(
    val root: View,
    val backgroundImage: ImageView,
    val backgroundScrim: View,
    val backgroundSheen: View,
    val appTitle: TextView,
    val appearanceButton: ImageButton,
    val cards: List<MaterialCardView>,
    val glassPanels: List<View>,
    val inputLayouts: List<TextInputLayout>,
    val primaryButtons: List<MaterialButton>,
    val secondaryButtons: List<MaterialButton>,
    val dangerButtons: List<MaterialButton>,
    val statusPill: TextView,
    val progressBar: GlassLinearProgressView,
    val circularProgress: CircularProgressIndicator,
    val dashboardHalo: DashboardHaloView,
    val summaryBadge: RunSummaryBadgeView,
)

class AppearanceApplier(
    private val context: Context,
    private val window: Window,
) {
    fun apply(targets: AppearanceTargets, config: AppearanceConfig) {
        val preset = AppearancePresetCatalog.find(config.presetId)
        val customBackgroundVisible = applyBackground(targets, preset, config)
        val backgroundIsLight = !customBackgroundVisible && AppearanceTone.prefersDarkStatusBarIcons(preset.backgroundStart)

        applyWindowBars(targets.root, preset, backgroundIsLight, customBackgroundVisible)
        applyHeader(targets, preset, backgroundIsLight, customBackgroundVisible)
        applySurfaces(targets, preset, config)
    }

    private fun applyBackground(
        targets: AppearanceTargets,
        preset: AppearancePreset,
        config: AppearanceConfig,
    ): Boolean {
        targets.root.background = createPresetBackground(preset)

        val hasCustomBackground = config.customBackgroundUri.isNotBlank()
        val customBackgroundVisible = if (hasCustomBackground) {
            try {
                val uri = Uri.parse(config.customBackgroundUri)
                val stream = context.contentResolver.openInputStream(uri) ?: error("background uri is not readable")
                stream.close()
                targets.backgroundImage.setImageURI(uri)
                targets.backgroundImage.visibility = View.VISIBLE
                true
            } catch (_: Exception) {
                targets.backgroundImage.setImageDrawable(null)
                targets.backgroundImage.visibility = View.GONE
                false
            }
        } else {
            targets.backgroundImage.setImageDrawable(null)
            targets.backgroundImage.visibility = View.GONE
            false
        }

        applyBlur(targets.backgroundImage, customBackgroundVisible, config.blurStrength)
        targets.backgroundScrim.setBackgroundColor(scrimColor(preset, config, customBackgroundVisible))
        targets.backgroundSheen.background = createBackgroundSheen(preset)
        targets.backgroundSheen.alpha = if (customBackgroundVisible) 0.22f else 0.34f
        return customBackgroundVisible
    }

    private fun createPresetBackground(preset: AppearancePreset): LayerDrawable {
        val base = GradientDrawable(
            GradientDrawable.Orientation.TOP_BOTTOM,
            intArrayOf(preset.backgroundStart, preset.backgroundEnd),
        )
        val glowA = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            gradientType = GradientDrawable.RADIAL_GRADIENT
            colors = intArrayOf(withAlpha(preset.glowStart, 88), Color.TRANSPARENT)
            setGradientCenter(0.18f, 0.08f)
            gradientRadius = context.dp(420).toFloat()
        }
        val glowB = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            gradientType = GradientDrawable.RADIAL_GRADIENT
            colors = intArrayOf(withAlpha(preset.glowEnd, 64), Color.TRANSPARENT)
            setGradientCenter(0.86f, 0.84f)
            gradientRadius = context.dp(360).toFloat()
        }
        return LayerDrawable(arrayOf(base, glowA, glowB))
    }

    private fun createBackgroundSheen(preset: AppearancePreset): GradientDrawable {
        return GradientDrawable(
            GradientDrawable.Orientation.TL_BR,
            intArrayOf(
                Color.TRANSPARENT,
                withAlpha(Color.WHITE, 34),
                withAlpha(preset.accent, 24),
                Color.TRANSPARENT,
            ),
        ).apply {
            shape = GradientDrawable.RECTANGLE
        }
    }

    private fun applyBlur(imageView: ImageView, enabled: Boolean, strength: Int) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            return
        }
        if (!enabled || strength <= 0) {
            imageView.setRenderEffect(null)
            return
        }
        val radius = (strength / 100f) * 28f
        imageView.setRenderEffect(
            RenderEffect.createBlurEffect(radius, radius, Shader.TileMode.CLAMP),
        )
    }

    private fun scrimColor(
        preset: AppearancePreset,
        config: AppearanceConfig,
        customBackgroundVisible: Boolean,
    ): Int {
        val alpha = ((config.scrimStrength / 100f) * 190).toInt().coerceIn(0, 190)
        val presetIsLight = AppearanceTone.prefersDarkStatusBarIcons(preset.backgroundStart)
        return if (!customBackgroundVisible && presetIsLight) {
            withAlpha(Color.WHITE, (alpha * 0.42f).toInt())
        } else {
            withAlpha(Color.BLACK, alpha)
        }
    }

    @Suppress("DEPRECATION")
    private fun applyWindowBars(
        root: View,
        preset: AppearancePreset,
        backgroundIsLight: Boolean,
        customBackgroundVisible: Boolean,
    ) {
        val useDarkIcons = backgroundIsLight && !customBackgroundVisible
        val controller = WindowInsetsControllerCompat(window, root)
        controller.isAppearanceLightStatusBars = useDarkIcons
        controller.isAppearanceLightNavigationBars = useDarkIcons
        window.statusBarColor = Color.TRANSPARENT
        window.navigationBarColor = withAlpha(preset.backgroundEnd, if (customBackgroundVisible) 210 else 128)
    }

    private fun applyHeader(
        targets: AppearanceTargets,
        preset: AppearancePreset,
        backgroundIsLight: Boolean,
        customBackgroundVisible: Boolean,
    ) {
        val primary = if (backgroundIsLight && !customBackgroundVisible) {
            Color.rgb(22, 37, 41)
        } else {
            Color.WHITE
        }
        targets.appTitle.setTextColor(primary)
        targets.appearanceButton.imageTintList = ColorStateList.valueOf(primary)
        targets.appearanceButton.background = roundedGlassSurface(
            fillColor = withAlpha(Color.WHITE, if (isNightMode()) 48 else 142),
            strokeColor = withAlpha(if (isNightMode()) Color.WHITE else preset.accent, 76),
            preset = preset,
            cornerDp = 8,
        )
        targets.statusPill.background = roundedGlassSurface(
            fillColor = withAlpha(if (isNightMode()) Color.WHITE else surfaceBaseColor(), if (isNightMode()) 42 else 156),
            strokeColor = withAlpha(preset.accent, 118),
            preset = preset,
            cornerDp = 16,
        )
        targets.statusPill.setTextColor(preset.accent)
    }

    private fun applySurfaces(
        targets: AppearanceTargets,
        preset: AppearancePreset,
        config: AppearanceConfig,
    ) {
        val surfaceColor = surfaceColor(config)
        val strokeColor = strokeColor(config, preset)
        val textColor = textColorForSurface()
        val accentTextColor = textColorForAccent(preset.accent)
        val secondaryButtonColor = withAlpha(surfaceBaseColor(), 176)
        val primaryButtonColor = withAlpha(preset.accent, 188)
        val dangerButtonColor = withAlpha(context.getColor(R.color.danger), 190)

        targets.cards.forEach { card ->
            card.setCardBackgroundColor(surfaceColor)
            card.setStrokeColor(strokeColor)
            card.strokeWidth = context.dp(1)
            card.cardElevation = context.dp(1).toFloat()
            card.foreground = glassForeground(preset, 8)
        }

        targets.glassPanels.forEach { panel ->
            panel.background = roundedGlassSurface(surfaceColor, strokeColor, preset, 8)
            panel.elevation = context.dp(1).toFloat()
        }

        val inputStroke = ColorStateList(
            arrayOf(
                intArrayOf(android.R.attr.state_focused),
                intArrayOf(-android.R.attr.state_enabled),
                intArrayOf(),
            ),
            intArrayOf(
                withAlpha(preset.accent, 232),
                withAlpha(textColor, 70),
                withAlpha(if (isNightMode()) Color.WHITE else preset.accent, 112),
            ),
        )
        targets.inputLayouts.forEach { input ->
            input.boxBackgroundColor = withAlpha(surfaceBaseColor(), if (isNightMode()) 38 else 118)
            input.setBoxStrokeColorStateList(inputStroke)
            input.defaultHintTextColor = ColorStateList.valueOf(withAlpha(textColor, 148))
            input.hintTextColor = ColorStateList.valueOf(preset.accent)
            input.setBoxCornerRadii(
                context.dp(8).toFloat(),
                context.dp(8).toFloat(),
                context.dp(8).toFloat(),
                context.dp(8).toFloat(),
            )
        }

        targets.primaryButtons.forEach { button ->
            button.backgroundTintList = ColorStateList.valueOf(primaryButtonColor)
            button.strokeColor = ColorStateList.valueOf(strokeColor)
            button.strokeWidth = context.dp(1)
            button.rippleColor = ColorStateList.valueOf(withAlpha(Color.WHITE, 58))
            button.setTextColor(accentTextColor)
            button.iconTint = ColorStateList.valueOf(accentTextColor)
        }

        targets.secondaryButtons.forEach { button ->
            button.backgroundTintList = ColorStateList.valueOf(secondaryButtonColor)
            button.strokeColor = ColorStateList.valueOf(strokeColor)
            button.strokeWidth = context.dp(1)
            button.rippleColor = ColorStateList.valueOf(withAlpha(preset.accent, 46))
            button.setTextColor(textColor)
            button.iconTint = ColorStateList.valueOf(textColor)
        }

        targets.dangerButtons.forEach { button ->
            button.backgroundTintList = ColorStateList.valueOf(dangerButtonColor)
            button.strokeColor = ColorStateList.valueOf(withAlpha(context.getColor(R.color.danger), 170))
            button.strokeWidth = context.dp(1)
            button.rippleColor = ColorStateList.valueOf(withAlpha(Color.WHITE, 54))
            button.setTextColor(Color.WHITE)
            button.iconTint = ColorStateList.valueOf(Color.WHITE)
        }

        targets.progressBar.setPalette(
            accent = preset.accent,
            track = withAlpha(preset.accent, 48),
        )
        targets.circularProgress.setIndicatorColor(preset.accent)
        targets.circularProgress.trackColor = withAlpha(preset.accent, 48)
        targets.dashboardHalo.setPalette(
            accent = preset.accent,
            track = withAlpha(preset.accent, 48),
            danger = context.getColor(R.color.danger),
        )
        targets.summaryBadge.setPalette(
            accent = preset.accent,
            danger = context.getColor(R.color.danger),
        )
    }

    private fun surfaceColor(config: AppearanceConfig): Int {
        val alpha = (136 + config.glassStrength).coerceIn(136, 228)
        return withAlpha(surfaceBaseColor(), alpha)
    }

    private fun strokeColor(config: AppearanceConfig, preset: AppearancePreset): Int {
        val alpha = (52 + config.glassStrength / 2).coerceIn(52, 110)
        return withAlpha(if (isNightMode()) Color.WHITE else preset.accent, alpha)
    }

    private fun roundedGlassSurface(
        fillColor: Int,
        strokeColor: Int,
        preset: AppearancePreset,
        cornerDp: Int,
    ): LayerDrawable {
        val radius = context.dp(cornerDp).toFloat()
        val base = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = radius
            setColor(fillColor)
            setStroke(context.dp(1), strokeColor)
        }
        val topLight = GradientDrawable(
            GradientDrawable.Orientation.TOP_BOTTOM,
            intArrayOf(withAlpha(Color.WHITE, 58), withAlpha(Color.WHITE, 14), Color.TRANSPARENT),
        ).apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = radius
        }
        val edgeTint = GradientDrawable(
            GradientDrawable.Orientation.TL_BR,
            intArrayOf(withAlpha(Color.WHITE, 36), Color.TRANSPARENT, withAlpha(preset.accent, 34)),
        ).apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = radius
        }
        return LayerDrawable(arrayOf(base, edgeTint, topLight))
    }

    private fun glassForeground(preset: AppearancePreset, cornerDp: Int): LayerDrawable {
        val radius = context.dp(cornerDp).toFloat()
        val surfaceGlint = GradientDrawable(
            GradientDrawable.Orientation.TL_BR,
            intArrayOf(withAlpha(Color.WHITE, 48), Color.TRANSPARENT, withAlpha(preset.accent, 28)),
        ).apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = radius
        }
        val topEdge = GradientDrawable(
            GradientDrawable.Orientation.TOP_BOTTOM,
            intArrayOf(withAlpha(Color.WHITE, 42), Color.TRANSPARENT),
        ).apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = radius
        }
        return LayerDrawable(arrayOf(surfaceGlint, topEdge))
    }

    private fun surfaceBaseColor(): Int {
        return if (isNightMode()) {
            Color.rgb(17, 29, 34)
        } else {
            Color.WHITE
        }
    }

    private fun textColorForSurface(): Int {
        return if (isNightMode()) {
            Color.rgb(234, 245, 245)
        } else {
            Color.rgb(22, 37, 41)
        }
    }

    private fun textColorForAccent(accentColor: Int): Int {
        return if (AppearanceTone.prefersDarkStatusBarIcons(accentColor)) {
            Color.rgb(9, 23, 29)
        } else {
            Color.WHITE
        }
    }

    private fun isNightMode(): Boolean {
        val mode = context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
        return mode == Configuration.UI_MODE_NIGHT_YES
    }

    private fun withAlpha(color: Int, alpha: Int): Int {
        return Color.argb(
            alpha.coerceIn(0, 255),
            Color.red(color),
            Color.green(color),
            Color.blue(color),
        )
    }

    private fun Context.dp(value: Int): Int {
        return (value * resources.displayMetrics.density).toInt()
    }
}
