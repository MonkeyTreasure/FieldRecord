package com.fieldrecord.presentation.map

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color as AndroidColor
import android.graphics.PointF
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Close
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.fieldrecord.domain.model.FieldPhoto
import com.fieldrecord.domain.model.extractPhotoTags
import com.fieldrecord.domain.model.photoMarkerColorArgb
import com.fieldrecord.domain.model.photoPinColorArgb
import com.fieldrecord.presentation.PhotoNoteText
import com.fieldrecord.presentation.photo.PhotoFullscreenViewer
import coil.compose.rememberAsyncImagePainter
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale
import com.fieldrecord.location.LocationKalmanFilter
import com.google.android.gms.location.*
import android.location.Location
import org.maplibre.android.annotations.IconFactory
import org.maplibre.android.annotations.MarkerOptions
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.Style
import kotlinx.coroutines.delay

@Composable
fun MapScreen(
    modifier: Modifier = Modifier,
    photos: List<FieldPhoto>,
    onPhotoClick: (FieldPhoto) -> Unit,
    onClusterClick: (List<Long>) -> Unit = {},
    onSavePhotoNote: suspend (Long, String?) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var mapLibreMapRef by remember { mutableStateOf<MapLibreMap?>(null) }
    var mapViewRef by remember { mutableStateOf<MapView?>(null) }
    var currentLocation by remember { mutableStateOf<LatLng?>(null) }
    val mapLayerSelection = remember(context) {
        val preferences = context.getSharedPreferences("map_layer_preferences", Context.MODE_PRIVATE)
        MapLayerSelection(object : MapLayerStore {
            override fun readSatellite(): Boolean = preferences.getBoolean("satellite_enabled", false)
            override fun writeSatellite(value: Boolean) {
                preferences.edit().putBoolean("satellite_enabled", value).apply()
            }
        })
    }
    var isSatellite by remember(mapLayerSelection) { mutableStateOf(mapLayerSelection.read()) }
    var isNorthLocked by remember { mutableStateOf(true) }
    var isFollowingLocation by remember { mutableStateOf(false) }
    var hasUserExploredMap by remember { mutableStateOf(false) }
    var cameraRefresh by remember { mutableIntStateOf(0) }
    var noteSearchQuery by remember { mutableStateOf("") }
    val noteSearchResults = remember(photos, noteSearchQuery) {
        val query = noteSearchQuery.trim()
        if (query.isBlank()) emptyList() else photos.filter { !it.note.isNullOrBlank() && it.note.contains(query, ignoreCase = true) }
            .sortedByDescending { it.timestamp }
    }
    val latestPhotos by rememberUpdatedState(photos)
    val latestOnPhotoClick by rememberUpdatedState(onPhotoClick)
    val latestOnClusterClick by rememberUpdatedState(onClusterClick)

    // Raster basemaps avoid the demo vector tiles' "Map data not yet available" at high zoom.
    val streetStyle = """{"version":8,"sources":{"osm-street":{"type":"raster","tiles":["https://tile.openstreetmap.org/{z}/{x}/{y}.png"],"tileSize":256,"attribution":"© OpenStreetMap contributors"}},"layers":[{"id":"osm-street-layer","type":"raster","source":"osm-street"}]}"""
    val satelliteStyle = """{"version":8,"sources":{"esri-imagery":{"type":"raster","tiles":["https://server.arcgisonline.com/ArcGIS/rest/services/World_Imagery/MapServer/tile/{z}/{y}/{x}"],"tileSize":256,"attribution":"Tiles © Esri"}},"layers":[{"id":"esri-imagery-layer","type":"raster","source":"esri-imagery"}]}"""

    DisposableEffect(lifecycleOwner, mapViewRef) {
        val map = mapViewRef
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_CREATE -> map?.onCreate(null)
                Lifecycle.Event.ON_START -> map?.onStart()
                Lifecycle.Event.ON_RESUME -> map?.onResume()
                Lifecycle.Event.ON_PAUSE -> map?.onPause()
                Lifecycle.Event.ON_STOP -> map?.onStop()
                Lifecycle.Event.ON_DESTROY -> map?.onDestroy()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        if (lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) map?.onResume()
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer); map?.onStop() }
    }

    // Fused Location Provider with Kalman Filter
    DisposableEffect(context) {
        val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
        val kalmanFilter = LocationKalmanFilter()
        
        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 500L)
            .setMinUpdateIntervalMillis(200L)
            .build()

        var firstFix = true
        val locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                result.lastLocation?.let { rawLocation ->
                    val (filteredLat, filteredLon) = kalmanFilter.filter(
                        rawLocation.latitude,
                        rawLocation.longitude,
                        rawLocation.accuracy
                    )
                    val newLatLng = LatLng(filteredLat, filteredLon)
                    currentLocation = newLatLng
                    
                    mapLibreMapRef?.let { map ->
                        if (firstFix) {
                            if (!hasUserExploredMap) {
                                map.animateCamera(CameraUpdateFactory.newLatLngZoom(newLatLng, 16.0))
                            }
                            firstFix = false
                        } else if (isFollowingLocation) {
                            map.animateCamera(CameraUpdateFactory.newLatLng(newLatLng))
                        }
                    }
                }
            }
        }

        val hasPermission = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        if (hasPermission) {
            try {
                fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, context.mainLooper)
            } catch (_: SecurityException) {}
        }

        onDispose {
            fusedLocationClient.removeLocationUpdates(locationCallback)
        }
    }

    LaunchedEffect(noteSearchQuery, noteSearchResults, mapLibreMapRef) {
        val matchedPhoto = noteSearchResults.firstOrNull()
        if (noteSearchQuery.isNotBlank() && matchedPhoto != null) {
            isFollowingLocation = false
            mapLibreMapRef?.animateCamera(
                CameraUpdateFactory.newLatLngZoom(LatLng(matchedPhoto.latitude, matchedPhoto.longitude), 17.0)
            )
        }
    }

    Box(modifier = modifier.background(Color(0xFFE8F0E8))) {
        AndroidView(
            factory = { ctx ->
                MapView(ctx).apply {
                    mapViewRef = this
                    onCreate(null)
                    getMapAsync { map ->
                        mapLibreMapRef = map
                        val initialStyle = if (isSatellite) {
                            Style.Builder().fromJson(satelliteStyle)
                        } else {
                            Style.Builder().fromJson(streetStyle)
                        }
                        map.setStyle(initialStyle)
                        map.uiSettings.isCompassEnabled = true
                        map.uiSettings.isZoomGesturesEnabled = true
                        map.uiSettings.isRotateGesturesEnabled = true
                        map.setMinZoomPreference(3.0)
                        map.setMaxZoomPreference(17.0)
                        map.addOnCameraMoveStartedListener { reason ->
                            if (reason == MapLibreMap.OnCameraMoveStartedListener.REASON_API_GESTURE) {
                                hasUserExploredMap = true
                                isFollowingLocation = false
                            }
                        }
                        map.addOnCameraIdleListener { cameraRefresh++ }
                        map.setOnMarkerClickListener { marker ->
                                                    val markerTitle = marker.title.orEmpty()
                                                    when {
                                                        markerTitle.startsWith("photo:") -> {
                                                            val photoId = markerTitle.removePrefix("photo:").toLongOrNull()
                                                            val photo = latestPhotos.firstOrNull { it.id == photoId }
                                                            if (photo != null) latestOnPhotoClick(photo)
                                                            true
                                                        }
                                                        markerTitle.startsWith("cluster:") -> {
                                                            val photoIds = markerTitle.removePrefix("cluster:")
                                                                .split(',')
                                                                .mapNotNull(String::toLongOrNull)
                                                            if (photoIds.isNotEmpty()) latestOnClusterClick(photoIds)
                                                            true
                                                        }
                                                        else -> false
                                                    }
                                                }
                    }
                }
            },
            modifier = Modifier.fillMaxSize(),
            update = { _ ->
                cameraRefresh.let { _ ->
                    mapLibreMapRef?.let { map ->
                        if (isNorthLocked && map.cameraPosition.bearing != 0.0) {
                            map.animateCamera(CameraUpdateFactory.bearingTo(0.0))
                        }

                        map.clear()
                        currentLocation?.let { latLng ->
                            val blueDotBitmap = createDotBitmap(AndroidColor.rgb(30, 105, 235), 48)
                            val icon = IconFactory.getInstance(context).fromBitmap(blueDotBitmap)
                            map.addMarker(
                                MarkerOptions().position(latLng).icon(icon).title("Lokasi Saya")
                            )
                        }
                        renderPhotoMarkers(map, context, photos)
                    }
                }
            }
        )

        // Search by photo note; matching notes appear as tappable capsules directly below.
        Column(
            modifier = Modifier.align(Alignment.TopCenter).fillMaxWidth().statusBarsPadding().padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            TextField(
                value = noteSearchQuery,
                onValueChange = { noteSearchQuery = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                textStyle = androidx.compose.ui.text.TextStyle(color = Color.Black),
                placeholder = { Text("Cari catatan foto…", color = Color.DarkGray) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Cari catatan foto") },
                trailingIcon = if (noteSearchQuery.isNotEmpty()) ({
                    IconButton(onClick = { noteSearchQuery = "" }) {
                        Icon(Icons.Default.Close, contentDescription = "Hapus pencarian")
                    }
                }) else null,
                shape = androidx.compose.foundation.shape.RoundedCornerShape(24.dp),
                colors = TextFieldDefaults.textFieldColors(
                    backgroundColor = Color.White,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                )
            )
            if (noteSearchQuery.isNotBlank()) {
                Spacer(Modifier.height(6.dp))
                Surface(
                    modifier = Modifier.fillMaxWidth().heightIn(max = 320.dp),
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
                    color = Color.White,
                    elevation = 6.dp
                ) {
                    if (noteSearchResults.isEmpty()) {
                        Text("Catatan tidak ditemukan", modifier = Modifier.padding(14.dp), color = Color.DarkGray)
                    } else {
                        Column(modifier = Modifier.verticalScroll(rememberScrollState()).padding(8.dp)) {
                            noteSearchResults.forEachIndexed { index, photo ->
                                Column(
                                    modifier = Modifier.fillMaxWidth()
                                        .padding(vertical = 4.dp)
                                        .background(Color(0xFFF4F7F4), androidx.compose.foundation.shape.RoundedCornerShape(12.dp))
                                        .clickable { onPhotoClick(photo) }
                                        .padding(horizontal = 12.dp, vertical = 10.dp)
                                ) {
                                    PhotoNoteText(
                                        note = photo.note.orEmpty(),
                                        defaultColor = Color.Black,
                                        style = MaterialTheme.typography.body2
                                    )
                                }
                                if (index < noteSearchResults.lastIndex) Spacer(Modifier.height(4.dp))
                            }
                        }
                    }
                }
            }        }

        Column(
            modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            horizontalAlignment = Alignment.End
        ) {
            // Layer switcher FAB (Street vs Satellite)
            FloatingActionButton(
                onClick = {
                    isSatellite = mapLayerSelection.toggle()
                    val nextStyle = if (isSatellite) {
                        Style.Builder().fromJson(satelliteStyle)
                    } else {
                        Style.Builder().fromJson(streetStyle)
                    }
                    mapLibreMapRef?.setStyle(nextStyle)
                },
                backgroundColor = Color.White,
                contentColor = Color(0xFF2E7D32)
            ) { Icon(Icons.Default.Layers, contentDescription = if (isSatellite) "Lapisan Satelit (ketuk untuk Street)" else "Lapisan Street (ketuk untuk Satelit)") }

            // North lock / Free look toggle FAB
            FloatingActionButton(
                onClick = {
                    isNorthLocked = !isNorthLocked
                    if (isNorthLocked) {
                        mapLibreMapRef?.animateCamera(CameraUpdateFactory.bearingTo(0.0))
                    }
                },
                backgroundColor = if (isNorthLocked) Color.White else Color.Blue,
                contentColor = if (isNorthLocked) Color.Black else Color.White
            ) {
                Icon(
                    imageVector = Icons.Default.Navigation,
                    contentDescription = if (isNorthLocked) "North-lock aktif; ketuk untuk free look" else "Free look aktif; ketuk untuk north-lock"
                )
            }

            // My location recenter FAB
            FloatingActionButton(
                onClick = {
                    currentLocation?.let { latLng ->
                        isFollowingLocation = true
                        isNorthLocked = true
                        mapLibreMapRef?.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 16.0))
                        mapLibreMapRef?.animateCamera(CameraUpdateFactory.bearingTo(0.0))
                    }
                },
                backgroundColor = if (isFollowingLocation) Color(0xFF1E69EB) else Color.White,
                contentColor = if (isFollowingLocation) Color.White else Color(0xFF1E69EB)
            ) {
                Icon(
                    Icons.Default.MyLocation,
                    contentDescription = if (isFollowingLocation) "Mengikuti lokasi aktif; geser peta untuk menjelajah" else "Pusatkan lokasi; ikuti lokasi saya"
                )
            }
        }

    }
}

