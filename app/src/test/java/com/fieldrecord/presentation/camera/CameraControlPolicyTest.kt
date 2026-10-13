package com.fieldrecord.presentation.camera

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CameraControlPolicyTest {
    @Test
    fun flashCyclesOffAutoOnOff() {
        assertEquals(FlashSetting.AUTO, nextFlashSetting(FlashSetting.OFF))
        assertEquals(FlashSetting.ON, nextFlashSetting(FlashSetting.AUTO))
        assertEquals(FlashSetting.OFF, nextFlashSetting(FlashSetting.ON))
    }

    @Test
    fun zoomIsClampedToDeviceSupportedRange() {
        assertEquals(1f, clampZoomRatio(0.5f, 1f, 8f))
        assertEquals(8f, clampZoomRatio(10f, 1f, 8f))
        assertEquals(3f, clampZoomRatio(3f, 1f, 8f))
    }

    @Test
    fun zoomPresetsOnlyIncludeSupportedRatios() {
        assertEquals(listOf(1f, 2f), supportedZoomPresets(0.7f, 2.4f))
        assertEquals(listOf(0.5f, 1f, 2f), supportedZoomPresets(0.5f, 8f))
        assertFalse(supportedZoomPresets(1.1f, 1.8f).contains(0.5f))
    }

    @Test
    fun invalidZoomRangeFallsBackSafely() {
        assertEquals(1f, clampZoomRatio(3f, 0f, 0f))
        assertTrue(supportedZoomPresets(0f, 0f).isEmpty())
    }
}
