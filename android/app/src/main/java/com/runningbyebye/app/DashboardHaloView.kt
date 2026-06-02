package com.runningbyebye.app

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.BlurMaskFilter
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.os.Build
import android.util.AttributeSet
import android.view.View
import android.view.animation.LinearInterpolator
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

class DashboardHaloView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : View(context, attrs) {
    private val ringBounds = RectF()
    private val trackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeWidth = context.dp(1.5f)
    }
    private val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeWidth = context.dp(12f)
        maskFilter = BlurMaskFilter(context.dp(7f), BlurMaskFilter.Blur.NORMAL)
    }
    private val accentPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeWidth = context.dp(3f)
    }
    private val tickPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeWidth = context.dp(1f)
    }

    private var progress = 0
    private var active = false
    private var danger = false
    private var phase = 0f
    private var accentColor = Color.WHITE
    private var trackColor = Color.argb(64, 255, 255, 255)
    private var dangerColor = Color.rgb(255, 122, 136)
    private var animator: ValueAnimator? = null

    init {
        setLayerType(LAYER_TYPE_SOFTWARE, null)
    }

    fun setPalette(accent: Int, track: Int, danger: Int) {
        accentColor = accent
        trackColor = track
        dangerColor = danger
        invalidate()
    }

    fun setHaloState(progress: Int, active: Boolean, danger: Boolean = false) {
        this.progress = DashboardMotionSpec.clampProgress(progress)
        this.active = active
        this.danger = danger
        if (active) {
            startSpin()
        } else {
            stopSpin()
        }
        invalidate()
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        if (active) {
            startSpin()
        }
    }

    override fun onDetachedFromWindow() {
        stopSpin()
        super.onDetachedFromWindow()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val size = min(width, height).toFloat()
        if (size <= 0f) {
            return
        }

        val inset = context.dp(10f)
        ringBounds.set(
            (width - size) / 2f + inset,
            (height - size) / 2f + inset,
            (width + size) / 2f - inset,
            (height + size) / 2f - inset,
        )

        val color = if (danger) dangerColor else accentColor
        val alpha = (DashboardMotionSpec.haloAlpha(active, progress) * 255).toInt()
        trackPaint.color = withAlpha(trackColor, if (active) 96 else 62)
        glowPaint.color = withAlpha(color, alpha)
        accentPaint.color = withAlpha(color, if (active) 230 else 168)
        tickPaint.color = withAlpha(Color.WHITE, if (active) 118 else 72)

        canvas.drawArc(ringBounds, 0f, 360f, false, trackPaint)
        val start = -90f + phase * 360f
        val sweep = DashboardMotionSpec.accentSweepDegrees(progress)
        canvas.drawArc(ringBounds, start, sweep, false, glowPaint)
        canvas.drawArc(ringBounds, start, sweep, false, accentPaint)
        canvas.drawArc(ringBounds, start + 190f, 34f + progress * 0.42f, false, accentPaint)
        drawTicks(canvas, color)
    }

    private fun drawTicks(canvas: Canvas, color: Int) {
        val cx = ringBounds.centerX()
        val cy = ringBounds.centerY()
        val outer = ringBounds.width() / 2f + context.dp(4f)
        val inner = outer - context.dp(4f)
        for (index in 0 until 24) {
            val angle = Math.toRadians((index * 15f + phase * 18f - 90f).toDouble())
            val tickAlpha = if (index % 3 == 0) 132 else 68
            tickPaint.color = withAlpha(color, tickAlpha)
            canvas.drawLine(
                cx + cos(angle).toFloat() * inner,
                cy + sin(angle).toFloat() * inner,
                cx + cos(angle).toFloat() * outer,
                cy + sin(angle).toFloat() * outer,
                tickPaint,
            )
        }
    }

    private fun startSpin() {
        if (!animationsEnabled() || animator?.isStarted == true) {
            return
        }
        animator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 5200L
            repeatCount = ValueAnimator.INFINITE
            interpolator = LinearInterpolator()
            addUpdateListener {
                phase = it.animatedValue as Float
                invalidate()
            }
            start()
        }
    }

    private fun stopSpin() {
        animator?.cancel()
        animator = null
        phase = 0f
    }

    private fun withAlpha(color: Int, alpha: Int): Int {
        return Color.argb(
            alpha.coerceIn(0, 255),
            Color.red(color),
            Color.green(color),
            Color.blue(color),
        )
    }

    private fun animationsEnabled(): Boolean {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.O || ValueAnimator.areAnimatorsEnabled()
    }

    private fun Context.dp(value: Float): Float {
        return value * resources.displayMetrics.density
    }
}
