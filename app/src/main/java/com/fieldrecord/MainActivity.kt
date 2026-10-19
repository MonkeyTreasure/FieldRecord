package com.fieldrecord

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import java.io.File
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.Bundle
import android.provider.Settings
import android.provider.OpenableColumns
import java.net.URL
import java.util.Date
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import javax.inject.Inject
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.runtime.SideEffect
import androidx.core.view.WindowInsetsControllerCompat
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.compose.runtime.rememberCoroutineScope
import com.fieldrecord.domain.model.FieldPhoto
import com.fieldrecord.domain.model.PhotoTagFilter
import com.fieldrecord.domain.model.collectPhotoTagFacets
import com.fieldrecord.location.LocationTrackingService
import com.fieldrecord.presentation.dashboard.TagDashboardScreen
import com.fieldrecord.presentation.dashboard.TagPhotoGalleryScreen
import com.fieldrecord.presentation.camera.CameraScreen
import com.fieldrecord.presentation.gallery.GalleryScreen
import com.fieldrecord.presentation.gallery.GalleryViewModel
import com.fieldrecord.presentation.map.MapScreen
import com.fieldrecord.presentation.map.MapViewModel
import com.fieldrecord.presentation.note.NoteScreen
import com.fieldrecord.presentation.photo.PhotoFullscreenViewer
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var repo: com.fieldrecord.domain.repository.FieldPhotoRepository


    private val requiredPermissions: Array<String>
        get() {
            val perms = mutableListOf(
                Manifest.permission.CAMERA,
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                perms.add(Manifest.permission.READ_MEDIA_IMAGES)
            } else {
                perms.add(Manifest.permission.READ_EXTERNAL_STORAGE)
                perms.add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            }
            return perms.toTypedArray()
        }

    private val permissionsGranted = mutableStateOf(false)

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { _ ->
        if (allCriticalGranted() && !hasSharedStorageAccess()) openSharedStorageSettings()
        permissionsGranted.value = allCriticalGranted() && hasSharedStorageAccess()
        if (hasPermission(Manifest.permission.ACCESS_FINE_LOCATION)) LocationTrackingService.start(this)
    }

    private fun hasPermission(p: String) = ContextCompat.checkSelfPermission(this, p) == PackageManager.PERMISSION_GRANTED
    private fun allCriticalGranted() = hasPermission(Manifest.permission.CAMERA) && hasPermission(Manifest.permission.ACCESS_FINE_LOCATION)
    private fun hasSharedStorageAccess() = Build.VERSION.SDK_INT < Build.VERSION_CODES.R || Environment.isExternalStorageManager()
    private fun openSharedStorageSettings() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val intent = android.content.Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION, Uri.parse("package:$packageName"))
            runCatching { startActivity(intent) }.onFailure { startActivity(android.content.Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)) }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        checkTimeSync()
        permissionsGranted.value = allCriticalGranted() && hasSharedStorageAccess()
        if (allCriticalGranted()) LocationTrackingService.start(this)
        if (!allCriticalGranted()) permissionLauncher.launch(requiredPermissions)
        handleDeepLink(intent)
        setContent {
            FieldRecordTheme {
                val granted by permissionsGranted
                if (granted) MainScreen() else PermissionScreen(
                    onRequest = { permissionLauncher.launch(requiredPermissions) },
                    onStorageAccess = ::openSharedStorageSettings,
                    storageAccessMissing = allCriticalGranted() && !hasSharedStorageAccess()
                )
            }
        }
    }

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleDeepLink(intent)
    }

    private fun handleDeepLink(intent: android.content.Intent?) {
        val data = intent?.data
        val action = intent?.action

        // Receive .field documents through content URIs from Sharesheet, Files, and other apps.
        if (action == Intent.ACTION_VIEW || action == Intent.ACTION_SEND || action == Intent.ACTION_SEND_MULTIPLE) {
            val incomingUris = mutableListOf<Uri>()
            data?.let(incomingUris::add)
            @Suppress("DEPRECATION")
            (intent?.getParcelableExtra<Uri>(Intent.EXTRA_STREAM))?.let(incomingUris::add)
            @Suppress("DEPRECATION")
            intent?.getParcelableArrayListExtra<Uri>(Intent.EXTRA_STREAM)?.let(incomingUris::addAll)
            intent?.clipData?.let { clip ->
                for (index in 0 until clip.itemCount) clip.getItemAt(index).uri?.let(incomingUris::add)
            }
            incomingUris.distinct().firstOrNull(::isFieldUri)?.let { uri ->
                parseFieldFile(uri)
                return
            }
        }

        // Handle URL deep links (existing)
        if (data != null && (data.scheme == "fieldrecord" || data.host == "fieldrecord.redmonkey.my.id")) {
            val lat = data.getQueryParameter("lat")?.toDoubleOrNull()
            val lon = data.getQueryParameter("lon")?.toDoubleOrNull()
            val note = data.getQueryParameter("note")?.let { android.net.Uri.decode(it) }
            val timestamp = data.getQueryParameter("timestamp")?.toLongOrNull()
            if (lat != null && lon != null) {
                if (timestamp != null) {
                    lifecycleScope.launch {
                        repo.findByMetadata(lat, lon, timestamp)?.let {
                            runOnUiThread {
                                android.app.AlertDialog.Builder(this@MainActivity)
                                    .setTitle("Data sudah ada")
                                    .setMessage("Dokumen dengan metadata ini sudah ada di aplikasi.")
                                    .setPositiveButton("OK") { dialog, _ -> dialog.dismiss() }
                                    .show()
                            }
                            return@launch
                        }
                        saveDeepLinkData(lat, lon, note)
                    }
                } else {
                    saveDeepLinkData(lat, lon, note)
                }
            }
        } else {
            // Check EXTRA_TEXT for legacy format
            val clipText = intent?.getStringExtra(android.content.Intent.EXTRA_TEXT)
            if (clipText != null && (clipText.startsWith("fieldrecord://share") || clipText.contains("fieldrecord.redmonkey.my.id/share"))) {
                val uriText = android.net.Uri.parse(clipText.substringAfterLast(" ").trim())
                val lat = uriText.getQueryParameter("lat")?.toDoubleOrNull()
                val lon = uriText.getQueryParameter("lon")?.toDoubleOrNull()
                val note = uriText.getQueryParameter("note")?.let { android.net.Uri.decode(it) }
                if (lat != null && lon != null) saveDeepLinkData(lat, lon, note)
            }
        }
    }

    private fun isFieldUri(uri: Uri): Boolean {
        val displayName = runCatching {
            contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) cursor.getString(0) else null
            }
        }.getOrNull()
        val name = displayName ?: uri.lastPathSegment.orEmpty()
        return name.endsWith(".field", ignoreCase = true)
    }

    private fun parseFieldFile(uri: android.net.Uri) {
        runOnUiThread {
            val progressDialog = android.app.ProgressDialog(this).apply {
                setTitle("Mengimpor Data .field")
                setMessage("Memproses...")
                setCancelable(false)
                setProgressStyle(android.app.ProgressDialog.STYLE_HORIZONTAL)
                show()
            }
            lifecycleScope.launch(kotlinx.coroutines.Dispatchers.IO) {
                try {
                    val content = contentResolver.openInputStream(uri)?.use { it.bufferedReader().readText() } ?: ""
                    val entries = content.split("\n---\n")
                    var importedCount = 0
                    var skippedCount = 0
                    var errorCount = 0
                    val existingFilenames = mutableListOf<String>()
                    val total = entries.size
                    for ((index, entry) in entries.withIndex()) {
                        val progress = ((index + 1) * 100 / total).coerceAtMost(100)
                        runOnUiThread { progressDialog.progress = progress }
                        val params = entry.split("\n").associate { line ->
                            val parts = line.split("=", limit = 2)
                            if (parts.size == 2) parts[0].trim() to parts[1].trim() else "" to ""
                        }
                        val lat = params["Lat"]?.toDoubleOrNull()
                        val lon = params["Lon"]?.toDoubleOrNull()
                        val timestamp = params["Timestamp"]?.toLongOrNull() ?: System.currentTimeMillis()
                        val note = params["Note"]?.let { android.net.Uri.decode(it) } ?: ""
                        val photoBase64 = params["PhotoBase64"] ?: ""
                        val photographer = params["Photographer"]?.takeIf { it.isNotBlank() }
                        val sourcePhotoName = params["PhotoName"]?.takeIf { it.isNotBlank() }

                        if (lat != null && lon != null) {
                            if (repo.findByMetadata(lat, lon, timestamp) == null) {
                                val newFile = com.fieldrecord.presentation.camera.PhotoFileManager.getOutputFileForTimestamp(
                                    timestamp,
                                    existingFilenames,
                                    photographer,
                                    sourcePhotoName
                                )

                                if (photoBase64.isNotBlank()) {
                                    try {
                                        val imageBytes = android.util.Base64.decode(photoBase64, android.util.Base64.DEFAULT)
                                        newFile.writeBytes(imageBytes)
                                    } catch (_: Exception) {
                                        newFile.writeText("Imported FieldRecord Photo Placeholder")
                                        errorCount++
                                    }
                                } else {
                                    newFile.writeText("Imported FieldRecord Photo Placeholder")
                                }

                                // Scan file into MediaStore so it appears in the gallery
                                android.media.MediaScannerConnection.scanFile(
                                    this@MainActivity,
                                    arrayOf(newFile.absolutePath),
                                    arrayOf("image/jpeg"),
                                    null
                                )

                                repo.insertPhoto(
                                    com.fieldrecord.domain.model.FieldPhoto(
                                        id = 0,
                                        filePath = newFile.absolutePath,
                                        fileName = newFile.name,
                                        latitude = lat,
                                        longitude = lon,
                                        bearing = 0f,
                                        timestamp = timestamp,
                                        note = note
                                    )
                                )
                                importedCount++
                            } else {
                                skippedCount++
                            }
                        } else {
                            errorCount++
                        }
                    }
                    runOnUiThread {
                        progressDialog.dismiss()
                        val msg = StringBuilder()
                        if (importedCount > 0) msg.append("$importedCount data berhasil diimpor\n")
                        if (skippedCount > 0) msg.append("$skippedCount data dilewati (sudah ada)\n")
                        if (errorCount > 0) msg.append("$errorCount data gagal (format tidak valid)\n")
                        if (msg.isEmpty()) msg.append("Tidak ada data valid untuk diimpor")
                        android.app.AlertDialog.Builder(this@MainActivity)
                            .setTitle("Hasil Impor .field")
                            .setMessage(msg.toString().trim())
                            .setPositiveButton("OK", null)
                            .show()
                    }
                } catch (e: Exception) {
                    runOnUiThread {
                        progressDialog.dismiss()
                        android.app.AlertDialog.Builder(this@MainActivity)
                            .setTitle("Gagal")
                            .setMessage("Gagal membaca file .field: ${e.message}")
                            .setPositiveButton("OK", null)
                            .show()
                    }
                }
            }
        }
    }

    private fun checkTimeSync() {
        lifecycleScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            try {
                // 1. Cek pengaturan waktu otomatis di Android OS
                val isAutoTime = Settings.Global.getInt(contentResolver, Settings.Global.AUTO_TIME, 0) == 1
                if (!isAutoTime) {
                    kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                        showTimeErrorDialog("Pengaturan Waktu Otomatis Mati", "Silakan aktifkan 'Waktu Otomatis' pada pengaturan tanggal & waktu HP Anda agar data foto akurat.")
                    }
                    return@launch
                }

                // 2. Cek selisih waktu HP dengan Waktu Dunia (World Time Server / NTP / HTTP Date Header)
                val connection = URL("https://worldtimeapi.org/api/timezone/Etc/UTC").openConnection() as java.net.HttpURLConnection
                connection.connectTimeout = 3000
                connection.readTimeout = 3000
                connection.requestMethod = "GET"

                val dateHeader = connection.getHeaderField("Date")
                val networkTime = if (dateHeader != null) {
                    val format = java.text.SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z", java.util.Locale.US)
                    format.parse(dateHeader)?.time ?: System.currentTimeMillis()
                } else {
                    System.currentTimeMillis()
                }

                val deviceTime = System.currentTimeMillis()
                val diffSeconds = Math.abs(deviceTime - networkTime) / 1000

                // Jika selisih waktu HP dan server dunia > 3 menit (180 detik)
                if (diffSeconds > 180) {
                    kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                        showTimeErrorDialog("Tanggal & Waktu Tidak Sesuai", "Tanggal atau waktu pada HP Anda berbeda lebih dari 3 menit dari Waktu Dunia. Harap sesuaikan jam/tanggal pada HP Anda.")
                    }
                }
            } catch (_: Exception) {
                // Jika offline / internet gagal, pengecekan dialihkan ke AUTO_TIME saja
            }
        }
    }

    private fun showTimeErrorDialog(title: String, message: String) {
        android.app.AlertDialog.Builder(this)
            .setTitle(title)
            .setMessage(message)
            .setCancelable(false)
            .setPositiveButton("Pengaturan Waktu") { _, _ ->
                startActivity(Intent(Settings.ACTION_DATE_SETTINGS))
                finish()
            }
            .setNegativeButton("Tutup Aplikasi") { _, _ ->
                finish()
            }
            .show()
    }

    private fun saveDeepLinkData(lat: Double, lon: Double, note: String?) {
        getSharedPreferences("field_record_deep_link", MODE_PRIVATE).edit()
            .putString("deep_link_lat", lat.toString())
            .putString("deep_link_lon", lon.toString())
            .putString("deep_link_note", note ?: "")
            .apply()
    }
}

