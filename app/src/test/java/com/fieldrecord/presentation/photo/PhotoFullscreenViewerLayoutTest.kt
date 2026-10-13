package com.fieldrecord.presentation.photo

import org.junit.Assert.assertEquals
import org.junit.Test

class PhotoFullscreenViewerLayoutTest {
    @Test
    fun fitsLandscapePhotoInsideViewportWithoutCropping() {
        val result = calculateFittedPhotoSize(400f, 600f, 1200f, 800f)

        assertEquals(400f, result.width, 0.01f)
        assertEquals(800f / 3f, result.height, 0.01f)
    }

    @Test
    fun fitsPortraitPhotoInsideViewportWithoutCropping() {
        val result = calculateFittedPhotoSize(400f, 600f, 600f, 1200f)

        assertEquals(300f, result.width, 0.01f)
        assertEquals(600f, result.height, 0.01f)
    }
}
