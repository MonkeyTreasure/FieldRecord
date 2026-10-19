package com.fieldrecord.domain.model

import kotlin.math.abs
import kotlin.math.roundToInt

/** Uses base primary swatches first, then secondary/combined colors, then generated hues. */
object PhotoTagPalette {
    val primaryColors: List<Int> = listOf(
        0xFFE53935.toInt(), // red
        0xFF1E88E5.toInt(), // blue
        0xFF43A047.toInt()  // green
    )

    val combinedColors: List<Int> = listOf(
        0xFF8E24AA.toInt(), // red + blue
        0xFFFF8F00.toInt(), // red + yellow
        0xFF00897B.toInt(), // blue + green
        0xFFD81B60.toInt(),
        0xFF5E35B1.toInt(),
        0xFF039BE5.toInt(),
        0xFF7CB342.toInt(),
        0xFFF4511E.toInt(),
        0xFF3949AB.toInt(),
        0xFF6D4C41.toInt(),
        0xFF00ACC1.toInt(),
        0xFFC0CA33.toInt()
    )

    fun nextUnused(usedColors: Set<Int>): Int {
        primaryColors.firstOrNull { it !in usedColors }?.let { return it }
        combinedColors.firstOrNull { it !in usedColors }?.let { return it }
        var index = 0
        while (true) {
            val candidate = generatedColor(index++)
            if (candidate !in usedColors && candidate !in primaryColors && candidate !in combinedColors) return candidate
        }
    }

    /** Returns only unused colors from the active palette phase. */
    fun availableColors(usedColors: Set<Int>, limit: Int = 12): List<Int> {
        val primary = primaryColors.filterNot { it in usedColors }
        if (primary.isNotEmpty()) return primary.take(limit)
        val combined = combinedColors.filterNot { it in usedColors }
        if (combined.isNotEmpty()) return combined.take(limit)
        val result = ArrayList<Int>(limit)
        var index = 0
        while (result.size < limit) {
            val candidate = generatedColor(index++)
            if (candidate !in usedColors && candidate !in primaryColors && candidate !in combinedColors && candidate !in result) {
                result += candidate
            }
        }
        return result
    }

    fun isAvailable(color: Int, usedColors: Set<Int>): Boolean {
        if (color in usedColors) return false
        val primary = primaryColors.filterNot { it in usedColors }
        if (primary.isNotEmpty()) return color in primary
        val combined = combinedColors.filterNot { it in usedColors }
        if (combined.isNotEmpty()) return color in combined
        return color !in primaryColors && color !in combinedColors &&
            (0 until 100_000).any { generatedColor(it) == color }
    }

    fun generatedColor(index: Int): Int {
        val hue = ((index.coerceAtLeast(0) * 137.508f) % 360f + 360f) % 360f
        val saturation = 0.78f
        val value = 0.9f
        val chroma = value * saturation
        val x = chroma * (1f - abs((hue / 60f) % 2f - 1f))
        val m = value - chroma
        val (r, g, b) = when (hue.toInt() / 60) {
            0 -> Triple(chroma, x, 0f)
            1 -> Triple(x, chroma, 0f)
            2 -> Triple(0f, chroma, x)
            3 -> Triple(0f, x, chroma)
            4 -> Triple(x, 0f, chroma)
            else -> Triple(chroma, 0f, x)
        }
        fun channel(component: Float) = ((component + m) * 255f).roundToInt().coerceIn(0, 255)
        return (0xFF shl 24) or (channel(r) shl 16) or (channel(g) shl 8) or channel(b)
    }
}
