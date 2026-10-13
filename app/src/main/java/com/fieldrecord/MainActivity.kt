package com.fieldrecord

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Environment
import android.os.Bundle
import android.net.Uri
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.fieldrecord.domain.model.FieldPhoto
import com.fieldrecord.location.LocationTrackingService
import com.fieldrecord.presentation.camera.CameraScreen
import com.fieldrecord.presentation.gallery.GalleryScreen
import com.fieldrecord.presentation.gallery.GalleryViewModel
import com.fieldrecord.presentation.map.MapScreen
import com.fieldrecord.presentation.map.MapViewModel
import com.fieldrecord.presentation.photo.PhotoFullscreenViewer
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

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
        permissionsGranted.value = allCriticalGranted() && hasSharedStorageAccess()
        if (allCriticalGranted()) LocationTrackingService.start(this)
        if (!allCriticalGranted()) permissionLauncher.launch(requiredPermissions)
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

    override fun onResume() {
        super.onResume()
        if (allCriticalGranted()) LocationTrackingService.start(this)
        permissionsGranted.value = allCriticalGranted() && hasSharedStorageAccess()
    }
}

@Composable
fun PermissionScreen(onRequest: () -> Unit, onStorageAccess: () -> Unit, storageAccessMissing: Boolean) {
    val context = LocalContext.current
    val hasCamera = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
    val hasLocation = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
    val hasStorage = Build.VERSION.SDK_INT < Build.VERSION_CODES.R || Environment.isExternalStorageManager()
    Surface(modifier = Modifier.fillMaxSize(), color = Color.White) {
        Box(modifier = Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                Text("Izin Akses Aplikasi", style = MaterialTheme.typography.h5, color = Color.Black)
                Spacer(Modifier.height(8.dp))
                Text("Aplikasi memerlukan izin berikut untuk dapat bekerja secara optimal di lapangan:", style = MaterialTheme.typography.body2, color = Color.Black.copy(alpha = 0.7f), fontSize = 14.sp)
                Spacer(Modifier.height(32.dp))
                Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    PermissionCheckItem("Kamera", "Mengambil foto sampel lapangan", hasCamera)
                    PermissionCheckItem("Lokasi Presisi (GPS)", "Menandai posisi koordinat pada foto dan peta", hasLocation)
                    PermissionCheckItem("Akses Folder (Semua File)", "Menyimpan foto ke Internal Storage/FieldRecord/Photo (dapat diakses via USB)", hasStorage)
                }
                Spacer(Modifier.height(40.dp))
                if (storageAccessMissing) {
                    Button(onClick = onStorageAccess, colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFF2E7D32)), modifier = Modifier.fillMaxWidth().height(48.dp)) { Text("Izinkan Akses Folder USB", color = Color.White, fontSize = 16.sp) }
                } else {
                    Button(onClick = onRequest, colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFF2E7D32)), modifier = Modifier.fillMaxWidth().height(48.dp)) { Text("Berikan Semua Izin", color = Color.White, fontSize = 16.sp) }
                }
            }
        }
    }
}

@Composable
private fun PermissionCheckItem(title: String, description: String, isGranted: Boolean) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        RadioButton(selected = isGranted, onClick = null, colors = RadioButtonDefaults.colors(selectedColor = Color(0xFF2E7D32), unselectedColor = Color.Gray))
        Spacer(Modifier.width(12.dp))
        Column { Text(title, style = MaterialTheme.typography.subtitle1, color = Color.Black); Text(description, style = MaterialTheme.typography.caption, color = Color.Black.copy(alpha = 0.6f)) }
    }
}

private data class NavItem(val label: String, val icon: ImageVector, val route: String)

@Composable
fun MainScreen() {
    val navController = rememberNavController()
    var selectedTab by remember { mutableStateOf(0) }
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
                    colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFF2E7D32))
                ) {
                    Text("OK", color = Color.White)
                }
            }
        )
    }

    // Maps | Camera(FAB) | Gallery  — 3 item seimbang, tiru gaya pill floating di foto
    val navItems = listOf(
        NavItem("Maps", Icons.Default.Map, "map"),
        NavItem("Camera", Icons.Default.AddAPhoto, "camera"),
        NavItem("Gallery", Icons.Default.PhotoLibrary, "gallery")
    )

    fun navigateTo(index: Int) {
        selectedTab = index
        navController.navigate(navItems[index].route) {
            launchSingleTop = true
            if (index == 0) popUpTo("map") { inclusive = false }
        }
    }

    Scaffold(
        bottomBar = {
            BottomNavigation(
                backgroundColor = Color(0xFF1E1E1E),
                elevation = 8.dp,
                modifier = Modifier.navigationBarsPadding()
            ) {
                navItems.forEachIndexed { index, item ->
                    BottomNavigationItem(
                        icon = { Icon(item.icon, contentDescription = item.label) },
                        label = { Text(item.label) },
                        selected = selectedTab == index,
                        onClick = { navigateTo(index) },
                        selectedContentColor = Color(0xFF2E7D32),
                        unselectedContentColor = Color.White.copy(alpha = 0.6f)
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            NavHost(navController, startDestination = "map", modifier = Modifier.fillMaxSize()) {
                composable("map") {
                    val vm: MapViewModel = hiltViewModel()
                    val photos by vm.photos.collectAsStateWithLifecycle()
                    var selectedPhotoForViewer by remember { mutableStateOf<FieldPhoto?>(null) }
                    var selectedPhotoList by remember { mutableStateOf<List<FieldPhoto>>(emptyList()) }
                    MapScreen(
                        modifier = Modifier.fillMaxSize(),
                        photos = photos,
                        onPhotoClick = { photo ->
                            selectedPhotoList = listOf(photo)
                            selectedPhotoForViewer = photo
                        },
                        onClusterClick = { ids ->
                            val clusterPhotos = photos.filter { it.id in ids }.sortedBy { it.timestamp }
                            if (clusterPhotos.isNotEmpty()) { selectedPhotoList = clusterPhotos; selectedPhotoForViewer = clusterPhotos.first() }
                        },
                        onSavePhotoNote = { id, note -> vm.updatePhotoNote(id, note) }
                    )
                    selectedPhotoForViewer?.let { photo ->
                        val idx = selectedPhotoList.indexOfFirst { it.id == photo.id }.coerceAtLeast(0)
                        PhotoFullscreenViewer(photos = selectedPhotoList, initialIndex = idx, knownTags = photos.flatMap { com.fieldrecord.domain.model.extractPhotoTags(it.note) }.toSet(), onDismiss = { selectedPhotoForViewer = null }, onSaveNote = { id, n -> vm.updatePhotoNote(id, n) })
                    }
                }
                composable("gallery") {
                    val vm: GalleryViewModel = hiltViewModel()
                    GalleryScreen(viewModel = vm)
                }
                composable("camera") { CameraScreen(onPhotoCaptured = { }) }
            }
        }
    }
}

@Composable
fun FieldRecordTheme(content: @Composable () -> Unit) {
    MaterialTheme(colors = darkColors(primary = Color(0xFF2E7D32), secondary = Color(0xFFFFB300), background = Color(0xFF121212), surface = Color(0xFF1E1E1E)), content = content)
}

private fun Int.dpUnused() = this
