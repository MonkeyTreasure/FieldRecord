package com.fieldrecord.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class PhotoTagColorTest {
    @Test
    fun tagColorIsStableAndCaseInsensitive() {
        assertEquals(photoTagColorArgb("drainase"), photoTagColorArgb("DRAINASE"))
    }

    @Test
    fun differentTagsReceiveDifferentColorsForCommonCategories() {
        assertNotEquals(photoTagColorArgb("drainase"), photoTagColorArgb("jalan"))
    }

    @Test
    fun singlePhotoNoteHashtagSetsPinColor() {
        assertEquals(
            photoTagColorArgb("drainase"),
            photoPinColorArgb("Temuan #drainase di lapangan")
        )
    }

    @Test
    fun noteWithoutHashtagKeepsRedPin() {
        assertEquals(0xFFDC2D2D.toInt(), photoPinColorArgb("Temuan di lapangan"))
    }

    @Test
    fun singleTaggedPinUsesItsTagColor() {
        assertEquals(
            photoTagColorArgb("drainase"),
            photoMarkerColorArgb(listOf("drainase"))
        )
    }

    @Test
    fun untaggedClusterStaysRed() {
        assertEquals(0xFFDC2D2D.toInt(), photoMarkerColorArgb(listOf(null, null)))
    }

    @Test
    fun mixedClusterUsesMostCommonTagColor() {
        assertEquals(
            photoTagColorArgb("drainase"),
            photoMarkerColorArgb(listOf("drainase", "jalan", "drainase"))
        )
    }
}
