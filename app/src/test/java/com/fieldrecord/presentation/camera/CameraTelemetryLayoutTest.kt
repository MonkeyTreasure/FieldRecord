package com.fieldrecord.presentation.camera

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CameraTelemetryLayoutTest {
    @Test
    fun landscapeRotationsRotateMockupAndPinItsVisualBottomLeft() {
        val clockwise = cameraTelemetryOrientation(surfaceRotation = 1)
        assertEquals(90, clockwise.rotationDegrees)
        assertEquals(1f, clockwise.pivotX)
        assertEquals(1f, clockwise.pivotY)

        val counterClockwise = cameraTelemetryOrientation(surfaceRotation = 3)
        assertEquals(270, counterClockwise.rotationDegrees)
        assertEquals(0f, counterClockwise.pivotX)
        assertEquals(0f, counterClockwise.pivotY)
    }

    @Test
    fun portraitUpsideDownRotationAlsoKeepsVisualBottomLeftPinned() {
        val upsideDown = cameraTelemetryOrientation(surfaceRotation = 2)
        assertEquals(180, upsideDown.rotationDegrees)
        assertEquals(1f, upsideDown.pivotX)
        assertEquals(0f, upsideDown.pivotY)
    }

    @Test
    fun landscapeTelemetryUsesScreenBottomAnchor() {
        assertTrue(telemetryUsesScreenBottomAnchor(isLandscape = true))
    }

    @Test
    fun portraitTelemetryRemainsAttachedToViewfinder() {
        assertFalse(telemetryUsesScreenBottomAnchor(isLandscape = false))
    }
}
