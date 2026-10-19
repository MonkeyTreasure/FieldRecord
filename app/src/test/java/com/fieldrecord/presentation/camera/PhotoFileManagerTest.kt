package com.fieldrecord.presentation.camera

import org.junit.Assert.assertEquals
import org.junit.Test

class PhotoFileManagerTest {
    @Test
    fun videoFilenameUsesVideoPrefixAndThreeDigitSequence() {
        assertEquals("VID_260925_001.mp4", PhotoFileManager.formatVideoFilename("260925", 1))
        assertEquals("VID_260925_1000.mp4", PhotoFileManager.formatVideoFilename("260925", 1000))
    }

    @Test
    fun videoSequenceResetsOnNewMonthAndSkipsExistingNumbers() {
        val files = listOf("VID_261031_018.mp4", "VID_261031_019.mp4", "VID_261101_002.mp4")
        assertEquals(3, PhotoFileManager.nextMonthlyVideoSequence("261102", files))
        assertEquals(1, PhotoFileManager.nextMonthlyVideoSequence("261201", files))
    }

    @Test
    fun videoSequenceHandlesGapsWithoutColliding() {
        val files = listOf("VID_261001_001.mp4", "VID_261005_005.mp4")
        assertEquals(6, PhotoFileManager.nextMonthlyVideoSequence("261006", files))
    }

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

    @Test
    fun filenameIncludesSanitizedPhotographerName() {
        assertEquals(
            "IMG_260925_001_Novianto_Syahmud.jpg",
            PhotoFileManager.formatFilename("260925", 1, "Novianto Syahmud")
        )
    }

    @Test
    fun monthlySequenceRecognizesNamesAddedToPreviousPhotos() {
        val files = listOf("IMG_261001_001_Novianto_Syahmud.jpg", "IMG_261002_004_Ani.jpg")
        assertEquals(5, PhotoFileManager.nextMonthlySequence("261003", files))
    }

    @Test
    fun photographerCanBeReadFromStampedFilename() {
        assertEquals(
            "Novianto Syahmud",
            PhotoFileManager.photographerStampFromFilename("IMG_260925_001_Novianto_Syahmud.jpg")
        )
    }

    @Test
    fun userNameSanitizerRemovesPathSeparators() {
        assertEquals("Novianto_Syahmud", PhotoFileManager.sanitizeUserNameForFilename("Novianto/Syahmud"))
    }
}