@Composable
fun PermissionScreen(onRequest: () -> Unit, onStorageAccess: () -> Unit, storageAccessMissing: Boolean) {
    val context = LocalContext.current
    val hasCamera = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
    val hasLocation = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
    val hasStorage = Build.VERSION.SDK_INT < Build.VERSION_CODES.R || Environment.isExternalStorageManager()
    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colors.background) {
        Box(modifier = Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                Text("Izin Akses Aplikasi", style = MaterialTheme.typography.h5, color = MaterialTheme.colors.onBackground)
                Spacer(Modifier.height(8.dp))
                Text("Aplikasi memerlukan izin berikut untuk dapat bekerja secara optimal di lapangan:", style = MaterialTheme.typography.body2, color = MaterialTheme.colors.onBackground.copy(alpha = 0.7f), fontSize = 14.sp)
                Spacer(Modifier.height(32.dp))
                Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    PermissionCheckItem("Kamera", "Mengambil foto sampel lapangan", hasCamera)
                    PermissionCheckItem("Lokasi Presisi (GPS)", "Menandai posisi koordinat pada foto dan peta", hasLocation)
                    PermissionCheckItem("Akses Folder (Semua File)", "Menyimpan foto ke Internal Storage/FieldRecord/Photo (dapat diakses via USB)", hasStorage)
                }
                Spacer(Modifier.height(40.dp))
                if (storageAccessMissing) {
                    Button(onClick = onStorageAccess, colors = ButtonDefaults.buttonColors(backgroundColor = MaterialTheme.colors.primary), modifier = Modifier.fillMaxWidth().height(48.dp)) { Text("Izinkan Akses Folder USB", color = MaterialTheme.colors.onPrimary, fontSize = 16.sp) }
                } else {
                    Button(onClick = onRequest, colors = ButtonDefaults.buttonColors(backgroundColor = MaterialTheme.colors.primary), modifier = Modifier.fillMaxWidth().height(48.dp)) { Text("Berikan Semua Izin", color = MaterialTheme.colors.onPrimary, fontSize = 16.sp) }
                }
            }
        }
    }
}

