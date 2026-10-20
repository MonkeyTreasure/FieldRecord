package com.fieldrecord.presentation.map

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class RouteNavigationTest {
    @Test
    fun routeUrlUsesLongitudeThenLatitudeForEachWaypoint() {
        val url = osrmDrivingRouteUrl(
            RouteCoordinate(-7.25, 112.75),
            RouteCoordinate(-7.26, 112.76)
        )
        assertEquals(
            "https://router.project-osrm.org/route/v1/driving/112.750000,-7.250000;112.760000,-7.260000?overview=full&geometries=geojson&steps=true",
            url
        )
    }

    @Test
    fun stepInstructionUsesIndonesianTurnAndRoadName() {
        assertEquals("Belok ke kanan ke Jalan Merdeka", formatManeuverInstruction("turn", "right", "Jalan Merdeka"))
        assertEquals("Tiba di tujuan", formatManeuverInstruction("arrive", "", ""))
    }

    @Test
    fun routeProgressAndNextInstructionAdvanceAlongGeometry() {
        val route = DrivingRoute(
            coordinates = listOf(
                RouteCoordinate(0.0, 0.0),
                RouteCoordinate(0.0, 0.001),
                RouteCoordinate(0.0, 0.002)
            ),
            distanceMeters = 222.4,
            durationSeconds = 120.0,
            steps = listOf(
                NavigationStep("Mulai perjalanan", "depart", 0.0, RouteCoordinate(0.0, 0.0)),
                NavigationStep("Belok ke kanan", "turn", 100.0, RouteCoordinate(0.0, 0.001)),
                NavigationStep("Tiba di tujuan", "arrive", 200.0, RouteCoordinate(0.0, 0.002))
            )
        )

        val progress = routeProgressMeters(route, RouteCoordinate(0.0, 0.001))
        assertEquals(111.2, progress, 2.0)
        assertEquals("Tiba di tujuan", nextNavigationStep(route, progress)?.instruction)
        assertNotNull(nextNavigationStep(route, 0.0))
    }
}
