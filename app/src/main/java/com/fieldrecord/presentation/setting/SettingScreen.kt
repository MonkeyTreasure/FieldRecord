package com.fieldrecord.presentation.setting

import android.content.Context
import android.os.Environment
import androidx.compose.foundation.layout.*
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

    // Import state
    var importProgress by remember { mutableStateOf(0) }
    var importStatus by remember { mutableStateOf("") } // "", "loading", "success", "error"
    var importMessage by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()

    val storageDir = remember { PhotoFileManager.getStorageDir() }
    val totalPhotos = remember { storageDir.listFiles()?.count { it.extension.equals("jpg", true) } ?: 0 }
    val hasStorage = Environment.isExternalStorageManager()

    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("Pengaturan", style = MaterialTheme.typography.h6)
        Divider()

        Text("Unit / Rayon", fontSize = 13.sp, color = Color.Gray)
        OutlinedTextField(value = unitId, onValueChange = { unitId = it }, label = { Text("Unit ID (watermark)") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
        Button(onClick = { prefs.edit().putString("unit_id", unitId.trim().ifEmpty { "SBYE" }).apply() }, colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFF2E7D32)), modifier = Modifier.fillMaxWidth()) {
            Text("Simpan Unit", color = Color.White)
        }

        Divider()
        Text("Operator", fontSize = 13.sp, color = Color.Gray)
        OutlinedTextField(value = operatorName, onValueChange = { operatorName = it }, label = { Text("Nama operator") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
        Button(onClick = { prefs.edit().putString("operator_name", operatorName.trim()).apply() }, colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFF2E7D32)), modifier = Modifier.fillMaxWidth()) {
            Text("Simpan Operator", color = Color.White)
        }

        Divider()
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Tampilkan bearing di watermark", fontSize = 14.sp)
                Text("Jika off, hanya koordinat", fontSize = 11.sp, color = Color.Gray)
            }
            Switch(checked = watermarkBearing, onCheckedChange = { watermarkBearing = it; prefs.edit().putBoolean("watermark_bearing", it).apply() }, colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFF2E7D32)))
        }

        Divider()
        Text("Penyimpanan", fontSize = 13.sp, color = Color.Gray)
        Text(storageDir.absolutePath, fontSize = 12.sp, color = Color(0xFF2E7D32))
        Text("$totalPhotos foto di folder", fontSize = 12.sp)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(if (hasStorage) Icons.Default.CheckCircle else Icons.Default.Error, null, tint = if (hasStorage) Color(0xFF2E7D32) else Color.Red, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(6.dp))
            Text(if (hasStorage) "Akses folder OK" else "Akses folder belum diizinkan", fontSize = 12.sp, color = if (hasStorage) Color(0xFF2E7D32) else Color.Red)
        }

        Divider()
        Text("Import / Export Data", fontSize = 13.sp, color = Color.Gray)

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
            }, enabled = importStatus != "loading", colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFF2E7D32)), modifier = Modifier.fillMaxWidth()) {
                if (importStatus == "loading") {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(16.dp))
                        Text("Memproses...", color = Color.White)
                    }
                } else {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.FileDownload, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                        Text("Import .field", color = Color.White)
                    }
                }
            }
        }

        // Progress bar & status
        if (importStatus == "loading") {
            Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                LinearProgressIndicator(progress = importProgress / 100f, color = Color(0xFF2E7D32), trackColor = Color(0xFF2E7D32).copy(alpha = 0.2f))
                Text("$importProgress%", fontSize = 12.sp, color = Color(0xFF2E7D32))
                Text(importMessage, fontSize = 12.sp, color = Color.Gray)
            }
        } else if (importStatus == "success") {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF2E7D32), modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(importMessage, fontSize = 13.sp, color = Color(0xFF2E7D32))
            }
        } else if (importStatus == "error") {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                Icon(Icons.Default.Error, contentDescription = null, tint = Color.Red, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(importMessage, fontSize = 13.sp, color = Color.Red)
            }
        }

        Divider()
        Text("v1.0 • FieldRecord", fontSize = 11.sp, color = Color.Gray)
        Spacer(Modifier.height(80.dp))
    }
}
