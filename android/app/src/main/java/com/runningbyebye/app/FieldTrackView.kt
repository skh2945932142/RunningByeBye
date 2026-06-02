package com.runningbyebye.app

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PathMeasure
import android.graphics.RectF
import android.os.Build
import android.util.AttributeSet
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import kotlin.math.max

class FieldTrackView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : View(context, attrs) {
    private val path = Path()
    private val revealPath = Path()
    private val pathMeasure = PathMeasure()
    private val bounds = RectF()
    private val trackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
        strokeWidth = context.dp(2.5f)
        color = Color.WHITE
    }
    private val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
        strokeWidth = context.dp(7f)
        color = Color.argb(46, 255, 255, 255)
    }

    private var points: List<FieldPoint> = emptyList()
    private var accentColor: Int = Color.WHITE
    private var revealProgress = 1f
    private var revealAnimator: ValueAnimator? = null

    fun setTrack(points: List<FieldPoint>, accentColor: Int) {
        this.points = points
        this.accentColor = accentColor
        trackPaint.color = accentColor
        revealProgress = 1f
        invalidate()
    }

    fun playReveal() {
        revealAnimator?.cancel()
        if (!animationsEnabled()) {
            revealProgress = 1f
            invalidate()
            return
        }
        revealProgress = 0f
        revealAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 680L
            interpolator = AccelerateDecelerateInterpolator()
            addUpdateListener {
                revealProgress = it.animatedValue as Float
                invalidate()
            }
            start()
        }
    }

    override fun onDetachedFromWindow() {
        revealAnimator?.cancel()
        revealAnimator = null
        super.onDetachedFromWindow()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (points.size < 2) {
            drawEmptyTrack(canvas)
            return
        }
        rebuildPath()
        val drawPath = revealedPath()
        canvas.drawPath(drawPath, glowPaint)
        canvas.drawPath(drawPath, trackPaint)
    }

    private fun drawEmptyTrack(canvas: Canvas) {
        trackPaint.color = accentColor
        val cy = height / 2f
        val start = paddingLeft + context.dp(8f)
        val end = width - paddingRight - context.dp(8f)
        canvas.drawLine(start, cy, end, cy, glowPaint)
        canvas.drawLine(start, cy, end, cy, trackPaint)
    }

    private fun rebuildPath() {
        val minLon = points.minOf { it.longitude }
        val maxLon = points.maxOf { it.longitude }
        val minLat = points.minOf { it.latitude }
        val maxLat = points.maxOf { it.latitude }
        val lonRange = max(0.000001, maxLon - minLon)
        val latRange = max(0.000001, maxLat - minLat)
        bounds.set(
            paddingLeft + context.dp(6f),
            paddingTop + context.dp(6f),
            width - paddingRight - context.dp(6f),
            height - paddingBottom - context.dp(6f),
        )

        path.reset()
        points.forEachIndexed { index, point ->
            val x = bounds.left + ((point.longitude - minLon) / lonRange).toFloat() * bounds.width()
            val normalizedLat = ((point.latitude - minLat) / latRange).toFloat()
            val y = bounds.bottom - normalizedLat * bounds.height()
            if (index == 0) {
                path.moveTo(x, y)
            } else {
                path.lineTo(x, y)
            }
        }
    }

    private fun revealedPath(): Path {
        if (revealProgress >= 0.995f) {
            return path
        }
        revealPath.reset()
        pathMeasure.setPath(path, false)
        pathMeasure.getSegment(0f, pathMeasure.length * revealProgress.coerceIn(0f, 1f), revealPath, true)
        return revealPath
    }

    private fun animationsEnabled(): Boolean {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.O || ValueAnimator.areAnimatorsEnabled()
    }

    private fun Context.dp(value: Float): Float {
        return value * resources.displayMetrics.density
    }
}
