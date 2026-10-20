package com.fieldrecord.presentation.video

import org.junit.Assert.assertEquals
import org.junit.Test

class VideoDisplayOrientationTest {
    @Test
    fun galleryTileAddsClockwiseQuarterTurnAfterCaptureOrientation() {
        assertEquals(90, adjustedVideoThumbnailRotation(0, 90))
        assertEquals(180, adjustedVideoThumbnailRotation(90, 90))
    }

    @Test
    fun quarterTurnCaptureOrientationSwapsGalleryAspect() {
        assertEquals(1080f / 1920f, videoDisplayAspectRatio(1920, 1080, 90), 0.0001f)
        assertEquals(1080f / 1920f, videoDisplayAspectRatio(1920, 1080, 270), 0.0001f)
    }

    @Test
    fun zeroAndHalfTurnPreserveLandscapeGalleryAspect() {
        assertEquals(1920f / 1080f, videoDisplayAspectRatio(1920, 1080, 0), 0.0001f)
        assertEquals(1920f / 1080f, videoDisplayAspectRatio(1920, 1080, 180), 0.0001f)
    }
}
