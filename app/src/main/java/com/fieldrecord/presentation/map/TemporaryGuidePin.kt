package com.fieldrecord.presentation.map

internal data class TemporaryGuidePin(
    val id: Long,
    val latitude: Double,
    val longitude: Double
)

internal fun googleNavigationUri(latitude: Double, longitude: Double): String =
    "google.navigation:q=$latitude,$longitude"
