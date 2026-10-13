package com.fieldrecord.presentation.map

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MapLayerSelectionTest {
    private class MemoryStore(initial: Boolean = false) : MapLayerStore {
        private var value = initial
        override fun readSatellite(): Boolean = value
        override fun writeSatellite(value: Boolean) { this.value = value }
    }

    @Test
    fun lastLayerChoiceIsRestoredByNextSelection() {
        val store = MemoryStore()
        assertFalse(MapLayerSelection(store).read())
        assertTrue(MapLayerSelection(store).toggle())
        assertTrue(MapLayerSelection(store).read())
    }
}
