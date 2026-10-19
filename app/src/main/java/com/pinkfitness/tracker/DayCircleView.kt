package com.pinkfitness.tracker

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.DashPathEffect
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Shader
import android.util.AttributeSet
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

/** Большой розовый круг с номером текущего дня — сердце главного экрана. */
class DayCircleView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0
) : View(context, attrs, defStyle) {

    var dayNumber: Int = 1
        set(v) { field = v; invalidate() }
    var totalDays: Int = 75
        set(v) { field = v; invalidate() }
    var finished: Boolean = false
        set(v) { field = v; invalidate() }
    var doneToday: Boolean = false
        set(v) { field = v; invalidate() }

    private val density = resources.displayMetrics.density
    private fun dp(v: Float): Float = v * density

    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0x40FF6FA5 }
    private val rimPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = dp(4f)
        color = Color.WHITE
    }
    private val dashPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = dp(2.5f)
        color = 0xB3FFFFFF.toInt()
        pathEffect = DashPathEffect(floatArrayOf(dp(9f), dp(7f)), 0f)
    }
    private val bigTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textAlign = Paint.Align.CENTER
        isFakeBoldText = true
    }
    private val smallTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xF2FFFFFF.toInt()
        textAlign = Paint.Align.CENTER
        isFakeBoldText = true
        letterSpacing = 0.4f
    }
    private val subTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xE6FFFFFF.toInt()
        textAlign = Paint.Align.CENTER
    }
    private val emojiPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
    }
    private val badgePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF7ED9A8.toInt() }
    private val badgeRimPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = dp(3f)
        color = Color.WHITE
    }

    private var pulse = 0f
    private var animator: ValueAnimator? = null

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        val r = min(w, h) / 2f
        fillPaint.shader = LinearGradient(
            -r * 0.5f, -r * 0.5f, r * 1.1f, r * 1.1f,
            intArrayOf(0xFFFFB3D1.toInt(), 0xFFFF6FA5.toInt(), 0xFFF04E8E.toInt()),
            floatArrayOf(0f, 0.55f, 1f),
            Shader.TileMode.CLAMP
        )
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        animator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 1500
            repeatCount = ValueAnimator.INFINITE
            repeatMode = ValueAnimator.REVERSE
            interpolator = AccelerateDecelerateInterpolator()
            addUpdateListener {
                pulse = it.animatedValue as Float
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
        val cx = width / 2f
        val cy = height / 2f
        val maxR = min(width, height) / 2f

        // Мягкое пульсирующее свечение
        glowPaint.alpha = (28 + pulse * 44).toInt()
        canvas.drawCircle(cx, cy, maxR * (0.90f + pulse * 0.05f), glowPaint)

        // Искорки вокруг круга
        emojiPaint.textSize = dp(17f)
        val sparkles = arrayOf("✨", "💖", "✨", "🌸")
        for (i in sparkles.indices) {
            val a = Math.toRadians(i * 90.0 + 45.0 + pulse * 8.0)
            val rr = maxR * 0.93f
            val x = cx + rr * cos(a).toFloat()
            val y = cy + rr * sin(a).toFloat() + dp(6f)
            canvas.drawText(sparkles[i], x, y, emojiPaint)
        }

        // Основной круг
        val r = maxR * 0.78f
        canvas.drawCircle(cx, cy, r, fillPaint)
        canvas.drawCircle(cx, cy, r - dp(2f), rimPaint)
        canvas.drawCircle(cx, cy, r * 0.88f, dashPaint)

        if (finished) {
            emojiPaint.textSize = r * 0.95f
            val fm = emojiPaint.fontMetrics
            canvas.drawText("🏆", cx, cy - (fm.ascent + fm.descent) / 2f, emojiPaint)
            subTextPaint.textSize = dp(14f)
            canvas.drawText("челлендж пройден!", cx, cy + r * 0.68f, subTextPaint)
        } else {
            // «ДЕНЬ»
            smallTextPaint.textSize = dp(14f)
            canvas.drawText("Д Е Н Ь", cx, cy - r * 0.40f, smallTextPaint)
            // Большая цифра
            bigTextPaint.textSize = r * 0.82f
            val fm = bigTextPaint.fontMetrics
            canvas.drawText(dayNumber.toString(), cx, cy - (fm.ascent + fm.descent) / 2f + r * 0.10f, bigTextPaint)
            // «из N»
            subTextPaint.textSize = dp(14f)
            canvas.drawText("из $totalDays", cx, cy + r * 0.62f, subTextPaint)
        }

        // Значок «сегодня выполнено»
        if (doneToday && !finished) {
            val bx = cx + r * 0.74f
            val by = cy + r * 0.74f
            val br = dp(19f)
            canvas.drawCircle(bx, by, br, badgeRimPaint)
            canvas.drawCircle(bx, by, br, badgePaint)
            bigTextPaint.textSize = dp(19f)
            val fm = bigTextPaint.fontMetrics
            canvas.drawText("✓", bx, by - (fm.ascent + fm.descent) / 2f, bigTextPaint)
        }
    }
}
