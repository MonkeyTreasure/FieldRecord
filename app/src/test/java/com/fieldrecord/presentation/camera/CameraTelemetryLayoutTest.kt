package com.fieldrecord.presentation.camera

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CameraTelemetryLayoutTest {
    @Test
    fun landscapeTelemetryUsesScreenBottomAnchor() {
        assertTrue(telemetryUsesScreenBottomAnchor(isLandscape = true))
    }

    @Test
    fun portraitTelemetryRemainsAttachedToViewfinder() {
        assertFalse(telemetryUsesScreenBottomAnchor(isLandscape = false))
    }
}
