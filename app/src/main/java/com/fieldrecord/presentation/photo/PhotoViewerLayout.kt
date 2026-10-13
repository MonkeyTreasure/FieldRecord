package com.fieldrecord.presentation.photo

internal data class FittedPhotoSize(val width: Float, val height: Float)

internal fun calculateFittedPhotoSize(
    viewportWidth: Float,
    viewportHeight: Float,
    photoWidth: Float,
    photoHeight: Float
): FittedPhotoSize {
    if (viewportWidth <= 0f || viewportHeight <= 0f || photoWidth <= 0f || photoHeight <= 0f) {
        return FittedPhotoSize(0f, 0f)
    }
    val scale = minOf(viewportWidth / photoWidth, viewportHeight / photoHeight)
    return FittedPhotoSize(photoWidth * scale, photoHeight * scale)
}
