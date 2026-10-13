package com.fieldrecord.presentation.map

/** Persistence boundary for the user's selected basemap. */
interface MapLayerStore {
    fun readSatellite(): Boolean
    fun writeSatellite(value: Boolean)
}

/** Holds the current basemap choice and saves every toggle through its store. */
class MapLayerSelection(private val store: MapLayerStore) {
    fun read(): Boolean = store.readSatellite()

    fun toggle(): Boolean {
        val next = !store.readSatellite()
        store.writeSatellite(next)
        return next
    }
}
