package com.fieldrecord.presentation.camera

import org.junit.Assert.assertEquals
import org.junit.Test

class PhotoFileManagerTest {
    @Test
    fun filenameUsesCalendarDateAndThreeDigitSequence() {
        assertEquals("IMG_260925_001.jpg", PhotoFileManager.formatFilename("260925", 1))
        assertEquals("IMG_260925_027.jpg", PhotoFileManager.formatFilename("260925", 27))
    }

    @Test
    fun monthlySequenceResetsToOneOnNewMonth() {
        val files = listOf("IMG_261031_018.jpg", "IMG_261031_019.jpg")
        assertEquals(1, PhotoFileManager.nextMonthlySequence("261101", files))
    }

    @Test
    fun monthlySequenceCountsPhotosAcrossDifferentDatesInSameMonth() {
        val files = listOf(
            "IMG_261001_001.jpg",
            "IMG_261002_002.jpg",
            "IMG_261013_001.jpg",
            "IMG_260930_099.jpg"
        )
        assertEquals(4, PhotoFileManager.nextMonthlySequence("261014", files))
    }

    @Test
    fun sequenceMayExceedThreeDigitsWithoutCollision() {
        assertEquals("IMG_260925_1000.jpg", PhotoFileManager.formatFilename("260925", 1000))
    }
}
