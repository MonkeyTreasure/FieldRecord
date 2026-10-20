package com.fieldrecord.presentation.video

internal fun normalizeVideoRotationDegrees(rotationDegrees: Int): Int =
    ((rotationDegrees % 360) + 360) % 360

internal fun adjustedVideoThumbnailRotation(captureRotationDegrees: Int, additionalRotationDegrees: Int): Int =
    normalizeVideoRotationDegrees(captureRotationDegrees + additionalRotationDegrees)

internal fun videoDisplayAspectRatio(width: Int, height: Int, rotationDegrees: Int): Float {
    if (width <= 0 || height <= 0) return 16f / 9f
    val rotation = normalizeVideoRotationDegrees(rotationDegrees)
    val quarterTurn = rotation == 90 || rotation == 270
    val displayWidth = if (quarterTurn) height else width
    val displayHeight = if (quarterTurn) width else height
    return displayWidth.toFloat() / displayHeight.toFloat()
}
