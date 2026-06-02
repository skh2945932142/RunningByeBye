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
import android.view.animation.DecelerateInterpolator
import kotlin.math.min

class RunSummaryBadgeView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : View(context, attrs) {
    enum class State {
        SUCCESS,
        DANGER,
        NEUTRAL,
    }

    private val bounds = RectF()
    private val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeWidth = context.dp(10f)
        maskFilter = BlurMaskFilter(context.dp(6f), BlurMaskFilter.Blur.NORMAL)
    }
    private val ringPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeWidth = context.dp(3f)
    }
    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val markPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
        strokeWidth = context.dp(2.6f)
    }

    private var state = State.NEUTRAL
    private var accentColor = Color.rgb(103, 232, 180)
    private var dangerColor = Color.rgb(255, 122, 136)
    private var reveal = 1f
    private var animator: ValueAnimator? = null

    init {
        setLayerType(LAYER_TYPE_SOFTWARE, null)
    }

    fun setPalette(accent: Int, danger: Int) {
        accentColor = accent
        dangerColor = danger
        invalidate()
    }

    fun setBadgeState(state: State, animate: Boolean) {
        this.state = state
        animator?.cancel()
        if (!animate || !animationsEnabled()) {
            reveal = 1f
            invalidate()
            return
        }
        reveal = 0f
        animator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 560L
            interpolator = DecelerateInterpolator(1.8f)
            addUpdateListener {
                reveal = it.animatedValue as Float
                invalidate()
            }
            start()
        }
    }

    override fun onDetachedFromWindow() {
        animator?.cancel()
        animator = null
        super.onDetachedFromWindow()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val size = min(width, height).toFloat()
        if (size <= 0f) {
            return
        }
        val color = when (state) {
            State.SUCCESS -> accentColor
            State.DANGER -> dangerColor
            State.NEUTRAL -> Color.WHITE
        }
        val cx = width / 2f
        val cy = height / 2f
        val radius = size / 2f - context.dp(9f)
        bounds.set(cx - radius, cy - radius, cx + radius, cy + radius)

        fillPaint.color = withAlpha(color, if (state == State.NEUTRAL) 34 else 42)
        canvas.drawCircle(cx, cy, radius, fillPaint)

        glowPaint.color = withAlpha(color, 92)
        canvas.drawArc(bounds, -90f, 360f * reveal, false, glowPaint)
        ringPaint.color = withAlpha(color, 232)
        canvas.drawArc(bounds, -90f, 360f * reveal, false, ringPaint)

        markPaint.color = color
        markPaint.alpha = (255 * reveal).toInt().coerceIn(0, 255)
        when (state) {
            State.SUCCESS -> drawCheck(canvas, cx, cy)
            State.DANGER -> drawStop(canvas, cx, cy)
            State.NEUTRAL -> drawDot(canvas, cx, cy)
        }
        markPaint.alpha = 255
    }

    private fun drawCheck(canvas: Canvas, cx: Float, cy: Float) {
        val startX = cx - context.dp(9f)
        val startY = cy + context.dp(1f)
        canvas.drawLine(startX, startY, cx - context.dp(2f), cy + context.dp(8f), markPaint)
        canvas.drawLine(cx - context.dp(2f), cy + context.dp(8f), cx + context.dp(10f), cy - context.dp(8f), markPaint)
    }

    private fun drawStop(canvas: Canvas, cx: Float, cy: Float) {
        val half = context.dp(7f)
        canvas.drawLine(cx - half, cy - half, cx + half, cy + half, markPaint)
        canvas.drawLine(cx + half, cy - half, cx - half, cy + half, markPaint)
    }

    private fun drawDot(canvas: Canvas, cx: Float, cy: Float) {
        fillPaint.color = withAlpha(Color.WHITE, 210)
        canvas.drawCircle(cx, cy, context.dp(4f), fillPaint)
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
