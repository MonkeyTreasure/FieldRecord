package com.fieldrecord.presentation.camera

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VideoWatermarkOrientationTest {
    @Test
    fun videoWatermarkBarUsesSameFourteenPercentHeightAsPhoto() {
        assertEquals(98f, watermarkBarHeightPx(width = 1200, height = 700), 0.001f)
    }

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
    fun videoTargetRotationIsLockedWhileRecordingStartsOrRuns() {
        assertTrue(shouldUpdateVideoTargetRotation(isStarting = false, isRecording = false))
        assertFalse(shouldUpdateVideoTargetRotation(isStarting = true, isRecording = false))
        assertFalse(shouldUpdateVideoTargetRotation(isStarting = false, isRecording = true))
    }

    @Test
    fun landscapeToPortraitRotationChangeRepositionsWatermarkOnEachFrame() {
        val landscape = videoWatermarkLayout(1920, 1080, 0)
        val portrait = videoWatermarkLayout(1920, 1080, 90)
        val landscapeAgain = videoWatermarkLayout(1920, 1080, 0)

        assertEquals(WatermarkBufferEdge.BOTTOM, landscape.bufferEdge)
        assertEquals(WatermarkBufferEdge.RIGHT, portrait.bufferEdge)
        assertEquals(1080, portrait.logicalWidth)
        assertEquals(1920, portrait.logicalHeight)
        assertEquals(WatermarkBufferEdge.BOTTOM, landscapeAgain.bufferEdge)
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