private fun renderPhotoMarkers(map: MapLibreMap, context: Context, photos: List<FieldPhoto>) {
    val validPhotos = photos.filter { photo ->
        photo.latitude.isFinite() && photo.longitude.isFinite() &&
            photo.latitude in -90.0..90.0 && photo.longitude in -180.0..180.0 &&
            !(photo.latitude == 0.0 && photo.longitude == 0.0)
    }
    if (validPhotos.isEmpty()) return

    val currentZoom = map.cameraPosition.zoom
    if (currentZoom >= 16.0) {
        // Zoom in tinggi (>= 16): Tampilkan titik merah individu
        validPhotos.forEach { photo ->
            val iconBitmap = createDotBitmap(AndroidColor.rgb(220, 45, 45), 36)
            val icon = IconFactory.getInstance(context).fromBitmap(iconBitmap)
            map.addMarker(
                MarkerOptions()
                    .position(LatLng(photo.latitude, photo.longitude))
                    .icon(icon)
                    .title("photo:${photo.id}")
                    .snippet(photo.note.orEmpty())
            )
        }
        return
    }

    // Zoom out (< 16): Cluster berdasarkan jarak layar (screen pixel distance)
    // Seiring zoom out, jarak geografis per pixel membesar sehingga marker yang tampak berdekatan di layar otomatis menyatu
    val projection = map.projection
    val clusterRadiusPx = 100.0 // Radius cluster dalam pixel di layar
    val clusters = mutableListOf<MutableList<FieldPhoto>>()

    validPhotos.forEach { photo ->
        val screenPt = projection.toScreenLocation(LatLng(photo.latitude, photo.longitude))
        val cluster = clusters.firstOrNull { members ->
            val first = members.first()
            val firstPt = projection.toScreenLocation(LatLng(first.latitude, first.longitude))
            val dx = screenPt.x - firstPt.x
            val dy = screenPt.y - firstPt.y
            Math.hypot(dx.toDouble(), dy.toDouble()) <= clusterRadiusPx
        }
        if (cluster == null) clusters += mutableListOf(photo) else cluster += photo
    }

    clusters.forEach { cluster ->
        val centerLat = cluster.map { it.latitude }.average()
        val centerLon = cluster.map { it.longitude }.average()
        val isCluster = cluster.size > 1

        val primaryTags = cluster.map { extractPhotoTags(it.note).firstOrNull() }
        val markerColor = if (isCluster) {
            photoMarkerColorArgb(primaryTags)
        } else {
            photoPinColorArgb(cluster.first().note)
        }
        val iconBitmap = if (!isCluster) {
            createDotBitmap(markerColor, 36)
        } else {
            createClusterBitmap(cluster.size, markerColor)
        }
        val icon = IconFactory.getInstance(context).fromBitmap(iconBitmap)
        map.addMarker(
            MarkerOptions()
                .position(LatLng(centerLat, centerLon))
                .icon(icon)
                .title(
                    if (!isCluster) "photo:${cluster.first().id}"
                    else "cluster:${cluster.joinToString(",") { it.id.toString() }}"
                )
        )
    }
}

