package com.fieldrecord.presentation.photo

import android.content.Intent
import androidx.core.content.FileProvider
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Share
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.isSpecified
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import coil.size.Size
import com.fieldrecord.domain.model.FieldPhoto
import com.fieldrecord.domain.model.extractPhotoTags
import com.fieldrecord.domain.model.photoTagSuggestions
import com.fieldrecord.domain.model.applyPhotoTagSuggestion
import com.fieldrecord.domain.model.photoTagColorArgb
import com.fieldrecord.presentation.PhotoNoteText
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale
import kotlinx.coroutines.launch

private val Emerald = Color(0xFF2E7D32)

/**
 * Shared fullscreen photo viewer. Input order must be chronological (oldest first).
 *
 * Layout (atas → bawah):
 *   Header (nama file, tanggal, GPS, index, tombol tutup)
 *   [ruang fleksibel]
 *   Foto (lebar = layar, tinggi = rasio foto)
 *   2 dp
 *   Note (lebar = layar, tombol +/edit kanan)
 *   [ruang fleksibel]
 */
@Composable
fun PhotoFullscreenViewer(
    photos: List<FieldPhoto>,
    initialIndex: Int = 0,
    knownTags: Set<String> = photos.flatMap { extractPhotoTags(it.note) }.toSet(),
    onSaveNote: suspend (photoId: Long, note: String?) -> Unit,
    onDismiss: () -> Unit
) {
    if (photos.isEmpty()) return

    var currentIndex by remember(initialIndex) { mutableIntStateOf(initialIndex.coerceIn(0, photos.lastIndex)) }
    LaunchedEffect(photos.size) {
        currentIndex = currentIndex.coerceIn(0, photos.lastIndex)
    }
    var dragDistance by remember { mutableFloatStateOf(0f) }
    val photo = photos[currentIndex.coerceIn(0, photos.lastIndex)]
    var displayedNote by remember(photo.id, photo.note) { mutableStateOf(photo.note) }
    var showNoteEditor by remember(photo.id) { mutableStateOf(false) }
    var noteDraft by remember(photo.id) { mutableStateOf(photo.note.orEmpty()) }
    var noteSaveError by remember(photo.id) { mutableStateOf<String?>(null) }
    var isSavingNote by remember(photo.id) { mutableStateOf(false) }
    val context = LocalContext.current
    val imageRequest = remember(photo.filePath) {
        ImageRequest.Builder(context).data(File(photo.filePath)).size(Size.ORIGINAL).build()
    }
    val painter = rememberAsyncImagePainter(model = imageRequest, contentScale = ContentScale.Fit)
    val scope = rememberCoroutineScope()
    val tagSuggestions = photoTagSuggestions(noteDraft, knownTags)

    // Sebelum foto selesai load, intrinsicSize = Unspecified; membaca .width langsung → crash.
    // Cek isSpecified dulu, fallback 4:3.
    val intrinsic = painter.intrinsicSize
    val ratio = if (intrinsic.isSpecified && intrinsic.width > 0f && intrinsic.height > 0f) {
        intrinsic.width / intrinsic.height
    } else 4f / 3f

    val swipeModifier = Modifier.pointerInput(photos.size, currentIndex) {
        detectHorizontalDragGestures(
            onDragEnd = {
                if (kotlin.math.abs(dragDistance) > 60f) {
                    // Geser kanan = sebelumnya, geser kiri = berikutnya.
                    currentIndex = if (dragDistance < 0f) {
                        (currentIndex + 1).coerceAtMost(photos.lastIndex)
                    } else {
                        (currentIndex - 1).coerceAtLeast(0)
                    }
                }
                dragDistance = 0f
            },
            onHorizontalDrag = { _, amount -> dragDistance += amount }
        )
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black)
            .zIndex(10f)
    ) {
        Column(Modifier.fillMaxSize()) {
            PhotoMetadataHeader(
                photo, currentIndex, photos.size,
                onShare = { sharePhoto(context, photo) },
                onClose = onDismiss
            )

            // Foto + note di-center vertikal sebagai satu blok.
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .pointerInput(Unit) { detectTapGestures { onDismiss() } } // tap area hitam = tutup
                    .then(swipeModifier),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = false)
                        .aspectRatio(ratio)
                        .pointerInput(Unit) { detectTapGestures { } } // tap foto tidak menutup
                ) {
                    Image(
                        painter = painter,
                        contentDescription = viewerPhotoDescription(photo),
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                Spacer(Modifier.height(2.dp))

                // Hanya munculkan NoteBar jika sudah ada note.
                if (!displayedNote.isNullOrBlank()) {
                    NoteBar(
                        note = displayedNote!!,
                        onEdit = {
                            noteDraft = displayedNote.orEmpty()
                            noteSaveError = null
                            showNoteEditor = true
                        }
                    )
                }
            }
        }

        // Jika belum ada note, munculkan tombol melayang (FAB) di pojok kanan bawah.
        if (displayedNote.isNullOrBlank()) {
            FloatingActionButton(
                onClick = {
                    noteDraft = ""
                    noteSaveError = null
                    showNoteEditor = true
                },
                backgroundColor = Emerald,
                contentColor = Color.White,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(24.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Tambah note")
            }
        }

        if (showNoteEditor) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.5f))
                    .pointerInput(Unit) { detectTapGestures { } }
                    .zIndex(20f),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    modifier = Modifier.fillMaxWidth(0.9f),
                    shape = RoundedCornerShape(12.dp),
                    elevation = 8.dp,
                    backgroundColor = Color.White
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Text(
                            "Catatan Foto",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = Color(0xFF202020)
                        )
                        Spacer(Modifier.height(12.dp))
                        OutlinedTextField(
                            value = noteDraft,
                            onValueChange = { noteDraft = it },
                            modifier = Modifier.fillMaxWidth().heightIn(min = 120.dp),
                            placeholder = { Text("Tulis catatan untuk foto ini...") },
                            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                            colors = TextFieldDefaults.outlinedTextFieldColors(
                                textColor = Color(0xFF202020),
                                focusedBorderColor = Emerald,
                                unfocusedBorderColor = Color(0xFFCCCCCC),
                                cursorColor = Emerald
                            )
                        )
                        if (tagSuggestions.isNotEmpty()) {
                            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())) {
                                tagSuggestions.forEach { tag ->
                                    TextButton(onClick = { noteDraft = applyPhotoTagSuggestion(noteDraft, tag) }) {
                                        Text("#$tag", color = Color(photoTagColorArgb(tag)))
                                    }
                                }
                            }
                        }
                        noteSaveError?.let {
                            Text(
                                it,
                                color = MaterialTheme.colors.error,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                            horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TextButton(onClick = { showNoteEditor = false }) {
                                Text("BATAL", color = Color.Gray, fontWeight = FontWeight.Bold)
                            }
                            Spacer(Modifier.width(8.dp))
                            Button(
                                enabled = !isSavingNote,
                                colors = ButtonDefaults.buttonColors(backgroundColor = Emerald),
                                onClick = {
                                    scope.launch {
                                        isSavingNote = true
                                        noteSaveError = null
                                        val normalized = noteDraft.trim().takeIf { it.isNotEmpty() }
                                        try {
                                            onSaveNote(photo.id, normalized)
                                            displayedNote = normalized
                                            showNoteEditor = false
                                        } catch (error: Exception) {
                                            noteSaveError = error.message ?: "Gagal menyimpan. Coba lagi."
                                        } finally {
                                            isSavingNote = false
                                        }
                                    }
                                }
                            ) {
                                Text(
                                    if (isSavingNote) "SIMPAN..." else "SIMPAN",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/** Note selebar layar, tampil di bawah foto hanya jika ada isinya. */
@Composable
private fun NoteBar(note: String, onEdit: () -> Unit) {
    Surface(
        color = Color.White,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp, max = 160.dp)
            .clickable(onClick = onEdit)
    ) {
        Row(verticalAlignment = Alignment.Top, modifier = Modifier.padding(12.dp)) {
            PhotoNoteText(
                note = note,
                modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()),
                defaultColor = Color(0xFF202020),
                style = MaterialTheme.typography.body1.copy(fontSize = 16.sp)
            )
            IconButton(onClick = onEdit, modifier = Modifier.size(32.dp)) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = "Edit note",
                    tint = Emerald
                )
            }
        }
    }
}

