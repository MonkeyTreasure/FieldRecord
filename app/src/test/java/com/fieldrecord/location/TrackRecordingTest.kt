package com.fieldrecord.location

import com.fieldrecord.data.local.entity.TrackPointEntity
import com.fieldrecord.data.local.entity.TrackSessionEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TrackRecordingTest {
    @Test
    fun acceptsFirstAccurateFixAndRejectsInaccurateFixes() {
        val first = TrackFix(1.0, 1.0, 1_000L, 4f, 0f)
        assertTrue(shouldRecordTrackFix(null, first))
        assertFalse(shouldRecordTrackFix(null, first.copy(accuracyMeters = 45f)))
    }

    @Test
    fun filtersSmallGpsDriftAndAcceptsMeaningfulMovement() {
        val previous = TrackPointEntity(1, 7, 0, 0.0, 0.0, 1_000L, 4f, 0f)
        assertFalse(shouldRecordTrackFix(previous, TrackFix(0.0, 0.00001, 2_000L, 4f, 0f)))
        assertTrue(shouldRecordTrackFix(previous, TrackFix(0.0, 0.00005, 2_000L, 4f, 0f)))
        assertFalse(shouldRecordTrackFix(previous, TrackFix(0.0, 0.00001, 6_000L, 4f, 0f)))
    }

    @Test
    fun computesElapsedTimeExcludingPausedTime() {
        val recording = TrackSessionEntity(
            id = 4,
            startedAt = 1_000L,
            pausedDurationMillis = 2_000L,
            status = TrackSessionEntity.STATUS_RECORDING
        )
        assertEquals(7_000L, elapsedTrackMillis(recording, 10_000L))

        val paused = recording.copy(
            status = TrackSessionEntity.STATUS_PAUSED,
            pauseStartedAt = 8_000L
        )
        assertEquals(5_000L, elapsedTrackMillis(paused, 12_000L))
    }
}
