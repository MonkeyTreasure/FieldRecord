package com.fieldrecord.presentation.video

internal fun shouldNavigateGallerySwipe(zoomScale: Float): Boolean = zoomScale <= 1.001f

internal fun isMediaZoomed(zoomScale: Float): Boolean = !shouldNavigateGallerySwipe(zoomScale)

internal data class MediaZoomTransform(
    val scale: Float = 1f,
    val offsetX: Float = 0f,
    val offsetY: Float = 0f
)

internal fun updateMediaZoom(
    current: MediaZoomTransform,
    zoomChange: Float,
    panX: Float,
    panY: Float,
    centroidX: Float,
    centroidY: Float,
    viewportWidth: Float,
    viewportHeight: Float
): MediaZoomTransform {
    val oldScale = current.scale.coerceIn(1f, 5f)
    val newScale = (oldScale * zoomChange).coerceIn(1f, 5f)
    if (newScale <= 1f) return MediaZoomTransform()

    val scaleFactor = newScale / oldScale
    val maxOffsetX = (viewportWidth * (newScale - 1f) / 2f).coerceAtLeast(0f)
    val maxOffsetY = (viewportHeight * (newScale - 1f) / 2f).coerceAtLeast(0f)
    val nextX = (current.offsetX - centroidX) * scaleFactor + centroidX + panX
    val nextY = (current.offsetY - centroidY) * scaleFactor + centroidY + panY
    return MediaZoomTransform(
        scale = newScale,
        offsetX = nextX.coerceIn(-maxOffsetX, maxOffsetX),
        offsetY = nextY.coerceIn(-maxOffsetY, maxOffsetY)
    )
}
