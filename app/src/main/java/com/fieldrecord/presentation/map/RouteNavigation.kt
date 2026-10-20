package com.fieldrecord.presentation.map

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

internal data class RouteCoordinate(val latitude: Double, val longitude: Double)

internal data class NavigationStep(
    val instruction: String,
    val maneuverType: String,
    val distanceFromStartMeters: Double,
    val location: RouteCoordinate
)

internal data class DrivingRoute(
    val coordinates: List<RouteCoordinate>,
    val distanceMeters: Double,
    val durationSeconds: Double,
    val steps: List<NavigationStep>,
    val sourceFileName: String? = null,
    val isKmlRoute: Boolean = false
)

internal data class RouteLineSegments(
    val traveled: List<RouteCoordinate>,
    val remaining: List<RouteCoordinate>
)

internal fun splitRouteLineAtProgress(route: DrivingRoute, progressMeters: Double): RouteLineSegments {
    if (route.coordinates.size < 2) return RouteLineSegments(route.coordinates, emptyList())
    val progress = progressMeters.coerceIn(0.0, route.distanceMeters)
    var cumulative = 0.0
    var splitIndex = 0
    for (index in 0 until route.coordinates.lastIndex) {
        val segmentLength = distanceMeters(route.coordinates[index], route.coordinates[index + 1])
        if (progress < cumulative + segmentLength) {
            splitIndex = index
            break
        }
        cumulative += segmentLength
        splitIndex = index + 1
    }
    return RouteLineSegments(
        traveled = route.coordinates.take(splitIndex + 1),
        remaining = route.coordinates.drop(splitIndex)
    )
}

internal fun osrmDrivingRouteUrl(origin: RouteCoordinate, destination: RouteCoordinate): String {
    fun coordinate(value: Double) = String.format(Locale.US, "%.6f", value)
    return "https://router.project-osrm.org/route/v1/driving/" +
        "${coordinate(origin.longitude)},${coordinate(origin.latitude)};" +
        "${coordinate(destination.longitude)},${coordinate(destination.latitude)}" +
        "?overview=full&geometries=geojson&steps=true"
}

internal suspend fun requestDrivingRoute(
    origin: RouteCoordinate,
    destination: RouteCoordinate
): DrivingRoute = withContext(Dispatchers.IO) {
    val connection = (URL(osrmDrivingRouteUrl(origin, destination)).openConnection() as HttpURLConnection).apply {
        requestMethod = "GET"
        connectTimeout = 12_000
        readTimeout = 20_000
        setRequestProperty("Accept", "application/json")
        setRequestProperty("User-Agent", "FieldRecord-Android")
    }
    try {
        val status = connection.responseCode
        if (status !in 200..299) throw IOException("Routing service HTTP $status")
        val body = connection.inputStream.bufferedReader().use { it.readText() }
        parseOsrmDrivingRoute(body)
    } finally {
        connection.disconnect()
    }
}

internal fun parseOsrmDrivingRoute(json: String): DrivingRoute {
    val response = JSONObject(json)
    if (response.optString("code") != "Ok") {
        throw IOException(response.optString("message", "Rute tidak ditemukan"))
    }
    val route = response.getJSONArray("routes").optJSONObject(0)
        ?: throw IOException("Rute tidak ditemukan")
    val coordinatesJson = route.getJSONObject("geometry").getJSONArray("coordinates")
    val coordinates = buildList {
        for (index in 0 until coordinatesJson.length()) {
            val point = coordinatesJson.getJSONArray(index)
            add(RouteCoordinate(point.getDouble(1), point.getDouble(0)))
        }
    }
    if (coordinates.size < 2) throw IOException("Geometri rute tidak lengkap")

    val leg = route.getJSONArray("legs").optJSONObject(0)
        ?: throw IOException("Langkah rute tidak tersedia")
    val stepsJson = leg.getJSONArray("steps")
    var distanceFromStart = 0.0
    val steps = buildList {
        for (index in 0 until stepsJson.length()) {
            val step = stepsJson.getJSONObject(index)
            val maneuver = step.getJSONObject("maneuver")
            val position = maneuver.getJSONArray("location")
            val type = maneuver.optString("type", "continue")
            val modifier = maneuver.optString("modifier", "")
            val roadName = step.optString("name", "")
            val instruction = formatManeuverInstruction(type, modifier, roadName)
            add(
                NavigationStep(
                    instruction = instruction,
                    maneuverType = type,
                    distanceFromStartMeters = distanceFromStart,
                    location = RouteCoordinate(position.getDouble(1), position.getDouble(0))
                )
            )
            distanceFromStart += step.optDouble("distance", 0.0)
        }
    }
    return DrivingRoute(
        coordinates = coordinates,
        distanceMeters = route.getDouble("distance"),
        durationSeconds = route.getDouble("duration"),
        steps = steps
    )
}

