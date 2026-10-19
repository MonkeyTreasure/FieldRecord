package com.fieldrecord.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PhotoTagFacetsTest {
    @Test
    fun parsesCategoriesBlocksAndLocationsFromNotes() {
        val facets = extractPhotoTagFacets(
            "#kebakaran #lingkaran_pagi #blok_U21_SBYE #blok_U23"
        )

        assertEquals(listOf("Kebakaran", "Lingkaran Pagi"), facets.categories.map { it.label })
        assertEquals(listOf("U21", "U23"), facets.blocks.map { it.label })
        assertEquals(listOf("SBYE"), facets.locations.map { it.label })
    }

    @Test
    fun ignoresMalformedBlockTagsAndDeduplicatesCaseInsensitively() {
        val facets = extractPhotoTagFacets("#blok_ #Kebakaran #kebakaran #blok_U21")

        assertEquals(listOf("Kebakaran"), facets.categories.map { it.label })
        assertEquals(listOf("U21"), facets.blocks.map { it.label })
        assertTrue(facets.locations.isEmpty())
    }

    @Test
    fun categoryBlockAndLocationFiltersMatchOnlyTheirOwnFacet() {
        val note = "#lingkaran_pagi #blok_U21_SBYE"

        assertTrue(matchesPhotoTagFilter(note, PhotoTagFilter(PhotoTagKind.CATEGORY, "lingkaran_pagi")))
        assertTrue(matchesPhotoTagFilter(note, PhotoTagFilter(PhotoTagKind.BLOCK, "u21")))
        assertTrue(matchesPhotoTagFilter(note, PhotoTagFilter(PhotoTagKind.LOCATION, "sbye")))
        assertFalse(matchesPhotoTagFilter(note, PhotoTagFilter(PhotoTagKind.BLOCK, "u2")))
        assertFalse(matchesPhotoTagFilter(note, PhotoTagFilter(PhotoTagKind.LOCATION, "sby")))
    }
}
