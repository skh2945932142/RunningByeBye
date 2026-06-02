package com.runningbyebye.app

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import android.util.AttributeSet
import android.util.TypedValue
import android.view.View
import kotlin.math.min

class AppearancePresetCardView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : View(context, attrs) {
    private val bounds = RectF()
    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
    }
    private val topLightPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = android.graphics.Typeface.DEFAULT_BOLD
        textSize = context.sp(13f)
    }

    private var preset: AppearancePreset = AppearancePresetCatalog.defaultPreset()
    private var selected: Boolean = false

    fun bind(preset: AppearancePreset, selected: Boolean) {
        this.preset = preset
        this.selected = selected
        contentDescription = preset.title
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val radius = context.dp(8f)
        bounds.set(0f, 0f, width.toFloat(), height.toFloat())

        fillPaint.shader = android.graphics.LinearGradient(
            0f,
            0f,
            width.toFloat(),
            height.toFloat(),
            preset.backgroundStart,
            preset.backgroundEnd,
            Shader.TileMode.CLAMP,
        )
        canvas.drawRoundRect(bounds, radius, radius, fillPaint)

        glowPaint.shader = RadialGradient(
            width * 0.22f,
            height * 0.26f,
            min(width, height) * 0.78f,
            withAlpha(preset.glowStart, 160),
            Color.TRANSPARENT,
            Shader.TileMode.CLAMP,
        )
        canvas.drawRoundRect(bounds, radius, radius, glowPaint)

        glowPaint.shader = RadialGradient(
            width * 0.86f,
            height * 0.86f,
            min(width, height) * 0.72f,
            withAlpha(preset.glowEnd, 122),
            Color.TRANSPARENT,
            Shader.TileMode.CLAMP,
        )
        canvas.drawRoundRect(bounds, radius, radius, glowPaint)

        topLightPaint.shader = android.graphics.LinearGradient(
            0f,
            0f,
            0f,
            height.toFloat(),
            withAlpha(Color.WHITE, 86),
            Color.TRANSPARENT,
            Shader.TileMode.CLAMP,
        )
        canvas.drawRoundRect(bounds, radius, radius, topLightPaint)

        strokePaint.shader = null
        strokePaint.strokeWidth = context.dp(if (selected) 2.4f else 1.2f)
        strokePaint.color = withAlpha(if (selected) preset.accent else Color.WHITE, if (selected) 235 else 98)
        val strokeInset = strokePaint.strokeWidth / 2f
        bounds.inset(strokeInset, strokeInset)
        canvas.drawRoundRect(bounds, radius, radius, strokePaint)
        bounds.inset(-strokeInset, -strokeInset)

        val textColor = if (AppearanceTone.prefersDarkStatusBarIcons(preset.backgroundStart)) {
            Color.rgb(16, 28, 32)
        } else {
            Color.WHITE
        }
        textPaint.color = textColor
        val baseline = height - context.dp(14f)
        canvas.drawText(preset.title, context.dp(12f), baseline, textPaint)

        if (selected) {
            val dotRadius = context.dp(3.6f)
            val dotX = width - context.dp(14f)
            val dotY = context.dp(14f)
            fillPaint.shader = null
            fillPaint.color = withAlpha(Color.WHITE, 230)
            canvas.drawCircle(dotX, dotY, dotRadius + context.dp(2f), fillPaint)
            fillPaint.color = preset.accent
            canvas.drawCircle(dotX, dotY, dotRadius, fillPaint)
        }
    }

    private fun withAlpha(color: Int, alpha: Int): Int {
        return Color.argb(
            alpha.coerceIn(0, 255),
            Color.red(color),
            Color.green(color),
            Color.blue(color),
        )
    }

    private fun Context.dp(value: Float): Float {
        return value * resources.displayMetrics.density
    }

    private fun Context.sp(value: Float): Float {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, value, resources.displayMetrics)
    }
}