@Composable
private fun PhotoMetadataHeader(
    photo: FieldPhoto,
    currentIndex: Int,
    total: Int,
    onShare: () -> Unit,
    onClose: () -> Unit
) {
    val hasGps = photo.latitude != 0.0 || photo.longitude != 0.0
    val dateText = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(photo.timestamp)
    val gpsText = if (hasGps) {
        String.format(Locale.US, "%.5f, %.5f  •  %.0f°", photo.latitude, photo.longitude, photo.bearing.toDouble())
    } else "GPS tidak tersedia"
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.Black)
            .padding(start = 16.dp, end = 4.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                photo.fileName,
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(dateText, color = Color.White.copy(alpha = 0.8f), fontSize = 13.sp)
            Text(
                gpsText,
                color = Color.White.copy(alpha = 0.8f),
                fontSize = 13.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Text(
            "${currentIndex + 1}/$total",
            color = Color.White,
            fontSize = 13.sp,
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .background(Color.White.copy(alpha = 0.18f))
                .padding(horizontal = 10.dp, vertical = 4.dp)
        )
        IconButton(onClick = onShare) {
            Icon(Icons.Default.Share, contentDescription = "Bagikan foto via WhatsApp", tint = Color.White)
        }
        IconButton(onClick = onClose) {
            Icon(Icons.Default.Close, contentDescription = "Tutup preview", tint = Color.White)
        }
    }
}

private fun sharePhoto(context: android.content.Context, photo: FieldPhoto) {
    val file = File(photo.filePath)
    if (!file.isFile) {
        android.widget.Toast.makeText(context, "File foto tidak ditemukan", android.widget.Toast.LENGTH_SHORT).show()
        return
    }
    runCatching {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val sendIntent = Intent(Intent.ACTION_SEND).apply {
            type = "image/jpeg"
            putExtra(Intent.EXTRA_STREAM, uri)
            val shareText = buildString {
                append("Koordinat: ${photo.latitude}, ${photo.longitude}")
                if (!photo.note.isNullOrBlank()) {
                    append("\n\n").append(photo.note)
                }
            }
            putExtra(Intent.EXTRA_TEXT, shareText)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(sendIntent, "Bagikan foto"))
    }.onFailure {
        android.widget.Toast.makeText(context, "Tidak dapat membagikan foto: ${it.message}", android.widget.Toast.LENGTH_LONG).show()
    }
}

private fun viewerPhotoDescription(photo: FieldPhoto) =
    "Foto ${photo.fileName}. Geser kanan untuk foto sebelumnya, kiri untuk foto berikutnya."
