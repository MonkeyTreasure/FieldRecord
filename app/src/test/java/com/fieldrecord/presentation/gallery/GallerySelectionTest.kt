package com.fieldrecord.presentation.gallery

import org.junit.Assert.assertEquals
import org.junit.Test

class GallerySelectionTest {
    @Test
    fun togglesPhotoAndVideoKeysIndependently() {
        val photoKey = "photo:7"
        val videoKey = "video:clip.mp4"
        val selectedPhoto = toggleGallerySelection(emptySet(), photoKey)
        val selectedBoth = toggleGallerySelection(selectedPhoto, videoKey)

        assertEquals(setOf(photoKey, videoKey), selectedBoth)
        assertEquals(setOf(videoKey), toggleGallerySelection(selectedBoth, photoKey))
    }

    @Test
    fun selectAllAppliesToAllVisibleMediaKeys() {
        val allKeys = setOf("photo:1", "video:a.mp4")
        assertEquals(allKeys, toggleAllGallerySelection(emptySet(), allKeys))
        assertEquals(emptySet<String>(), toggleAllGallerySelection(allKeys, allKeys))
    }
}
