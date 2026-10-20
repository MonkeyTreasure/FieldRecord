package com.fieldrecord.presentation.video

import org.junit.Assert.assertEquals
import org.junit.Test

class MediaZoomTest {
    @Test
    fun pinchZoomScalesAndKeepsPanInsideImageBounds() {
        val zoom = updateMediaZoom(
            current = MediaZoomTransform(),
            zoomChange = 3f,
            panX = 1000f,
            panY = -400f,
            centroidX = 200f,
            centroidY = 100f,
            viewportWidth = 300f,
            viewportHeight = 200f
        )
        assertEquals(3f, zoom.scale, 0f)
        assertEquals(300f, zoom.offsetX, 0f)
        assertEquals(-200f, zoom.offsetY, 0f)
    }

    @Test
    fun zoomIsLimitedToFiveTimes() {
        val zoom = updateMediaZoom(
            current = MediaZoomTransform(scale = 4f),
            zoomChange = 3f,
            panX = 0f,
            panY = 0f,
            centroidX = 150f,
            centroidY = 100f,
            viewportWidth = 300f,
            viewportHeight = 200f
        )
        assertEquals(5f, zoom.scale, 0f)
    }

    @Test
    fun zoomOutResetsScaleAndPanAtOriginalSize() {
        val zoom = updateMediaZoom(
            current = MediaZoomTransform(scale = 2f, offsetX = 100f, offsetY = -50f),
            zoomChange = 0.1f,
            panX = 0f,
            panY = 0f,
            centroidX = 100f,
            centroidY = 100f,
            viewportWidth = 300f,
            viewportHeight = 200f
        )
        assertEquals(1f, zoom.scale, 0f)
        assertEquals(0f, zoom.offsetX, 0f)
        assertEquals(0f, zoom.offsetY, 0f)
    }
}