@Composable
private fun PermissionCheckItem(title: String, description: String, isGranted: Boolean) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        RadioButton(selected = isGranted, onClick = null, colors = RadioButtonDefaults.colors(selectedColor = MaterialTheme.colors.primary, unselectedColor = MaterialTheme.colors.onSurface.copy(alpha = 0.6f)))
        Spacer(Modifier.width(12.dp))
        Column { Text(title, style = MaterialTheme.typography.subtitle1, color = MaterialTheme.colors.onBackground); Text(description, style = MaterialTheme.typography.caption, color = MaterialTheme.colors.onBackground.copy(alpha = 0.65f)) }
    }
}

private data class NavItem(val label: String, val icon: ImageVector, val route: String)

@Composable
fun MainScreen() {
    val navController = rememberNavController()
    val screenScope = rememberCoroutineScope()
    val mapViewModel: MapViewModel = hiltViewModel()
    val photos by mapViewModel.photos.collectAsStateWithLifecycle()
    val tagFacets = remember(photos) { collectPhotoTagFacets(photos.map { it.note }) }
    val hasDashboardTags = !tagFacets.isEmpty
    var selectedTagFilter by remember { mutableStateOf<PhotoTagFilter?>(null) }
    var mapOpenRequest by remember { mutableIntStateOf(0) }
    val currentBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = currentBackStackEntry?.destination?.route ?: "map"
    val context = LocalContext.current
    
    // Check for app update notification
    var showUpdateDialog by remember { mutableStateOf(false) }
    var currentVersionName by remember { mutableStateOf("") }
    
    LaunchedEffect(Unit) {
        try {
            val pInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            val versionCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) pInfo.longVersionCode else pInfo.versionCode.toLong()
            currentVersionName = pInfo.versionName ?: "1.3"
            
            val prefs = context.getSharedPreferences("app_update_prefs", Context.MODE_PRIVATE)
            val lastVersionCode = prefs.getLong("last_version_code", -1L)
            
            if (lastVersionCode == -1L) {
                // First install or first time tracking, save it silently
                prefs.edit().putLong("last_version_code", versionCode).apply()
            } else if (versionCode > lastVersionCode) {
                // Update detected!
                showUpdateDialog = true
                prefs.edit().putLong("last_version_code", versionCode).apply()
            }
        } catch (_: Exception) {}
    }

    if (showUpdateDialog) {
        AlertDialog(
            onDismissRequest = { showUpdateDialog = false },
            title = { Text("Pembaruan Aplikasi") },
            text = { Text("Aplikasi FieldRecord telah berhasil diperbarui ke versi $currentVersionName.") },
            confirmButton = {
                Button(
                    onClick = { showUpdateDialog = false },
                    colors = ButtonDefaults.buttonColors(backgroundColor = MaterialTheme.colors.primary)
                ) {
                    Text("OK", color = MaterialTheme.colors.onPrimary)
                }
            }
        )
    }

    val navItems = buildList {
        if (hasDashboardTags) add(NavItem("Dashboard", Icons.Default.Dashboard, "dashboard"))
        add(NavItem("Maps", Icons.Default.Map, "map"))
        add(NavItem("Gallery", Icons.Default.PhotoLibrary, "gallery"))
        add(NavItem("Noted", Icons.Default.Description, "noted"))
    }
    val selectedNavRoute = if (currentRoute == "tag-gallery") "dashboard" else currentRoute

    LaunchedEffect(hasDashboardTags, currentRoute) {
        if (!hasDashboardTags && currentRoute == "dashboard") {
            selectedTagFilter = null
            navController.popBackStack("map", inclusive = false)
        }
    }

    fun navigateTo(route: String) {
        if (route == "map") {
            mapOpenRequest++
            if (currentRoute != "map") navController.popBackStack("map", inclusive = false)
        } else if (currentRoute != route) {
            navController.navigate(route) { launchSingleTop = true }
        }
    }

    Scaffold(
        bottomBar = {
            FieldRecordBottomBar(
                items = navItems,
                selectedRoute = selectedNavRoute,
                onNavigate = ::navigateTo
            )
        }
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            NavHost(navController, startDestination = "map", modifier = Modifier.fillMaxSize()) {
                composable("dashboard") {
                    if (hasDashboardTags) {
                        TagDashboardScreen(tagFacets) { filter ->
                            selectedTagFilter = filter
                            navController.navigate("tag-gallery") { launchSingleTop = true }
                        }
                    }
                }
                composable("tag-gallery") {
                    val filter = selectedTagFilter
                    if (filter == null) {
                        LaunchedEffect(Unit) { navController.popBackStack("dashboard", inclusive = false) }
                    } else {
                        TagPhotoGalleryScreen(
                            photos = photos,
                            filter = filter,
                            onBack = { navController.popBackStack("dashboard", inclusive = false) },
                            onSaveNote = { id, note -> screenScope.launch { mapViewModel.updatePhotoNote(id, note) } }
                        )
                    }
                }
                composable("map") {
                    var selectedPhotoForViewer by remember { mutableStateOf<FieldPhoto?>(null) }
                    var selectedPhotoList by remember { mutableStateOf<List<FieldPhoto>>(emptyList()) }
                    MapScreen(
                        modifier = Modifier.fillMaxSize(),
                        photos = photos,
                        tagFilter = selectedTagFilter,
                        mapOpenRequest = mapOpenRequest,
                        onClearTagFilter = { selectedTagFilter = null },
                        onPhotoClick = { photo ->
                            selectedPhotoList = listOf(photo)
                            selectedPhotoForViewer = photo
                        },
                        onClusterClick = { ids ->
                            val clusterPhotos = photos.filter { it.id in ids }.sortedBy { it.timestamp }
                            if (clusterPhotos.isNotEmpty()) { selectedPhotoList = clusterPhotos; selectedPhotoForViewer = clusterPhotos.first() }
                        },
                        onSavePhotoNote = { id, note -> mapViewModel.updatePhotoNote(id, note) }
                    )
                    selectedPhotoForViewer?.let { photo ->
                        val idx = selectedPhotoList.indexOfFirst { it.id == photo.id }.coerceAtLeast(0)
                        PhotoFullscreenViewer(photos = selectedPhotoList, initialIndex = idx, knownTags = photos.flatMap { com.fieldrecord.domain.model.extractPhotoTags(it.note) }.toSet(), onDismiss = { selectedPhotoForViewer = null }, onSaveNote = { id, n -> mapViewModel.updatePhotoNote(id, n) })
                    }
                }
                composable("gallery") {
                    val vm: GalleryViewModel = hiltViewModel()
                    GalleryScreen(viewModel = vm)
                }
                composable("noted") {
                    NoteScreen(
                        photos = photos,
                        onSaveNote = { id, note -> mapViewModel.updatePhotoNote(id, note) }
                    )
                }
                composable("camera") { CameraScreen(onPhotoCaptured = { }) }
            }
        }
    }
}

