package com.fieldrecord.presentation.camera

import org.junit.Assert.assertEquals
import org.junit.Test

class VideoWatermarkTextTest {
    @Test
    fun recordingTimerStartsAtZeroAndClearsWhenRecordingStops() {
        assertEquals("REC 00:00", recordingTimerLabel(isStartingVideo = true, isRecording = false, elapsedSeconds = 0))
        assertEquals("REC 00:07", recordingTimerLabel(isStartingVideo = false, isRecording = true, elapsedSeconds = 7))
        assertEquals(null, recordingTimerLabel(isStartingVideo = false, isRecording = false, elapsedSeconds = 7))
        assertEquals("REC 00:00", recordingTimerLabel(isStartingVideo = true, isRecording = false, elapsedSeconds = 0))
    }

    @Test
    fun recordMockupIsVisibleFromVideoModeAndUsesSameTimerWhileRecording() {
        assertEquals("REC 00:00", videoModeRecordMockupLabel(true, false, false, 0))
        assertEquals("REC 00:07", videoModeRecordMockupLabel(true, false, true, 7))
        assertEquals("REC 00:00", videoModeRecordMockupLabel(true, false, false, 7))
        assertEquals(null, videoModeRecordMockupLabel(false, false, false, 0))
    }

    @Test
    fun previewShowsOnlyActiveRecordingDuration() {
        assertEquals("REC 01:01:01", formatRecordingDuration(3661))
    }

    @Test
    fun watermarkLinesMatchPhotoMetadataWithoutRecordingTime() {
        val lines = formatVideoWatermarkLines(
            unitId = "SBYE",
            dateText = "19.10.2026, 10:15",
            latitude = -6.2,
            longitude = 106.8,
            bearingDegrees = 45f
        )

        assertEquals("SBYE", lines.unitId)
        assertEquals("19.10.2026, 10:15", lines.date)
        assertEquals("-6.20000, 106.80000 (NE - 45°)", lines.location)
    }

    @Test
    fun missingGpsDoesNotInventCoordinatesOrBearing() {
        val lines = formatVideoWatermarkLines(
            unitId = "SBYE",
            dateText = "19.10.2026, 10:15",
            latitude = null,
            longitude = null,
            bearingDegrees = Float.NaN
        )

        assertEquals("GPS unavailable", lines.location)
    }

    @Test
    fun invalidBearingKeepsCoordinatesWithoutDirection() {
        val lines = formatVideoWatermarkLines(
            unitId = "SBYE",
            dateText = "19.10.2026, 10:15",
            latitude = 1.25,
            longitude = 103.5,
            bearingDegrees = -1f
        )

        assertEquals("1.25000, 103.50000", lines.location)
    }
}
