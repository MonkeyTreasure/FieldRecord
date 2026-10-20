package com.fieldrecord.presentation.gallery

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class GalleryMediaNavigationTest {
    @Test
    fun horizontalSwipesMoveAcrossMixedPhotoAndVideoTimeline() {
        assertEquals(2, gallerySwipeTargetIndex(currentIndex = 1, itemCount = 4, swipeLeft = true))
        assertEquals(0, gallerySwipeTargetIndex(currentIndex = 1, itemCount = 4, swipeLeft = false))
    }

    @Test
    fun swipingPastTimelineEndsDoesNotWrapAround() {
        assertNull(gallerySwipeTargetIndex(currentIndex = 0, itemCount = 4, swipeLeft = false))
        assertNull(gallerySwipeTargetIndex(currentIndex = 3, itemCount = 4, swipeLeft = true))
    }
}
