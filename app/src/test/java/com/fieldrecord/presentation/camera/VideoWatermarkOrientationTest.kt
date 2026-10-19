package com.fieldrecord.presentation.camera

import org.junit.Assert.assertEquals
import org.junit.Test

class VideoWatermarkOrientationTest {
    @Test
    fun portraitOutputUsesPortraitLayoutAndRightBufferEdgeForQuarterTurn() {
        val layout = videoWatermarkLayout(bufferWidth = 1920, bufferHeight = 1080, rotationDegrees = 90)

        assertEquals(1080, layout.logicalWidth)
        assertEquals(1920, layout.logicalHeight)
        assertEquals(WatermarkBufferEdge.RIGHT, layout.bufferEdge)
    }

    @Test
    fun landscapeOutputKeepsWatermarkAlongBottomEdge() {
        val layout = videoWatermarkLayout(bufferWidth = 1920, bufferHeight = 1080, rotationDegrees = 0)

        assertEquals(1920, layout.logicalWidth)
        assertEquals(1080, layout.logicalHeight)
        assertEquals(WatermarkBufferEdge.BOTTOM, layout.bufferEdge)
    }

    @Test
    fun upsideDownRotationMovesWatermarkToTopBufferEdge() {
        val layout = videoWatermarkLayout(bufferWidth = 1920, bufferHeight = 1080, rotationDegrees = 180)

        assertEquals(WatermarkBufferEdge.TOP, layout.bufferEdge)
    }

    @Test
    fun negativeRotationNormalizesToClockwiseQuarterTurn() {
        val layout = videoWatermarkLayout(bufferWidth = 1920, bufferHeight = 1080, rotationDegrees = -90)

        assertEquals(270, layout.rotationDegrees)
        assertEquals(WatermarkBufferEdge.LEFT, layout.bufferEdge)
    }
}
