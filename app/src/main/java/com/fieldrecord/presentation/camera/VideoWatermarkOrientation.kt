package com.fieldrecord.presentation.camera

internal enum class WatermarkBufferEdge { BOTTOM, RIGHT, TOP, LEFT }

internal data class VideoWatermarkLayout(
    val logicalWidth: Int,
    val logicalHeight: Int,
    val rotationDegrees: Int,
    val bufferEdge: WatermarkBufferEdge,
    val bufferTransformValues: FloatArray
)

internal fun videoWatermarkLayout(
    bufferWidth: Int,
    bufferHeight: Int,
    rotationDegrees: Int
): VideoWatermarkLayout {
    val normalized = ((rotationDegrees % 360) + 360) % 360
    val snapped = (((normalized + 45) / 90) * 90) % 360
    val rotation = if (snapped in setOf(0, 90, 180, 270)) snapped else 0
    return when (rotation) {
        90 -> VideoWatermarkLayout(
            logicalWidth = bufferHeight,
            logicalHeight = bufferWidth,
            rotationDegrees = rotation,
            bufferEdge = WatermarkBufferEdge.RIGHT,
            bufferTransformValues = floatArrayOf(0f, 1f, 0f, -1f, 0f, bufferHeight.toFloat(), 0f, 0f, 1f)
        )
        180 -> VideoWatermarkLayout(
            logicalWidth = bufferWidth,
            logicalHeight = bufferHeight,
            rotationDegrees = rotation,
            bufferEdge = WatermarkBufferEdge.TOP,
            bufferTransformValues = floatArrayOf(-1f, 0f, bufferWidth.toFloat(), 0f, -1f, bufferHeight.toFloat(), 0f, 0f, 1f)
        )
        270 -> VideoWatermarkLayout(
            logicalWidth = bufferHeight,
            logicalHeight = bufferWidth,
            rotationDegrees = rotation,
            bufferEdge = WatermarkBufferEdge.LEFT,
            bufferTransformValues = floatArrayOf(0f, -1f, bufferWidth.toFloat(), 1f, 0f, 0f, 0f, 0f, 1f)
        )
        else -> VideoWatermarkLayout(
            logicalWidth = bufferWidth,
            logicalHeight = bufferHeight,
            rotationDegrees = 0,
            bufferEdge = WatermarkBufferEdge.BOTTOM,
            bufferTransformValues = floatArrayOf(1f, 0f, 0f, 0f, 1f, 0f, 0f, 0f, 1f)
        )
    }
}
