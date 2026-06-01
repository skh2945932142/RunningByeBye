package com.runningbyebye.app

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import android.view.MotionEvent
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.DecelerateInterpolator
import android.widget.ProgressBar
import android.widget.ScrollView

class GlassMotionController(private val context: Context) {
    private var ambientAnimator: ObjectAnimator? = null
    private var statusPulse: AnimatorSet? = null
    private var statusPulseView: View? = null
    private var progressAnimator: ObjectAnimator? = null

    private val settleInterpolator = DecelerateInterpolator(1.6f)
    private val pulseInterpolator = AccelerateDecelerateInterpolator()

    fun startAmbientSheen(view: View) {
        ambientAnimator?.cancel()
        ambientAnimator = null
        view.visibility = View.VISIBLE
        view.alpha = 0.34f
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
            view.translationY = context.dp(14).toFloat()
            view.scaleX = 0.985f
            view.scaleY = 0.985f
            view.animate()
                .alpha(1f)
                .translationY(0f)
                .scaleX(1f)
                .scaleY(1f)
                .setStartDelay(index * 58L)
                .setDuration(360L)
                .setInterpolator(settleInterpolator)
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
        sheet.translationY = context.dp(20).toFloat()
        sheet.scaleX = 0.99f
        sheet.scaleY = 0.99f
        sheet.animate()
            .alpha(1f)
            .translationY(0f)
            .scaleX(1f)
            .scaleY(1f)
            .setDuration(280L)
            .setInterpolator(settleInterpolator)
            .start()
    }

    fun bindScrollParallax(scrollView: ScrollView, target: View) {
        if (!animationsEnabled()) {
            target.translationY = 0f
            return
        }

        scrollView.setOnScrollChangeListener { _, _, scrollY, _, _ ->
            target.translationY = -scrollY * 0.045f
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    fun bindPressFeedback(vararg views: View) {
        if (!animationsEnabled()) {
            return
        }

        views.forEach { view ->
            view.setOnTouchListener { touched, event ->
                when (event.actionMasked) {
                    MotionEvent.ACTION_DOWN -> {
                        touched.animate()
                            .scaleX(0.975f)
                            .scaleY(0.975f)
                            .alpha(0.92f)
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

        val scaleX = ObjectAnimator.ofFloat(view, View.SCALE_X, 1f, 1.035f, 1f).apply {
            repeatCount = ValueAnimator.INFINITE
            repeatMode = ValueAnimator.RESTART
        }
        val scaleY = ObjectAnimator.ofFloat(view, View.SCALE_Y, 1f, 1.035f, 1f).apply {
            repeatCount = ValueAnimator.INFINITE
            repeatMode = ValueAnimator.RESTART
        }
        statusPulse = AnimatorSet().apply {
            playTogether(scaleX, scaleY)
            duration = 980L
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
            duration = 320L
            interpolator = settleInterpolator
            start()
        }
    }

    fun pulseMetrics(vararg views: View) {
        if (!animationsEnabled()) {
            return
        }

        views.forEachIndexed { index, view ->
            view.animate()
                .scaleX(1.035f)
                .scaleY(1.035f)
                .setStartDelay(index * 24L)
                .setDuration(90L)
                .withEndAction {
                    view.animate()
                        .scaleX(1f)
                        .scaleY(1f)
                        .setDuration(130L)
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

    private fun animationsEnabled(): Boolean {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.O || ValueAnimator.areAnimatorsEnabled()
    }
}
