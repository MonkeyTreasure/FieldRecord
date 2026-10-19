package com.fieldrecord.presentation.camera

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LatestCameraMediaTest {
    @Test
    fun latestThumbnailTracksNewestPhotoOrVideo() {
        assertTrue(isVideoTheLatestMedia(videoModifiedAt = 200L, photoModifiedAt = 100L))
        assertFalse(isVideoTheLatestMedia(videoModifiedAt = 100L, photoModifiedAt = 200L))
        assertTrue(isVideoTheLatestMedia(videoModifiedAt = 100L, photoModifiedAt = null))
        assertFalse(isVideoTheLatestMedia(videoModifiedAt = null, photoModifiedAt = 100L))
        assertTrue(isVideoTheLatestMedia(videoModifiedAt = 100L, photoModifiedAt = 100L))
    }
}
