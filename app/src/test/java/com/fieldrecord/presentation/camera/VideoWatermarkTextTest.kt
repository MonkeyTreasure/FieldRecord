package com.fieldrecord.presentation.camera

import org.junit.Assert.assertEquals
import org.junit.Test

class VideoWatermarkTextTest {
    @Test
    fun watermarkLinesMatchPhotoMetadataAndIncludeElapsedDuration() {
        val lines = formatVideoWatermarkLines(
            unitId = "SBYE",
            dateText = "19.10.2026, 10:15",
            latitude = -6.2,
            longitude = 106.8,
            bearingDegrees = 45f,
            elapsedSeconds = 3661
        )

        assertEquals("SBYE", lines.unitId)
        assertEquals("19.10.2026, 10:15", lines.date)
        assertEquals("-6.20000, 106.80000 (NE - 45°)", lines.location)
        assertEquals("REC 01:01:01", lines.duration)
    }

    @Test
    fun missingGpsDoesNotInventCoordinatesOrBearing() {
        val lines = formatVideoWatermarkLines(
            unitId = "SBYE",
            dateText = "19.10.2026, 10:15",
            latitude = null,
            longitude = null,
            bearingDegrees = Float.NaN,
            elapsedSeconds = 7
        )

        assertEquals("GPS unavailable", lines.location)
        assertEquals("REC 00:07", lines.duration)
    }

    @Test
    fun invalidBearingKeepsCoordinatesWithoutDirection() {
        val lines = formatVideoWatermarkLines(
            unitId = "SBYE",
            dateText = "19.10.2026, 10:15",
            latitude = 1.25,
            longitude = 103.5,
            bearingDegrees = -1f,
            elapsedSeconds = 0
        )

        assertEquals("1.25000, 103.50000", lines.location)
        assertEquals("REC 00:00", lines.duration)
    }
}
