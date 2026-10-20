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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.border
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.ui.text.style.TextOverflow
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.provider.OpenableColumns
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
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
import com.fieldrecord.LocalAppIconColor
import com.fieldrecord.data.local.PhotoTagColorStore
import com.fieldrecord.domain.model.FieldPhoto
import com.fieldrecord.domain.model.PhotoTagFilter
import com.fieldrecord.domain.model.matchesPhotoTagFilter
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
import org.maplibre.android.annotations.PolylineOptions
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.geometry.LatLngBounds
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.Style
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun MapScreen(
    modifier: Modifier = Modifier,
    photos: List<FieldPhoto>,
    onPhotoClick: (FieldPhoto) -> Unit,
    onClusterClick: (List<Long>) -> Unit = {},
    onSavePhotoNote: suspend (Long, String?) -> Unit,
    tagFilter: PhotoTagFilter? = null,
    onClearTagFilter: () -> Unit = {},
    mapOpenRequest: Int = 0
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var mapLibreMapRef by remember { mutableStateOf<MapLibreMap?>(null) }
    var mapViewRef by remember { mutableStateOf<MapView?>(null) }
    var currentLocation by remember { mutableStateOf<LatLng?>(null) }
    val temporaryGuidePins = remember { mutableStateListOf<TemporaryGuidePin>() }
    val coroutineScope = rememberCoroutineScope()
    var activeRoute by remember { mutableStateOf<DrivingRoute?>(null) }
    var pendingKmlRoute by remember { mutableStateOf<DrivingRoute?>(null) }
    var isRouteLoading by remember { mutableStateOf(false) }
    var isKmlLoading by remember { mutableStateOf(false) }
    var routeError by remember { mutableStateOf<String?>(null) }
    var selectedGuidePinId by remember { mutableStateOf<Long?>(null) }
    var selectedGuidePhoto by remember { mutableStateOf<FieldPhoto?>(null) }
    var isFollowingLocation by remember { mutableStateOf(false) }
    val selectedGuidePin = temporaryGuidePins.firstOrNull { it.id == selectedGuidePinId }
    val kmlPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            coroutineScope.launch {
                isKmlLoading = true
                routeError = null
                try {
                    val displayName = context.contentResolver.query(
                        uri,
                        arrayOf(OpenableColumns.DISPLAY_NAME),
                        null,
                        null,
                        null
                    )?.use { cursor ->
                        if (cursor.moveToFirst()) cursor.getString(0) else null
                    } ?: "rute.kml"
                    require(displayName.endsWith(".kml", ignoreCase = true)) { "Pilih file dengan format .kml" }
                    val route = withContext(Dispatchers.IO) {
                        val kmlText = context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
                            ?: throw IllegalArgumentException("File KML tidak dapat dibuka")
                        drivingRouteFromKml(parseKmlRouteCoordinates(kmlText), displayName)
                    }
                    pendingKmlRoute = route
                    android.widget.Toast.makeText(context, "Rute KML siap untuk ditinjau", android.widget.Toast.LENGTH_LONG).show()
                } catch (error: Exception) {
                    val message = error.message ?: "File KML tidak dapat dibaca"
                    routeError = message
                    android.widget.Toast.makeText(context, message, android.widget.Toast.LENGTH_LONG).show()
                } finally {
                    isKmlLoading = false
                }
            }
        }
    }
    val latestOnGuidePinCreated = rememberUpdatedState<(LatLng) -> Unit>(newValue = { point ->
        val pin = TemporaryGuidePin(System.nanoTime(), point.latitude, point.longitude)
        temporaryGuidePins.add(pin)
        selectedGuidePinId = pin.id
        selectedGuidePhoto = null
        activeRoute = null
        routeError = null
        isFollowingLocation = false
    })
    val latestOnGuidePinSelected = rememberUpdatedState<(Long) -> Unit>(newValue = { pinId ->
        selectedGuidePinId = temporaryGuidePins.firstOrNull { it.id == pinId }?.id
        selectedGuidePhoto = null
        activeRoute = null
        routeError = null
    })
    val latestOnGuidePhotoSelected = rememberUpdatedState<(FieldPhoto) -> Unit>(newValue = { photo ->
        selectedGuidePinId = null
        selectedGuidePhoto = photo
        activeRoute = null
        routeError = null
    })
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
    var hasUserExploredMap by remember { mutableStateOf(false) }
    var cameraRefresh by remember { mutableIntStateOf(0) }
    var mapZoom by remember { mutableFloatStateOf(16f) }
    var noteSearchQuery by remember { mutableStateOf("") }
    val tagFilteredPhotos = remember(photos, tagFilter) {
        if (tagFilter == null) photos else photos.filter { matchesPhotoTagFilter(it.note, tagFilter) }
    }
    val noteSearchResults = remember(tagFilteredPhotos, noteSearchQuery) {
        val query = noteSearchQuery.trim()
        if (query.isBlank()) emptyList() else tagFilteredPhotos.filter { !it.note.isNullOrBlank() && it.note.contains(query, ignoreCase = true) }
            .sortedByDescending { it.timestamp }
    }
    LaunchedEffect(tagFilter) {
        noteSearchQuery = ""
    }
    val latestPhotos by rememberUpdatedState(photos)
    val latestOnPhotoClick by rememberUpdatedState(onPhotoClick)
    val latestOnClusterClick by rememberUpdatedState(onClusterClick)

    // Handle deep link from shared location
    val deepLinkLat = remember { mutableStateOf<Double?>(null) }
    val deepLinkLon = remember { mutableStateOf<Double?>(null) }
    val deepLinkNote = remember { mutableStateOf<String>("") }
    LaunchedEffect(Unit) {
        val prefs = context.getSharedPreferences("field_record_deep_link", Context.MODE_PRIVATE)
        val latStr = prefs.getString("deep_link_lat", "") ?: ""
        val lonStr = prefs.getString("deep_link_lon", "") ?: ""
        val noteStr = prefs.getString("deep_link_note", "") ?: ""
        if (latStr.isNotBlank() && lonStr.isNotBlank()) {
            deepLinkLat.value = latStr.toDoubleOrNull()
            deepLinkLon.value = lonStr.toDoubleOrNull()
            deepLinkNote.value = noteStr
            prefs.edit().clear().apply()
        }
    }

    // Animate to deep link location when map is ready
    LaunchedEffect(mapLibreMapRef, deepLinkLat.value, deepLinkLon.value) {
        if (deepLinkLat.value != null && deepLinkLon.value != null && mapLibreMapRef != null) {
            val latLng = LatLng(deepLinkLat.value!!, deepLinkLon.value!!)
            mapLibreMapRef!!.animateCamera(
                CameraUpdateFactory.newLatLngZoom(latLng, 16.0),
                1000
            )
            // Show a toast or marker for the shared location
            val marker = MarkerOptions()
                .position(latLng)
                .title("Shared Location")
                .snippet(deepLinkNote.value)
            mapLibreMapRef!!.addMarker(marker)
            if (deepLinkNote.value.isNotBlank()) {
                // Show note in a toast or bottom sheet
                android.widget.Toast.makeText(context, "Dibagikan ke: ${deepLinkNote.value}", android.widget.Toast.LENGTH_LONG).show()
            }
            deepLinkLat.value = null
            deepLinkLon.value = null
            deepLinkNote.value = ""
        }
    }

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
                                map.animateCamera(CameraUpdateFactory.newLatLngZoom(newLatLng, 12.0))
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

    LaunchedEffect(mapOpenRequest, mapLibreMapRef) {
        if (mapOpenRequest > 0) {
            mapLibreMapRef?.animateCamera(CameraUpdateFactory.zoomTo(12.0))
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

    LaunchedEffect(tagFilter, tagFilteredPhotos, mapLibreMapRef) {
        val firstTaggedPhoto = tagFilteredPhotos.firstOrNull()
        if (tagFilter != null && firstTaggedPhoto != null) {
            isFollowingLocation = false
            mapLibreMapRef?.animateCamera(
                CameraUpdateFactory.newLatLngZoom(LatLng(firstTaggedPhoto.latitude, firstTaggedPhoto.longitude), 16.0)
            )
        }
    }

    Box(modifier = modifier.background(MaterialTheme.colors.background)) {
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
                        map.moveCamera(CameraUpdateFactory.zoomTo(12.0))
                        mapZoom = 12f
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
                        map.addOnCameraIdleListener {
                            mapZoom = map.cameraPosition.zoom.toFloat()
                            cameraRefresh++
                        }
                        map.setOnMarkerClickListener { marker ->
                                                    val markerTitle = marker.title.orEmpty()
                                                    when {
                                                        markerTitle.startsWith("guide-pin:") -> {
                                                            markerTitle.removePrefix("guide-pin:").toLongOrNull()?.let(latestOnGuidePinSelected.value)
                                                            true
                                                        }
                                                        markerTitle.startsWith("photo:") -> {
                                                            val photoId = markerTitle.removePrefix("photo:").toLongOrNull()
                                                            val photo = latestPhotos.firstOrNull { it.id == photoId }
                                                            if (photo != null) latestOnGuidePhotoSelected.value(photo)
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
                        map.addOnMapLongClickListener { point ->
                            latestOnGuidePinCreated.value(point)
                            true
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
                        activeRoute?.let { route ->
                            val progress = currentLocation?.let { location ->
                                routeProgressMeters(route, RouteCoordinate(location.latitude, location.longitude))
                            } ?: 0.0
                            val lineSegments = splitRouteLineAtProgress(route, progress)
                            if (lineSegments.remaining.size >= 2) {
                                map.addPolyline(
                                    PolylineOptions()
                                        .addAll(lineSegments.remaining.map { LatLng(it.latitude, it.longitude) })
                                        .color(AndroidColor.rgb(220, 45, 45))
                                        .width(8f)
                                )
                            }
                            if (lineSegments.traveled.size >= 2) {
                                map.addPolyline(
                                    PolylineOptions()
                                        .addAll(lineSegments.traveled.map { LatLng(it.latitude, it.longitude) })
                                        .color(AndroidColor.rgb(46, 125, 50))
                                        .width(8f)
                                )
                            }
                        }
                        val filteredPhotos = if (noteSearchQuery.isBlank()) tagFilteredPhotos else noteSearchResults
                        renderPhotoMarkers(map, context, filteredPhotos)
                        temporaryGuidePins.forEach { pin ->
                            map.addMarker(
                                MarkerOptions()
                                    .position(LatLng(pin.latitude, pin.longitude))
                                    .title("guide-pin:${pin.id}")
                                    .snippet("Titik panduan sementara")
                            )
                        }
                    }
                }
            }
        )

        // Search by photo note; matching notes appear as tappable capsules directly below.
        Column(
            modifier = Modifier.align(Alignment.TopCenter).fillMaxWidth().statusBarsPadding().padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            tagFilter?.let { activeFilter ->
                Surface(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp),
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(20.dp),
                    color = MaterialTheme.colors.surface,
                    elevation = 4.dp
                ) {
                    Row(
                        modifier = Modifier.padding(start = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val filterType = when (activeFilter.kind) {
                            com.fieldrecord.domain.model.PhotoTagKind.CATEGORY -> "Kategori"
                            com.fieldrecord.domain.model.PhotoTagKind.BLOCK -> "Blok"
                            com.fieldrecord.domain.model.PhotoTagKind.LOCATION -> "Lokasi"
                        }
                        Text(
                            text = "$filterType: ${activeFilter.label}",
                            modifier = Modifier.weight(1f),
                            color = MaterialTheme.colors.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        IconButton(onClick = onClearTagFilter) {
                            Icon(Icons.Default.Close, contentDescription = "Hapus filter tag", tint = LocalAppIconColor.current)
                        }
                    }
                }
            }
            TextField(
                value = noteSearchQuery,
                onValueChange = { noteSearchQuery = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                textStyle = androidx.compose.ui.text.TextStyle(color = MaterialTheme.colors.onSurface),
                placeholder = { Text("Cari catatan foto…", color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f)) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Cari catatan foto", tint = Color.White) },
                trailingIcon = if (noteSearchQuery.isNotEmpty()) ({
                    IconButton(onClick = { noteSearchQuery = "" }) {
                        Icon(Icons.Default.Close, contentDescription = "Hapus pencarian")
                    }
                }) else null,
                shape = androidx.compose.foundation.shape.RoundedCornerShape(24.dp),
                colors = TextFieldDefaults.textFieldColors(
                    backgroundColor = Color(0xFF808080).copy(alpha = 0.5f),
                    cursorColor = Color.White,
                    placeholderColor = Color.White.copy(alpha = 0.75f),
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                )
            )
            if (noteSearchQuery.isNotBlank()) {
                Spacer(Modifier.height(6.dp))
                Surface(
                    modifier = Modifier.fillMaxWidth().heightIn(max = 320.dp),
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
                    color = MaterialTheme.colors.surface,
                    elevation = 6.dp
                ) {
                    if (noteSearchResults.isEmpty()) {
                        Text("Catatan tidak ditemukan", modifier = Modifier.padding(14.dp), color = MaterialTheme.colors.onSurface.copy(alpha = 0.65f))
                    } else {
                        Column(modifier = Modifier.verticalScroll(rememberScrollState()).padding(8.dp)) {
                            noteSearchResults.forEachIndexed { index, photo ->
                                Column(
                                    modifier = Modifier.fillMaxWidth()
                                        .padding(vertical = 4.dp)
                                        .background(MaterialTheme.colors.background, androidx.compose.foundation.shape.RoundedCornerShape(12.dp))
                                        .clickable { onPhotoClick(photo) }
                                        .padding(horizontal = 12.dp, vertical = 10.dp)
                                ) {
                                    PhotoNoteText(
                                        note = photo.note.orEmpty(),
                                        defaultColor = MaterialTheme.colors.onSurface,
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
            Surface(
                modifier = Modifier
                    .size(56.dp)
                    .border(1.5.dp, Color.White, CircleShape)
                    .clickable(enabled = !isKmlLoading) {
                        kmlPicker.launch(
                            arrayOf(
                                "application/vnd.google-earth.kml+xml",
                                "application/xml",
                                "text/xml",
                                "application/octet-stream"
                            )
                        )
                    },
                shape = CircleShape,
                color = MaterialTheme.colors.surface,
                elevation = 6.dp
            ) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    if (isKmlLoading) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                    } else {
                        Icon(
                            Icons.Default.FolderOpen,
                            contentDescription = "Import rute KML",
                            tint = LocalAppIconColor.current
                        )
                    }
                }
            }

            // Round map-zoom selector, styled like the camera zoom badge, directly above map-layer toggle.
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .background(MaterialTheme.colors.surface, CircleShape)
                    .border(1.5.dp, Color.White, CircleShape)
                    .clickable {
                        val presets = listOf(5f, 8f, 11f, 14f, 17f)
                        val nextZoom = presets.firstOrNull { it > mapZoom + 0.25f } ?: presets.first()
                        mapLibreMapRef?.animateCamera(CameraUpdateFactory.zoomTo(nextZoom.toDouble()))
                    },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "${String.format(Locale.US, "%.1f", mapZoom)}×",
                    color = Color.White,
                    style = MaterialTheme.typography.caption
                )
            }

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
                backgroundColor = MaterialTheme.colors.surface,
                contentColor = LocalAppIconColor.current
            ) { Icon(Icons.Default.Layers, contentDescription = if (isSatellite) "Lapisan Satelit (ketuk untuk Street)" else "Lapisan Street (ketuk untuk Satelit)") }

            // North lock / Free look toggle FAB
            FloatingActionButton(
                onClick = {
                    isNorthLocked = !isNorthLocked
                    if (isNorthLocked) {
                        mapLibreMapRef?.animateCamera(CameraUpdateFactory.bearingTo(0.0))
                    }
                },
                backgroundColor = MaterialTheme.colors.primary,
                contentColor = Color.White
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
                backgroundColor = MaterialTheme.colors.primary,
                contentColor = Color.White
            ) {
                Icon(
                    Icons.Default.MyLocation,
                    contentDescription = if (isFollowingLocation) "Mengikuti lokasi aktif; geser peta untuk menjelajah" else "Pusatkan lokasi; ikuti lokasi saya"
                )
            }
        }

        val photoGuideTarget = selectedGuidePhoto?.let { photo ->
            TemporaryGuidePin(photo.id, photo.latitude, photo.longitude)
        }
        val guideTarget = selectedGuidePin ?: photoGuideTarget
        guideTarget?.let { target ->
            val photo = selectedGuidePhoto
            Surface(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(start = 12.dp, end = 88.dp, bottom = 16.dp)
                    .fillMaxWidth(),
                shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
                color = MaterialTheme.colors.surface,
                elevation = 8.dp
            ) {
                Column(Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                if (photo == null) "Pin tujuan" else "Lokasi foto",
                                style = MaterialTheme.typography.subtitle2,
                                color = MaterialTheme.colors.onSurface
                            )
                            Text(
                                String.format(Locale.US, "%.6f, %.6f", target.latitude, target.longitude),
                                style = MaterialTheme.typography.caption,
                                color = MaterialTheme.colors.onSurface.copy(alpha = 0.72f)
                            )
                        }
                        IconButton(onClick = {
                            selectedGuidePinId = null
                            selectedGuidePhoto = null
                        }) {
                            Icon(Icons.Default.Close, contentDescription = "Tutup panduan lokasi")
                        }
                    }
                    Text(
                        "Koordinat GPS dan tujuan dikirim ke layanan rute untuk menghitung jalur kendaraan.",
                        style = MaterialTheme.typography.caption,
                        color = MaterialTheme.colors.onSurface.copy(alpha = 0.65f)
                    )
                    if (routeError != null) {
                        Text(
                            routeError.orEmpty(),
                            color = MaterialTheme.colors.error,
                            style = MaterialTheme.typography.caption,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (photo != null) {
                            TextButton(onClick = { latestOnPhotoClick(photo) }) {
                                Text("Buka foto")
                            }
                            Spacer(Modifier.width(8.dp))
                        }
                        Button(
                            enabled = !isRouteLoading,
                            onClick = {
                                val origin = currentLocation
                                if (origin == null) {
                                    routeError = "Lokasi GPS belum tersedia"
                                } else {
                                    routeError = null
                                    isRouteLoading = true
                                    coroutineScope.launch {
                                        try {
                                            val route = requestDrivingRoute(
                                                RouteCoordinate(origin.latitude, origin.longitude),
                                                RouteCoordinate(target.latitude, target.longitude)
                                            )
                                            activeRoute = route
                                            selectedGuidePinId = null
                                            selectedGuidePhoto = null
                                            isFollowingLocation = true
                                            mapLibreMapRef?.let { fitMapToRoute(it, route) }
                                        } catch (error: Exception) {
                                            routeError = error.message ?: "Rute tidak dapat dihitung"
                                        } finally {
                                            isRouteLoading = false
                                        }
                                    }
                                }
                            }
                        ) {
                            Icon(Icons.Default.Navigation, contentDescription = null)
                            Spacer(Modifier.width(4.dp))
                            Text(if (isRouteLoading) "Mencari rute…" else "Mulai navigasi")
                        }
                    }
                }
            }
        }
        activeRoute?.let { route ->
            val progress = currentLocation?.let { location ->
                routeProgressMeters(route, RouteCoordinate(location.latitude, location.longitude))
            } ?: 0.0
            val remaining = (route.distanceMeters - progress).coerceAtLeast(0.0)
            val progressRatio = if (route.distanceMeters > 0.0) (progress / route.distanceMeters).coerceIn(0.0, 1.0) else 0.0
            val etaSeconds = route.durationSeconds * (1.0 - progressRatio)
            val nextStep = nextNavigationStep(route, progress)
            val distanceToNext = ((nextStep?.distanceFromStartMeters ?: progress) - progress).coerceAtLeast(0.0)
            Surface(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(start = 12.dp, end = 88.dp, bottom = 16.dp)
                    .fillMaxWidth(),
                shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
                color = MaterialTheme.colors.surface,
                elevation = 8.dp
            ) {
                Row(
                    modifier = Modifier.padding(start = 14.dp, end = 6.dp, top = 8.dp, bottom = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text("Navigasi aktif", style = MaterialTheme.typography.subtitle2, color = MaterialTheme.colors.primary)
                        Text(
                            if (route.isKmlRoute) "Ikuti jalur KML" else nextStep?.instruction ?: "Ikuti rute",
                            style = MaterialTheme.typography.body2,
                            color = MaterialTheme.colors.onSurface
                        )
                        Text(
                            if (route.isKmlRoute) {
                                "${route.sourceFileName ?: "Rute KML"} · Sisa ${formatRouteDistance(remaining)}"
                            } else {
                                "${formatRouteDistance(distanceToNext)} · Sisa ${formatRouteDistance(remaining)} · ${formatRouteDuration(etaSeconds)}"
                            },
                            style = MaterialTheme.typography.caption,
                            color = MaterialTheme.colors.onSurface.copy(alpha = 0.72f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (route.isKmlRoute) {
                            Text(
                                "Hijau: sudah dilalui · Merah: belum dilalui",
                                style = MaterialTheme.typography.caption,
                                color = MaterialTheme.colors.onSurface.copy(alpha = 0.65f)
                            )
                        }
                    }
                    IconButton(onClick = {
                        activeRoute = null
                        isFollowingLocation = false
                    }) {
                        Icon(Icons.Default.Close, contentDescription = "Hentikan navigasi")
                    }
                }
            }
        }
        pendingKmlRoute?.let { route ->
            val start = route.coordinates.first()
            val end = route.coordinates.last()
            AlertDialog(
                onDismissRequest = { pendingKmlRoute = null },
                title = { Text("Tinjau rute KML") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(route.sourceFileName ?: "Rute KML")
                        Text("Jarak jalur: ${formatRouteDistance(route.distanceMeters)}")
                        Text("Awal: ${String.format(Locale.US, "%.5f, %.5f", start.latitude, start.longitude)}")
                        Text("Tujuan: ${String.format(Locale.US, "%.5f, %.5f", end.latitude, end.longitude)}")
                        Text("File KML tidak menyediakan instruksi belokan atau ETA.")
                    }
                },
                confirmButton = {
                    TextButton(onClick = {
                        activeRoute = route
                        pendingKmlRoute = null
                        selectedGuidePinId = null
                        selectedGuidePhoto = null
                        isFollowingLocation = true
                        mapLibreMapRef?.let { fitMapToRoute(it, route) }
                    }) {
                        Text("Mulai navigasi")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { pendingKmlRoute = null }) {
                        Text("Batal")
                    }
                }
            )
        }
    }
}

private fun fitMapToRoute(map: MapLibreMap, route: DrivingRoute) {
    if (route.coordinates.isEmpty()) return
    val boundsBuilder = LatLngBounds.Builder()
    route.coordinates.forEach { coordinate ->
        boundsBuilder.include(LatLng(coordinate.latitude, coordinate.longitude))
    }
    runCatching {
        map.animateCamera(CameraUpdateFactory.newLatLngBounds(boundsBuilder.build(), 96))
    }
}

private fun renderPhotoMarkers(map: MapLibreMap, context: Context, photos: List<FieldPhoto>) {
    val tagColorStore = PhotoTagColorStore(context)
    tagColorStore.ensureTags(photos.flatMap { extractPhotoTags(it.note) })
    val colorForTag: (String) -> Int = tagColorStore::colorFor
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
            photoMarkerColorArgb(primaryTags, colorForTag)
        } else {
            photoPinColorArgb(cluster.first().note, colorForTag)
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