private fun createClusterBitmap(count: Int, color: Int): Bitmap {
    val size = 68
    val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
        this.color = color
        style = android.graphics.Paint.Style.FILL
    }
    canvas.drawCircle(size / 2f, size / 2f, size * 0.43f, paint)
    paint.color = AndroidColor.WHITE
    paint.style = android.graphics.Paint.Style.STROKE
    paint.strokeWidth = 4f
    canvas.drawCircle(size / 2f, size / 2f, size * 0.43f, paint)
    paint.style = android.graphics.Paint.Style.FILL
    paint.typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
    paint.textAlign = android.graphics.Paint.Align.CENTER
    paint.textSize = if (count >= 100) 20f else 25f
    val label = count.toString()
    val baseline = size / 2f - (paint.ascent() + paint.descent()) / 2f
    canvas.drawText(label, size / 2f, baseline, paint)
    return bitmap
}

private fun createDotBitmap(color: Int, size: Int): Bitmap {
    val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply { this.color = color }
    canvas.drawCircle(size / 2f, size / 2f, size * 0.35f, paint)
    paint.color = AndroidColor.WHITE
    paint.style = android.graphics.Paint.Style.STROKE
    paint.strokeWidth = size * 0.08f
    canvas.drawCircle(size / 2f, size / 2f, size * 0.35f, paint)
    return bitmap
}
