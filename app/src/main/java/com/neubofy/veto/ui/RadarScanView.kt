package com.neubofy.veto.ui

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.SweepGradient
import android.util.AttributeSet
import android.view.View
import android.view.animation.LinearInterpolator
import kotlin.math.min

class RadarScanView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var rotationAngle = 0f
    private val animator: ValueAnimator

    private val circlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#44CC0000") // Semi-transparent red
        style = Paint.Style.STROKE
        strokeWidth = 2f
    }

    private val sweepPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    init {
        animator = ValueAnimator.ofFloat(0f, 360f).apply {
            duration = 2000
            repeatCount = ValueAnimator.INFINITE
            interpolator = LinearInterpolator()
            addUpdateListener {
                rotationAngle = it.animatedValue as Float
                invalidate()
            }
        }
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        val cx = w / 2f
        val cy = h / 2f
        val radius = min(cx, cy) - 4f

        val colors = intArrayOf(Color.parseColor("#00CC0000"), Color.parseColor("#88CC0000"))
        val positions = floatArrayOf(0f, 1f)
        sweepPaint.shader = SweepGradient(cx, cy, colors, positions)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val cx = width / 2f
        val cy = height / 2f
        val radius = min(cx, cy) - 4f

        // Draw concentric circles
        for (i in 1..4) {
            canvas.drawCircle(cx, cy, radius * i / 4f, circlePaint)
        }
        canvas.drawLine(cx, cy - radius, cx, cy + radius, circlePaint)
        canvas.drawLine(cx - radius, cy, cx + radius, cy, circlePaint)

        // Draw sweeping radar
        canvas.save()
        canvas.rotate(rotationAngle, cx, cy)
        canvas.drawCircle(cx, cy, radius, sweepPaint)
        canvas.restore()
    }

    fun startAnimation() {
        if (!animator.isRunning) {
            animator.start()
        }
    }

    fun stopAnimation() {
        animator.cancel()
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        startAnimation()
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        stopAnimation()
    }
}
