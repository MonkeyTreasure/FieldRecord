package com.fieldrecord.presentation.camera

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.Color as AndroidColor

internal data class VideoWatermarkFrame(
    val lines: VideoWatermarkLines,
    val textColor: Int,
    val backgroundColor: Int,
    val transparentBackground: Boolean,
    val backgroundAlpha: Int
)

internal class VideoWatermarkRenderer {
    private val backgroundPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = Paint().typeface
    }

    fun draw(canvas: Canvas, width: Int, height: Int, frame: VideoWatermarkFrame) {
        if (width <= 0 || height <= 0) return
        canvas.drawColor(AndroidColor.TRANSPARENT, PorterDuff.Mode.CLEAR)

        val barHeight = (minOf(width, height) * 0.18f).coerceAtLeast(1f)
        val barTop = height - barHeight
        backgroundPaint.color = frame.backgroundColor
        backgroundPaint.alpha = if (frame.transparentBackground) frame.backgroundAlpha.coerceIn(0, 255) else 255
        canvas.drawRect(0f, barTop, width.toFloat(), height.toFloat(), backgroundPaint)

        val textSize = barHeight * 0.20f
        textPaint.color = frame.textColor
        textPaint.textSize = textSize
        textPaint.isAntiAlias = true
        textPaint.alpha = 255
        val lines = listOf(frame.lines.unitId, frame.lines.date, frame.lines.location, frame.lines.duration)
        val x = width * 0.03f
        val firstBaseline = barTop + textSize * 0.95f
        val lineSpacing = textSize * 1.1f
        lines.forEachIndexed { index, line ->
            canvas.drawText(line, x, firstBaseline + lineSpacing * index, textPaint)
        }
    }
}
