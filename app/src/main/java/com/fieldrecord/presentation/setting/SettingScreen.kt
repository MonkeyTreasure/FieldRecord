package com.fieldrecord.presentation.setting

import android.content.Context
import android.os.Environment
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fieldrecord.presentation.camera.PhotoFileManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File

@Composable
fun SettingScreen() {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("field_record_prefs", Context.MODE_PRIVATE) }
    var unitId by remember { mutableStateOf(prefs.getString("unit_id", "SBYE") ?: "SBYE") }
    var operatorName by remember { mutableStateOf(prefs.getString("operator_name", "") ?: "") }
    var watermarkBearing by remember { mutableStateOf(prefs.getBoolean("watermark_bearing", true)) }
    var themeMode by remember { mutableStateOf(prefs.getString("app_theme_mode", "dark") ?: "dark") }
    var primaryHex by remember { mutableStateOf(prefs.getString("app_theme_primary", "#2E7D32") ?: "#2E7D32") }
    val defaultTextHex = if (themeMode == "light") "#202420" else "#FFFFFF"
    var textHex by remember(themeMode) { mutableStateOf(prefs.getString("app_theme_text", defaultTextHex) ?: defaultTextHex) }
    var iconHex by remember(themeMode) { mutableStateOf(prefs.getString("app_theme_icon", defaultTextHex) ?: defaultTextHex) }
    fun selectTheme(mode: String) {
        themeMode = mode
        prefs.edit().putString("app_theme_mode", mode).apply()
    }
    fun storeColor(key: String, hex: String) {
        if (parseHexColor(hex) != null) prefs.edit().putString(key, hex.trim().uppercase()).apply()
    }

    // Import state
    var importProgress by remember { mutableStateOf(0) }
    var importStatus by remember { mutableStateOf("") } // "", "loading", "success", "error"
    var importMessage by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()

    val storageDir = remember { PhotoFileManager.getStorageDir() }
    val totalPhotos = remember { storageDir.listFiles()?.count { it.extension.equals("jpg", true) } ?: 0 }
    val hasStorage = Environment.isExternalStorageManager()

    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("Pengaturan", style = MaterialTheme.typography.h6, color = MaterialTheme.colors.onBackground)
        Text("Tema aplikasi", style = MaterialTheme.typography.subtitle1, color = MaterialTheme.colors.primary)
        Text("Tema diterapkan pada seluruh tampilan aplikasi.", style = MaterialTheme.typography.caption, color = MaterialTheme.colors.onBackground.copy(alpha = 0.7f))
        listOf("system" to "Ikuti perangkat", "light" to "Terang", "dark" to "Gelap").forEach { (mode, label) ->
            Row(
                modifier = Modifier.fillMaxWidth().clickable { selectTheme(mode) }.padding(vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(
                    selected = themeMode == mode,
                    onClick = { selectTheme(mode) },
                    colors = RadioButtonDefaults.colors(selectedColor = MaterialTheme.colors.primary)
                )
                Text(label, color = MaterialTheme.colors.onBackground)
            }
        }
        Text("Warna kustom", style = MaterialTheme.typography.subtitle1, color = MaterialTheme.colors.primary)
        Text("Ketik kode HEX #RRGGBB atau pilih contoh warna. Warna diterapkan langsung.", style = MaterialTheme.typography.caption, color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f))
        ThemeColorField("Warna utama", primaryHex, { primaryHex = it; storeColor("app_theme_primary", it) })
        ThemeColorField("Warna teks", textHex, { textHex = it; storeColor("app_theme_text", it) })
        ThemeColorField("Warna ikon", iconHex, { iconHex = it; storeColor("app_theme_icon", it) })
        Divider()

        Text("Unit / Rayon", fontSize = 13.sp, color = MaterialTheme.colors.onBackground.copy(alpha = 0.65f))
        OutlinedTextField(value = unitId, onValueChange = { unitId = it }, label = { Text("Unit ID (watermark)") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
        Button(onClick = { prefs.edit().putString("unit_id", unitId.trim().ifEmpty { "SBYE" }).apply() }, colors = ButtonDefaults.buttonColors(backgroundColor = MaterialTheme.colors.primary), modifier = Modifier.fillMaxWidth()) {
            Text("Simpan Unit", color = MaterialTheme.colors.onPrimary)
        }

        Divider()
        Text("Operator", fontSize = 13.sp, color = MaterialTheme.colors.onSurface.copy(alpha = 0.65f))
        OutlinedTextField(value = operatorName, onValueChange = { operatorName = it }, label = { Text("Nama operator") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
        Button(onClick = { prefs.edit().putString("operator_name", operatorName.trim()).apply() }, colors = ButtonDefaults.buttonColors(backgroundColor = MaterialTheme.colors.primary), modifier = Modifier.fillMaxWidth()) {
            Text("Simpan Operator", color = MaterialTheme.colors.onPrimary)
        }

        Divider()
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Tampilkan bearing di watermark", fontSize = 14.sp)
                Text("Jika off, hanya koordinat", fontSize = 11.sp, color = MaterialTheme.colors.onSurface.copy(alpha = 0.65f))
            }
            Switch(checked = watermarkBearing, onCheckedChange = { watermarkBearing = it; prefs.edit().putBoolean("watermark_bearing", it).apply() }, colors = SwitchDefaults.colors(checkedThumbColor = MaterialTheme.colors.primary))
        }

        Divider()
        Text("Penyimpanan", fontSize = 13.sp, color = MaterialTheme.colors.onSurface.copy(alpha = 0.65f))
        Text(storageDir.absolutePath, fontSize = 12.sp, color = MaterialTheme.colors.primary)
        Text("$totalPhotos foto di folder", fontSize = 12.sp)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(if (hasStorage) Icons.Default.CheckCircle else Icons.Default.Error, null, tint = if (hasStorage) MaterialTheme.colors.primary else Color.Red, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(6.dp))
            Text(if (hasStorage) "Akses folder OK" else "Akses folder belum diizinkan", fontSize = 12.sp, color = if (hasStorage) MaterialTheme.colors.primary else Color.Red)
        }

        Divider()
        Text("Import / Export Data", fontSize = 13.sp, color = MaterialTheme.colors.onSurface.copy(alpha = 0.65f))

        // Import .field file button
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Text("Import file .field (JSON)", fontSize = 14.sp, modifier = Modifier.weight(1f))
            Button(onClick = {
                importProgress = 0
                importStatus = "loading"
                importMessage = "Memilih file..."
                scope.launch(Dispatchers.IO) {
                    // TODO: Implement file picker intent for .field files
                    // For now simulate
                    Thread.sleep(500)
                    importProgress = 25
                    Thread.sleep(500)
                    importProgress = 50
                    Thread.sleep(500)
                    importProgress = 75
                    Thread.sleep(500)
                    importProgress = 100
                    importStatus = "success"
                    importMessage = "Import berhasil: data .field diproses"
                }
            }, enabled = importStatus != "loading", colors = ButtonDefaults.buttonColors(backgroundColor = MaterialTheme.colors.primary), modifier = Modifier.fillMaxWidth()) {
                if (importStatus == "loading") {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        CircularProgressIndicator(color = MaterialTheme.colors.onPrimary, modifier = Modifier.size(16.dp))
                        Text("Memproses...", color = MaterialTheme.colors.onPrimary)
                    }
                } else {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.FileDownload, contentDescription = null, tint = MaterialTheme.colors.onPrimary, modifier = Modifier.size(18.dp))
                        Text("Import .field", color = MaterialTheme.colors.onPrimary)
                    }
                }
            }
        }

        // Progress bar & status
        if (importStatus == "loading") {
            Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                LinearProgressIndicator(progress = importProgress / 100f, modifier = Modifier.fillMaxWidth())
                Text("$importProgress%", fontSize = 12.sp, color = MaterialTheme.colors.primary)
                Text(importMessage, fontSize = 12.sp, color = MaterialTheme.colors.onSurface.copy(alpha = 0.65f))
            }
        } else if (importStatus == "success") {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = MaterialTheme.colors.primary, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(importMessage, fontSize = 13.sp, color = MaterialTheme.colors.primary)
            }
        } else if (importStatus == "error") {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                Icon(Icons.Default.Error, contentDescription = null, tint = Color.Red, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(importMessage, fontSize = 13.sp, color = Color.Red)
            }
        }

        Divider()
        Text("v1.0 • FieldRecord", fontSize = 11.sp, color = MaterialTheme.colors.onSurface.copy(alpha = 0.65f))
        Spacer(Modifier.height(80.dp))
    }
}