@Composable
private fun FieldRecordBottomBar(
    items: List<NavItem>,
    selectedRoute: String,
    onNavigate: (String) -> Unit
) {
    val purple = Color(0xFF673AB7)
    val leftItems = items.filter { it.route == "dashboard" || it.route == "map" }
    val rightItems = items.filter { it.route == "gallery" || it.route == "noted" }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(purple)
            .navigationBarsPadding()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(72.dp)
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(1f).fillMaxHeight(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                leftItems.forEach { item ->
                    FieldRecordNavItem(
                        item = item,
                        selected = selectedRoute == item.route,
                        onClick = { onNavigate(item.route) },
                        modifier = Modifier.weight(1f).fillMaxHeight()
                    )
                }
            }
            Spacer(Modifier.width(72.dp))
            Row(
                modifier = Modifier.weight(1f).fillMaxHeight(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                rightItems.forEach { item ->
                    FieldRecordNavItem(
                        item = item,
                        selected = selectedRoute == item.route,
                        onClick = { onNavigate(item.route) },
                        modifier = Modifier.weight(1f).fillMaxHeight()
                    )
                }
            }
        }
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .offset(y = (-20).dp)
                .size(72.dp)
                .clickable(onClick = { onNavigate("camera") })
                .semantics {
                    contentDescription = "Kamera"
                    role = Role.Button
                },
            contentAlignment = Alignment.Center
        ) {
            val diamondShape = RoundedCornerShape(12.dp)
            Box(
                Modifier
                    .size(58.dp)
                    .rotate(45f)
                    .shadow(8.dp, diamondShape)
                    .background(Color(0xFF8E5AF7), diamondShape)
                    .border(3.dp, Color.White, diamondShape)
            )
            Icon(
                imageVector = Icons.Default.AddAPhoto,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(27.dp)
            )
        }
    }
}