internal fun formatManeuverInstruction(type: String, modifier: String, roadName: String): String {
    if (type == "depart") return "Mulai perjalanan"
    if (type == "arrive") return "Tiba di tujuan"
    val direction = when (modifier.lowercase(Locale.ROOT)) {
        "slight left" -> "sedikit ke kiri"
        "left" -> "ke kiri"
        "sharp left" -> "tajam ke kiri"
        "slight right" -> "sedikit ke kanan"
        "right" -> "ke kanan"
        "sharp right" -> "tajam ke kanan"
        "uturn" -> "putar balik"
        "straight" -> "lurus"
        else -> ""
    }
    val action = when (type.lowercase(Locale.ROOT)) {
        "turn", "end of road" -> "Belok"
        "roundabout", "rotary", "roundabout turn" -> "Masuk bundaran"
        "merge" -> "Gabung"
        "fork" -> "Ambil percabangan"
        "on ramp", "off ramp", "ramp" -> "Ambil ramp"
        else -> "Lanjutkan"
    }
    val road = roadName.trim().takeIf(String::isNotEmpty)?.let { " ke $it" }.orEmpty()
    return listOf(action, direction).filter(String::isNotBlank).joinToString(" ") + road
}

internal fun routeProgressMeters(route: DrivingRoute, position: RouteCoordinate): Double {
    var travelled = 0.0
    var bestDistance = Double.POSITIVE_INFINITY
    var bestProgress = 0.0
    route.coordinates.forEachIndexed { index, point ->
        val distanceToPoint = distanceMeters(position, point)
        if (distanceToPoint < bestDistance) {
            bestDistance = distanceToPoint
            bestProgress = travelled
        }
        if (index < route.coordinates.lastIndex) {
            travelled += distanceMeters(point, route.coordinates[index + 1])
        }
    }
    return bestProgress.coerceIn(0.0, route.distanceMeters)
}

internal fun nextNavigationStep(route: DrivingRoute, progressMeters: Double): NavigationStep? =
    route.steps.firstOrNull { step ->
        step.maneuverType != "depart" && step.distanceFromStartMeters >= progressMeters
    } ?: route.steps.lastOrNull { it.maneuverType == "arrive" }

internal fun distanceMeters(first: RouteCoordinate, second: RouteCoordinate): Double {
    val earthRadius = 6_371_000.0
    val latitude1 = Math.toRadians(first.latitude)
    val latitude2 = Math.toRadians(second.latitude)
    val latitudeDelta = latitude2 - latitude1
    val longitudeDelta = Math.toRadians(second.longitude - first.longitude)
    val a = sin(latitudeDelta / 2) * sin(latitudeDelta / 2) +
        cos(latitude1) * cos(latitude2) * sin(longitudeDelta / 2) * sin(longitudeDelta / 2)
    return earthRadius * 2 * atan2(sqrt(a), sqrt(1 - a))
}

internal fun formatRouteDistance(distanceMeters: Double): String =
    if (distanceMeters < 1000) String.format(Locale.US, "%.0f m", distanceMeters.coerceAtLeast(0.0))
    else String.format(Locale.US, "%.1f km", distanceMeters / 1000.0)

internal fun formatRouteDuration(durationSeconds: Double): String {
    val minutes = (durationSeconds.coerceAtLeast(0.0) / 60.0).toInt()
    return if (minutes < 60) "$minutes mnt" else "${minutes / 60} j ${minutes % 60} mnt"
}
