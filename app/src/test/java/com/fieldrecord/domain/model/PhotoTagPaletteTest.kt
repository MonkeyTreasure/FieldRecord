package com.fieldrecord.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PhotoTagPaletteTest {
    @Test
    fun `primary swatches are chosen before combined swatches`() {
        val first = PhotoTagPalette.nextUnused(emptySet())
        val afterPrimaries = PhotoTagPalette.nextUnused(PhotoTagPalette.primaryColors.toSet())

        assertEquals(PhotoTagPalette.primaryColors.first(), first)
        assertEquals(PhotoTagPalette.combinedColors.first(), afterPrimaries)
    }

    @Test
    fun `generated swatches continue after fixed palette is exhausted without repeats`() {
        val fixed = (PhotoTagPalette.primaryColors + PhotoTagPalette.combinedColors).toSet()
        val firstGenerated = PhotoTagPalette.nextUnused(fixed)
        val secondGenerated = PhotoTagPalette.nextUnused(fixed + firstGenerated)

        assertFalse(firstGenerated in fixed)
        assertFalse(secondGenerated in fixed)
        assertTrue(firstGenerated != secondGenerated)
    }

    @Test
    fun `used colors never reappear when combined colors are listed`() {
        val used = (PhotoTagPalette.primaryColors + PhotoTagPalette.combinedColors.take(4)).toSet()
        val available = PhotoTagPalette.availableColors(used)

        assertTrue(available.none { it in used })
        assertFalse(PhotoTagPalette.isAvailable(PhotoTagPalette.combinedColors[0], used))
        assertTrue(available.all { it in PhotoTagPalette.combinedColors.drop(4) })
    }

    @Test
    fun `generated palette contains only unique unused colors`() {
        val used = (PhotoTagPalette.primaryColors + PhotoTagPalette.combinedColors).toSet()
        val available = PhotoTagPalette.availableColors(used)

        assertEquals(12, available.size)
        assertEquals(available.size, available.toSet().size)
        assertTrue(available.none { it in used })
    }
}