@Composable
private fun FieldRecordNavItem(
    item: NavItem,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val tint = if (selected) Color.White else Color.White.copy(alpha = 0.68f)
    Column(
        modifier = modifier
            .clickable(onClick = onClick)
            .padding(vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(item.icon, contentDescription = null, tint = tint, modifier = Modifier.size(24.dp))
        Spacer(Modifier.height(3.dp))
        Text(item.label, color = tint, fontSize = 11.sp, maxLines = 1)
    }
}

val LocalAppIconColor = staticCompositionLocalOf { Color.White }

@Composable
fun FieldRecordTheme(content: @Composable () -> Unit) {
    val context = LocalContext.current
    val purple = Color(0xFF673AB7)
    val appColors = darkColors(
        primary = purple,
        primaryVariant = Color(0xFF512DA8),
        onPrimary = Color.White,
        secondary = purple,
        background = purple,
        surface = purple,
        onBackground = Color.White,
        onSurface = Color.White
    )
    SideEffect {
        (context as? Activity)?.window?.let { window ->
            val systemBarColor = android.graphics.Color.rgb(103, 58, 183)
            window.statusBarColor = systemBarColor
            window.navigationBarColor = systemBarColor
            WindowInsetsControllerCompat(window, window.decorView).apply {
                isAppearanceLightStatusBars = false
                isAppearanceLightNavigationBars = false
            }
        }
    }
    MaterialTheme(colors = appColors) {
        CompositionLocalProvider(LocalAppIconColor provides Color.White, content = content)
    }
}

private fun Int.dpUnused() = this