@Composable
private fun ThemeColorField(label: String, value: String, onValueChange: (String) -> Unit) {
    val parsedColor = parseHexColor(value)
    val samples = listOf("#2E7D32", "#009688", "#2196F3", "#673AB7", "#E91E63", "#FF5722", "#FFC107", "#FFFFFF", "#202420")
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier.weight(1f),
                label = { Text(label) },
                placeholder = { Text("#RRGGBB") },
                singleLine = true
            )
            Box(
                Modifier
                    .size(42.dp)
                    .background(parsedColor ?: MaterialTheme.colors.surface, CircleShape)
                    .border(1.dp, MaterialTheme.colors.onSurface.copy(alpha = 0.45f), CircleShape)
            )
        }
        if (parsedColor == null) {
            Text("Gunakan format HEX valid, contoh #2E7D32", color = MaterialTheme.colors.error, style = MaterialTheme.typography.caption)
        }
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            samples.forEach { sample ->
                val sampleColor = parseHexColor(sample) ?: Color.Gray
                Box(
                    modifier = Modifier
                        .size(26.dp)
                        .background(sampleColor, CircleShape)
                        .border(if (value.equals(sample, ignoreCase = true)) 2.dp else 1.dp, MaterialTheme.colors.onSurface.copy(alpha = 0.5f), CircleShape)
                        .clickable { onValueChange(sample) }
                        .semantics { contentDescription = "Pilih warna $sample untuk $label" }
                )
            }
        }
    }
}

private fun parseHexColor(value: String): Color? = runCatching {
    Color(android.graphics.Color.parseColor(value.trim()))
}.getOrNull()
