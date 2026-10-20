package com.fieldrecord.presentation.map

import java.io.ByteArrayInputStream
import javax.xml.parsers.DocumentBuilderFactory
import org.w3c.dom.Element

private const val MAX_KML_POINTS = 50_000

internal fun parseKmlRouteCoordinates(kml: String): List<RouteCoordinate> {
    require(kml.length <= 10_000_000) { "File KML terlalu besar" }
    val factory = DocumentBuilderFactory.newInstance().apply {
        isNamespaceAware = true
        runCatching { setFeature("http://apache.org/xml/features/disallow-doctype-decl", true) }
        runCatching { setFeature("http://xml.org/sax/features/external-general-entities", false) }
        runCatching { setFeature("http://xml.org/sax/features/external-parameter-entities", false) }
        runCatching { setAttribute("http://javax.xml.XMLConstants/property/accessExternalDTD", "") }
        runCatching { setAttribute("http://javax.xml.XMLConstants/property/accessExternalSchema", "") }
    }
    val document = try {
        factory.newDocumentBuilder().parse(ByteArrayInputStream(kml.toByteArray(Charsets.UTF_8)))
    } catch (error: Exception) {
        throw IllegalArgumentException("File KML tidak valid", error)
    }

    val lineStrings = document.getElementsByTagNameNS("*", "LineString")
    val paths = buildList {
        for (lineIndex in 0 until lineStrings.length) {
            val line = lineStrings.item(lineIndex) as? Element ?: continue
            val coordinatesNodes = line.getElementsByTagNameNS("*", "coordinates")
            for (nodeIndex in 0 until coordinatesNodes.length) {
                val text = coordinatesNodes.item(nodeIndex).textContent.orEmpty()
                val points = text.trim().split(Regex("\\s+")).mapNotNull { token ->
                    val values = token.split(',')
                    if (values.size < 2) return@mapNotNull null
                    val longitude = values[0].toDoubleOrNull() ?: return@mapNotNull null
                    val latitude = values[1].toDoubleOrNull() ?: return@mapNotNull null
                    if (!longitude.isFinite() || !latitude.isFinite() || longitude !in -180.0..180.0 || latitude !in -90.0..90.0) {
                        return@mapNotNull null
                    }
                    RouteCoordinate(latitude, longitude)
                }
                if (points.size >= 2) add(points)
            }
        }
    }
    val selectedPath = paths.maxByOrNull { points ->
        points.zipWithNext().sumOf { (first, second) -> distanceMeters(first, second) }
    } ?: throw IllegalArgumentException("KML tidak berisi jalur LineString yang bisa digunakan")
    require(selectedPath.size <= MAX_KML_POINTS) { "Jalur KML memiliki terlalu banyak titik" }
    return selectedPath
}

internal fun drivingRouteFromKml(points: List<RouteCoordinate>, fileName: String): DrivingRoute {
    require(points.size >= 2) { "Jalur KML harus memiliki minimal dua titik" }
    val distance = points.zipWithNext().sumOf { (first, second) -> distanceMeters(first, second) }
    require(distance > 0.0) { "Jalur KML tidak memiliki jarak" }
    return DrivingRoute(
        coordinates = points,
        distanceMeters = distance,
        durationSeconds = 0.0,
        steps = emptyList(),
        sourceFileName = fileName,
        isKmlRoute = true
    )
}
