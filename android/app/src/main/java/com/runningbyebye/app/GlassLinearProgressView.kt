package com.runningbyebye.app

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.BlurMaskFilter
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import android.os.Build
import android.util.AttributeSet
import android.view.View
import android.view.animation.DecelerateInterpolator
import kotlin.math.max

class GlassLinearProgressView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : View(context, attrs) {
    private val trackBounds = RectF()
    private val fillBounds = RectF()
    private val highlightBounds = RectF()
    private val trackPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        maskFilter = BlurMaskFilter(context.dp(4f), BlurMaskFilter.Blur.NORMAL)
    }
    private val highlightPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val sheenPaint = Paint(Paint.ANTI_ALIAS_FLAG)

    private var progressAnimator: ValueAnimator? = null
    private var sheenAnimator: ValueAnimator? = null
    private var accentColor = Color.rgb(103, 232, 180)
    private var trackColor = Color.argb(48, 103, 232, 180)
    private var progressValue = 0
    private var sheenPhase = 1.2f

    init {
        setLayerType(LAYER_TYPE_SOFTWARE, null)
    }

    var progress: Int
        get() = progressValue
        set(value) {
            setProgressValue(value, animate = false)
        }

    fun setPalette(accent: Int, track: Int) {
        accentColor = accent
        trackColor = track
        invalidate()
    }

    fun setProgressAnimated(target: Int) {
        setProgressValue(target, animate = true)
    }

    fun setProgressValue(target: Int, animate: Boolean) {
        val clampedTarget = GlassLinearProgressSpec.clampProgress(target)
        progressAnimator?.cancel()
        if (!animate || !animationsEnabled()) {
            progressValue = clampedTarget
            invalidate()
            return
        }
        val start = progressValue
        progressAnimator = ValueAnimator.ofInt(start, clampedTarget).apply {
            duration = GlassLinearProgressSpec.PROGRESS_ANIMATION_MS
            interpolator = DecelerateInterpolator(1.8f)
            addUpdateListener {
                progressValue = it.animatedValue as Int
                invalidate()
            }
            start()
        }
        playSheen()
    }

    fun playSheen() {
        sheenAnimator?.cancel()
        if (!animationsEnabled()) {
            sheenPhase = 1.2f
            invalidate()
            return
        }
        sheenAnimator = ValueAnimator.ofFloat(-0.35f, 1.2f).apply {
            duration = GlassLinearProgressSpec.SHEEN_ANIMATION_MS
            interpolator = DecelerateInterpolator(1.7f)
            addUpdateListener {
                sheenPhase = it.animatedValue as Float
                invalidate()
            }
            start()
        }
    }

    override fun onDetachedFromWindow() {
        progressAnimator?.cancel()
        sheenAnimator?.cancel()
        progressAnimator = null
        sheenAnimator = null
        super.onDetachedFromWindow()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (width <= 0 || height <= 0) {
            return
        }

        val verticalInset = context.dp(2f)
        val horizontalInset = context.dp(1f)
        trackBounds.set(
            paddingLeft.toFloat() + horizontalInset,
            paddingTop.toFloat() + verticalInset,
            width - paddingRight.toFloat() - horizontalInset,
            height - paddingBottom.toFloat() - verticalInset,
        )
        if (trackBounds.width() <= 0f || trackBounds.height() <= 0f) {
            return
        }

        val radius = trackBounds.height() / 2f
        drawTrack(canvas, radius)
        drawFill(canvas, radius)
        drawHighlight(canvas, radius)
    }

    private fun drawTrack(canvas: Canvas, radius: Float) {
        trackPaint.shader = LinearGradient(
            trackBounds.left,
            trackBounds.top,
            trackBounds.right,
            trackBounds.bottom,
            intArrayOf(withAlpha(Color.WHITE, 56), trackColor, withAlpha(Color.BLACK, 20)),
            floatArrayOf(0f, 0.48f, 1f),
            Shader.TileMode.CLAMP,
        )
        canvas.drawRoundRect(trackBounds, radius, radius, trackPaint)
        trackPaint.shader = null
    }

    private fun drawFill(canvas: Canvas, radius: Float) {
        val fraction = GlassLinearProgressSpec.fillFraction(progressValue)
        if (fraction <= 0f) {
            return
        }
        val minFillWidth = if (fraction > 0f) trackBounds.height() else 0f
        val fillWidth = max(trackBounds.width() * fraction, minFillWidth).coerceAtMost(trackBounds.width())
        fillBounds.set(trackBounds.left, trackBounds.top, trackBounds.left + fillWidth, trackBounds.bottom)

        val endTint = blend(accentColor, Color.WHITE, 0.38f)
        glowPaint.color = withAlpha(accentColor, 84)
        canvas.drawRoundRect(fillBounds, radius, radius, glowPaint)

        fillPaint.shader = LinearGradient(
            fillBounds.left,
            fillBounds.top,
            fillBounds.right,
            fillBounds.bottom,
            intArrayOf(withAlpha(accentColor, 196), endTint, withAlpha(Color.WHITE, 210)),
            floatArrayOf(0f, 0.72f, 1f),
            Shader.TileMode.CLAMP,
        )
        canvas.drawRoundRect(fillBounds, radius, radius, fillPaint)
        fillPaint.shader = null

        drawSheen(canvas, radius)
    }

    private fun drawSheen(canvas: Canvas, radius: Float) {
        if (sheenPhase > 1f || fillBounds.width() <= context.dp(18f)) {
            return
        }
        val sheenWidth = context.dp(30f)
        val center = fillBounds.left + fillBounds.width() * sheenPhase
        val left = (center - sheenWidth / 2f).coerceAtLeast(fillBounds.left)
        val right = (center + sheenWidth / 2f).coerceAtMost(fillBounds.right)
        if (right <= left) {
            return
        }
        sheenPaint.shader = LinearGradient(
            left,
            fillBounds.top,
            right,
            fillBounds.bottom,
            intArrayOf(Color.TRANSPARENT, withAlpha(Color.WHITE, 140), Color.TRANSPARENT),
            floatArrayOf(0f, 0.5f, 1f),
            Shader.TileMode.CLAMP,
        )
        val save = canvas.save()
        canvas.clipRect(fillBounds)
        canvas.drawRoundRect(RectF(left, fillBounds.top, right, fillBounds.bottom), radius, radius, sheenPaint)
        canvas.restoreToCount(save)
        sheenPaint.shader = null
    }

    private fun drawHighlight(canvas: Canvas, radius: Float) {
        val topInset = context.dp(2f)
        val highlightHeight = context.dp(1.2f)
        highlightBounds.set(
            trackBounds.left + context.dp(4f),
            trackBounds.top + topInset,
            trackBounds.right - context.dp(4f),
            trackBounds.top + topInset + highlightHeight,
        )
        highlightPaint.color = withAlpha(Color.WHITE, 102)
        canvas.drawRoundRect(highlightBounds, radius, radius, highlightPaint)
    }

    private fun blend(from: Int, to: Int, ratio: Float): Int {
        val t = ratio.coerceIn(0f, 1f)
        return Color.rgb(
            (Color.red(from) + (Color.red(to) - Color.red(from)) * t).toInt(),
            (Color.green(from) + (Color.green(to) - Color.green(from)) * t).toInt(),
            (Color.blue(from) + (Color.blue(to) - Color.blue(from)) * t).toInt(),
        )
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
