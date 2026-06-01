package com.runningbyebye.app

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.DecelerateInterpolator
import android.view.animation.OvershootInterpolator
import android.widget.ProgressBar
import android.widget.ScrollView
import androidx.constraintlayout.motion.widget.MotionLayout

class GlassMotionController(private val context: Context) {
    enum class PressFeedbackStyle(
        val pressedScale: Float,
        val pressedAlpha: Float,
        val haptic: Boolean,
    ) {
        PRIMARY(0.965f, 0.9f, true),
        SECONDARY(0.972f, 0.92f, true),
        ICON(0.94f, 0.9f, true),
        PANEL(0.982f, 0.94f, false),
    }

    private var ambientAnimator: ObjectAnimator? = null
    private var statusPulse: AnimatorSet? = null
    private var statusPulseView: View? = null
    private var progressAnimator: ObjectAnimator? = null

    private val settleInterpolator = DecelerateInterpolator(1.7f)
    private val entranceInterpolator = OvershootInterpolator(0.68f)
    private val pulseInterpolator = AccelerateDecelerateInterpolator()

    fun startAmbientSheen(view: View) {
        ambientAnimator?.cancel()
        ambientAnimator = null
        val configuredAlpha = view.alpha.takeIf { it > 0f } ?: 0.34f
        view.visibility = View.VISIBLE
        view.alpha = configuredAlpha
        view.scaleX = 1.12f
        view.scaleY = 1.12f
        view.translationX = -context.dp(28).toFloat()

        if (!animationsEnabled()) {
            return
        }

        ambientAnimator = ObjectAnimator.ofFloat(
            view,
            View.TRANSLATION_X,
            -context.dp(28).toFloat(),
            context.dp(28).toFloat(),
        ).apply {
            duration = 7200L
            repeatCount = ValueAnimator.INFINITE
            repeatMode = ValueAnimator.REVERSE
            interpolator = pulseInterpolator
            start()
        }
    }

    fun stopAmbientSheen() {
        ambientAnimator?.cancel()
        ambientAnimator = null
    }

    fun playEntrance(views: List<View>) {
        if (!animationsEnabled()) {
            views.filter { it.visibility == View.VISIBLE }.forEach { view ->
                view.alpha = 1f
                view.translationY = 0f
                view.scaleX = 1f
                view.scaleY = 1f
            }
            return
        }

        views.filter { it.visibility == View.VISIBLE }.forEachIndexed { index, view ->
            view.alpha = 0f
            view.translationY = context.dp(20).toFloat()
            view.scaleX = 0.972f
            view.scaleY = 0.972f
            view.animate()
                .alpha(1f)
                .translationY(0f)
                .scaleX(1f)
                .scaleY(1f)
                .setStartDelay(index * 70L)
                .setDuration(420L)
                .setInterpolator(entranceInterpolator)
                .start()
        }
    }

    fun playSheetEntrance(sheet: View) {
        if (!animationsEnabled()) {
            sheet.alpha = 1f
            sheet.translationY = 0f
            sheet.scaleX = 1f
            sheet.scaleY = 1f
            return
        }

        sheet.alpha = 0f
        sheet.translationY = context.dp(30).toFloat()
        sheet.scaleX = 0.985f
        sheet.scaleY = 0.985f
        sheet.animate()
            .alpha(1f)
            .translationY(0f)
            .scaleX(1f)
            .scaleY(1f)
            .setDuration(360L)
            .setInterpolator(entranceInterpolator)
            .start()
    }

