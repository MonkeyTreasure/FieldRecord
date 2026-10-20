package com.fieldrecord.presentation.map

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class KmlRouteParserTest {
    @Test
    fun parsesLineStringCoordinatesAsLatitudeLongitude() {
        val kml = """
            <?xml version="1.0" encoding="UTF-8"?>
            <kml xmlns="http://www.opengis.net/kml/2.2"><Document><Placemark><LineString>
              <coordinates>112.75,-7.25,0 112.76,-7.26,3</coordinates>
            </LineString></Placemark></Document></kml>
        """.trimIndent()

        val points = parseKmlRouteCoordinates(kml)

        assertEquals(listOf(RouteCoordinate(-7.25, 112.75), RouteCoordinate(-7.26, 112.76)), points)
    }

    @Test
    fun rejectsKmlWithoutAUsableLineString() {
        val error = runCatching {
            parseKmlRouteCoordinates("<kml><Placemark><Point><coordinates>112,-7</coordinates></Point></Placemark></kml>")
        }.exceptionOrNull()

        assertTrue(error is IllegalArgumentException)
    }
}
