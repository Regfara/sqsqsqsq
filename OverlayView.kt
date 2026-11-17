package com.example.gestureplayer.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import kotlin.math.max

/**
 * Рисует скелет руки поверх превью камеры.
 * Масштабирование совпадает с PreviewView в режиме FILL_CENTER.
 */
class OverlayView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    private var points: List<Pair<Float, Float>> = emptyList()
    private var imageWidth = 1
    private var imageHeight = 1

    private val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#6C63FF")
        strokeWidth = 6f
    }
    private val pointPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#00E5A0")
        style = Paint.Style.FILL
    }

    fun setHand(points: List<Pair<Float, Float>>, imageWidth: Int, imageHeight: Int) {
        this.points = points
        this.imageWidth = imageWidth
        this.imageHeight = imageHeight
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (points.size < 21) return

        val scale = max(width / imageWidth.toFloat(), height / imageHeight.toFloat())
        val offsetX = (width - imageWidth * scale) / 2f
        val offsetY = (height - imageHeight * scale) / 2f

        fun x(i: Int) = points[i].first * imageWidth * scale + offsetX
        fun y(i: Int) = points[i].second * imageHeight * scale + offsetY

        for ((a, b) in CONNECTIONS) {
            canvas.drawLine(x(a), y(a), x(b), y(b), linePaint)
        }
        for (i in points.indices) {
            canvas.drawCircle(x(i), y(i), 9f, pointPaint)
        }
    }

    companion object {
        private val CONNECTIONS = listOf(
            0 to 1, 1 to 2, 2 to 3, 3 to 4,          // большой
            0 to 5, 5 to 6, 6 to 7, 7 to 8,          // указательный
            5 to 9, 9 to 10, 10 to 11, 11 to 12,     // средний
            9 to 13, 13 to 14, 14 to 15, 15 to 16,   // безымянный
            13 to 17, 17 to 18, 18 to 19, 19 to 20,  // мизинец
            0 to 17                                  // край ладони
        )
    }
}