    fun bindHomeScrollMotion(
        scrollView: ScrollView,
        motionLayout: MotionLayout,
        headerBar: View,
        backgroundSheen: View,
        panels: List<View>,
    ) {
        if (!animationsEnabled()) {
            motionLayout.progress = 0f
            headerBar.translationY = 0f
            headerBar.scaleX = 1f
            headerBar.scaleY = 1f
            headerBar.alpha = 1f
            backgroundSheen.translationY = 0f
            panels.forEach { panel ->
                panel.translationY = 0f
                panel.scaleX = 1f
                panel.scaleY = 1f
            }
            return
        }

        scrollView.setOnScrollChangeListener { _, _, scrollY, _, _ ->
            applyHomeScrollMotion(scrollY, motionLayout, headerBar, backgroundSheen, panels)
        }
        scrollView.post {
            applyHomeScrollMotion(scrollView.scrollY, motionLayout, headerBar, backgroundSheen, panels)
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    fun bindPressFeedback(vararg views: View) {
        bindPressFeedback(PressFeedbackStyle.SECONDARY, *views)
    }

    @SuppressLint("ClickableViewAccessibility")
    fun bindPressFeedback(style: PressFeedbackStyle, vararg views: View) {
        if (!animationsEnabled()) {
            return
        }

        views.forEach { view ->
            view.setOnTouchListener { touched, event ->
                if (!touched.isEnabled) {
                    return@setOnTouchListener false
                }
                when (event.actionMasked) {
                    MotionEvent.ACTION_DOWN -> {
                        if (style.haptic) {
                            touched.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                        }
                        touched.animate()
                            .scaleX(style.pressedScale)
                            .scaleY(style.pressedScale)
                            .alpha(style.pressedAlpha)
                            .setDuration(90L)
                            .setInterpolator(pulseInterpolator)
                            .start()
                    }

                    MotionEvent.ACTION_UP,
                    MotionEvent.ACTION_CANCEL -> {
                        touched.animate()
                            .scaleX(1f)
                            .scaleY(1f)
                            .alpha(1f)
                            .setDuration(150L)
                            .setInterpolator(settleInterpolator)
                            .start()
                    }
                }
                false
            }
        }
    }

    fun setStatusPulse(view: View, active: Boolean) {
        if (active && statusPulseView == view && statusPulse?.isStarted == true) {
            return
        }

        statusPulse?.cancel()
        statusPulse = null
        statusPulseView = null

        if (!active) {
            view.scaleX = 1f
            view.scaleY = 1f
            return
        }

        if (!animationsEnabled()) {
            return
        }

        val scaleX = ObjectAnimator.ofFloat(view, View.SCALE_X, 1f, 1.055f, 1f).apply {
            repeatCount = ValueAnimator.INFINITE
            repeatMode = ValueAnimator.RESTART
        }
        val scaleY = ObjectAnimator.ofFloat(view, View.SCALE_Y, 1f, 1.055f, 1f).apply {
            repeatCount = ValueAnimator.INFINITE
            repeatMode = ValueAnimator.RESTART
        }
        statusPulse = AnimatorSet().apply {
            playTogether(scaleX, scaleY)
            duration = 1040L
            interpolator = pulseInterpolator
            startDelay = 120L
            start()
        }
        statusPulseView = view
    }

    fun animateProgress(progressBar: ProgressBar, target: Int) {
        progressAnimator?.cancel()
        progressAnimator = null

        if (!animationsEnabled()) {
            progressBar.progress = target.coerceIn(0, progressBar.max)
            return
        }

        progressAnimator = ObjectAnimator.ofInt(
            progressBar,
            "progress",
            progressBar.progress,
            target.coerceIn(0, progressBar.max),
        ).apply {
            duration = 520L
            interpolator = settleInterpolator
            start()
        }
    }

    fun playProgressSheen(view: View) {
        view.animate().cancel()
        if (!animationsEnabled()) {
            view.visibility = View.GONE
            view.alpha = 0f
            view.translationX = 0f
            return
        }

        view.visibility = View.VISIBLE
        view.alpha = 0f
        view.translationX = -context.dp(180).toFloat()
        view.animate()
            .alpha(0.44f)
            .translationX(context.dp(180).toFloat())
            .setDuration(720L)
            .setInterpolator(settleInterpolator)
            .withEndAction {
                view.alpha = 0f
                view.translationX = 0f
                view.visibility = View.GONE
            }
            .start()
    }

    fun pulseMetrics(vararg views: View) {
        if (!animationsEnabled()) {
            return
        }

        views.forEachIndexed { index, view ->
            view.animate()
                .alpha(0.88f)
                .scaleX(1.06f)
                .scaleY(1.06f)
                .setStartDelay(index * 24L)
                .setDuration(105L)
                .withEndAction {
                    view.animate()
                        .alpha(1f)
                        .scaleX(1f)
                        .scaleY(1f)
                        .setDuration(160L)
                        .setInterpolator(settleInterpolator)
                        .start()
                }
                .setInterpolator(pulseInterpolator)
                .start()
        }
    }

    fun release() {
        stopAmbientSheen()
        statusPulse?.cancel()
        statusPulseView = null
        progressAnimator?.cancel()
    }

    private fun Context.dp(value: Int): Int {
        return (value * resources.displayMetrics.density).toInt()
    }

    private fun applyHomeScrollMotion(
        scrollY: Int,
        motionLayout: MotionLayout,
        headerBar: View,
        backgroundSheen: View,
        panels: List<View>,
    ) {
        val progress = (scrollY / context.dp(180).toFloat()).coerceIn(0f, 1f)
        motionLayout.progress = progress
        headerBar.translationY = -context.dp(10) * progress
        headerBar.scaleX = 1f - 0.015f * progress
        headerBar.scaleY = 1f - 0.015f * progress
        headerBar.alpha = 1f - 0.08f * progress
        backgroundSheen.translationY = -scrollY * 0.035f

        panels.forEachIndexed { index, panel ->
            if (panel.visibility != View.VISIBLE) {
                return@forEachIndexed
            }
            val depth = (index + 1) / panels.size.toFloat()
            panel.translationY = -context.dp(5) * progress * depth
            val scale = 1f - 0.006f * progress * depth
            panel.scaleX = scale
            panel.scaleY = scale
        }
    }

    private fun animationsEnabled(): Boolean {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.O || ValueAnimator.areAnimatorsEnabled()
    }
}
