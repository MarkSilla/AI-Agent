package com.marksilla.auraagent

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.RectF
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.view.animation.AccelerateDecelerateInterpolator
import kotlin.math.min

class AuraEdgeOverlay(
    private val context: Context
) {
    private val windowManager =
        context.getSystemService(Context.WINDOW_SERVICE) as WindowManager

    private var overlayView: AuraEdgeView? = null
    private var animator: ValueAnimator? = null

    fun showListening(): Boolean = show("AURA is listening...")

    fun showStatus(message: String): Boolean = show(message)

    fun hide() {
        animator?.cancel()
        animator = null

        overlayView?.let { view ->
            runCatching {
                windowManager.removeView(view)
            }
        }

        overlayView = null
    }

    private fun show(message: String): Boolean {
        if (!Settings.canDrawOverlays(context)) {
            return false
        }

        val existingView = overlayView
        if (existingView != null) {
            existingView.message = message
            existingView.invalidate()
            return true
        }

        val view = AuraEdgeView(context).apply {
            this.message = message
        }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
                WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
        }

        return runCatching {
            windowManager.addView(view, params)
            overlayView = view
            startPulse(view)
            true
        }.getOrDefault(false)
    }

    private fun startPulse(view: AuraEdgeView) {
        animator?.cancel()

        animator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 1150L
            repeatMode = ValueAnimator.REVERSE
            repeatCount = ValueAnimator.INFINITE
            interpolator = AccelerateDecelerateInterpolator()
            addUpdateListener { animation ->
                view.pulse = animation.animatedValue as Float
                view.invalidate()
            }
            start()
        }
    }
}

private class AuraEdgeView(
    context: Context
) : View(context) {
    var pulse: Float = 0f
    var message: String = "AURA is listening..."

    private val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
    }

    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textAlign = Paint.Align.CENTER
        textSize = 44f
        setShadowLayer(22f, 0f, 0f, Color.rgb(0, 229, 255))
    }

    private val rect = RectF()

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val minSide = min(width, height).toFloat()
        val baseStroke = (minSide * 0.012f).coerceIn(7f, 18f)
        val glowAlpha = (90 + pulse * 115).toInt()
        val innerAlpha = (175 + pulse * 65).toInt()
        val inset = baseStroke * 1.8f
        val corner = baseStroke * 4.5f

        rect.set(
            inset,
            inset,
            width - inset,
            height - inset
        )

        borderPaint.strokeWidth = baseStroke * 4.2f
        borderPaint.color = Color.argb(glowAlpha, 124, 77, 255)
        canvas.drawRoundRect(rect, corner, corner, borderPaint)

        borderPaint.strokeWidth = baseStroke * 2.4f
        borderPaint.color = Color.argb(glowAlpha, 0, 229, 255)
        canvas.drawRoundRect(rect, corner, corner, borderPaint)

        borderPaint.strokeWidth = baseStroke
        borderPaint.color = Color.argb(innerAlpha, 160, 246, 255)
        canvas.drawRoundRect(rect, corner, corner, borderPaint)

        val textY = height * 0.44f
        textPaint.textSize = (minSide * 0.045f).coerceIn(32f, 52f)
        textPaint.color = Color.argb(225, 232, 252, 255)
        canvas.drawText(message, width / 2f, textY, textPaint)
    }
}
