package com.fieldrecord.presentation.gallery

import android.content.ClipData
import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.border
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material.icons.filled.Share
import androidx.compose.runtime.*
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.paging.compose.collectAsLazyPagingItems
import coil.compose.rememberAsyncImagePainter
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.fieldrecord.data.local.PhotoTagColorStore
import com.fieldrecord.domain.model.FieldPhoto
import com.fieldrecord.domain.model.extractPhotoTags
import com.fieldrecord.presentation.camera.PhotoFileManager
import com.fieldrecord.presentation.photo.PhotoFullscreenViewer
import com.fieldrecord.presentation.video.VideoPlayerDialog
import java.io.File
import java.util.Calendar

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun GalleryScreen(
    viewModel: GalleryViewModel
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val tagColorStore = remember(context) { PhotoTagColorStore(context) }
    val haptics = LocalHapticFeedback.current
    val lazyPagingItems = viewModel.pager.collectAsLazyPagingItems()
    val monthLabel = viewModel.getCurrentMonthLabel()
    var selectedIds by remember { mutableStateOf<Set<Long>>(emptySet()) }
    var fullscreenPhotos by remember { mutableStateOf<List<FieldPhoto>?>(null) }
    var fullscreenIndex by remember { mutableIntStateOf(0) }
    var selectedVideo by remember { mutableStateOf<File?>(null) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    var pendingCsvContent by remember { mutableStateOf<String?>(null) }
    var isPreparingCsv by remember { mutableStateOf(false) }
    val csvDocumentLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/csv")
    ) { uri ->
        val csv = pendingCsvContent
        pendingCsvContent = null
        if (uri != null && csv != null) {
            scope.launch {
                try {
                    val written = withContext(Dispatchers.IO) {
                        context.contentResolver.openOutputStream(uri)?.use { output ->
                            output.write(csv.toByteArray(Charsets.UTF_8))
                            true
                        } ?: false
                    }
                    Toast.makeText(
                        context,
                        if (written) "CSV note berhasil disimpan" else "File CSV tidak dapat dibuat",
                        if (written) Toast.LENGTH_SHORT else Toast.LENGTH_LONG
                    ).show()
                } catch (error: Exception) {
                    Toast.makeText(context, "Ekspor CSV gagal: ${error.message ?: "kesalahan penyimpanan"}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    val loadedPhotos = remember(lazyPagingItems.itemCount) {
        buildList {
            for (i in 0 until lazyPagingItems.itemCount) {
                lazyPagingItems.peek(i)?.let { add(it) }
            }
        }
    }
    val chronologicalPhotos = remember(loadedPhotos) { loadedPhotos.sortedBy { it.timestamp } }
    val currentMonth by viewModel.currentMonth.collectAsState()
    val allVideos by viewModel.videos.collectAsState()
    val monthRange = remember(currentMonth.timeInMillis) {
        val start = (currentMonth.clone() as Calendar).apply {
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val end = (currentMonth.clone() as Calendar).apply {
            set(Calendar.DAY_OF_MONTH, getActualMaximum(Calendar.DAY_OF_MONTH))
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
            set(Calendar.MILLISECOND, 999)
        }
        start.timeInMillis to end.timeInMillis
    }
    val monthVideos = remember(allVideos, monthRange) {
        videosInDateRange(allVideos, monthRange.first, monthRange.second)
    }
    val mediaTimeline = remember(loadedPhotos, monthVideos) {
        buildGalleryTimeline(loadedPhotos, monthVideos)
    }
    val selectionMode = selectedIds.isNotEmpty()

    LaunchedEffect(Unit) {
        viewModel.syncPhotosFromStorage(context)
        viewModel.syncVideosFromStorage()
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) viewModel.syncVideosFromStorage()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Sticky header — tidak ikut scroll
            androidx.compose.material.Surface(elevation = 4.dp) {
                if (selectionMode) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("${selectedIds.size} dipilih", color = MaterialTheme.colors.onSurface)
                        Row {
                            IconButton(onClick = {
                                val selected = loadedPhotos.filter { it.id in selectedIds }
                                showMultiShareDialog(context, selected)
                            }) { Icon(Icons.Default.Share, "Bagikan") }
                            IconButton(onClick = {
                                selectedIds = if (selectedIds.size == loadedPhotos.size) emptySet()
                                else loadedPhotos.map { it.id }.toSet()
                            }) { Icon(Icons.Default.SelectAll, "Pilih semua") }
                            IconButton(onClick = { showDeleteConfirm = true }) {
                                Icon(Icons.Default.Delete, "Hapus foto terpilih", tint = Color.White)
                            }
                            IconButton(onClick = { selectedIds = emptySet() }) {
                                Icon(Icons.Default.Close, "Batalkan pilihan")
                            }
                        }
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(monthLabel, style = MaterialTheme.typography.subtitle1)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            IconButton(
                                enabled = !isPreparingCsv,
                                onClick = {
                                    scope.launch {
                                        isPreparingCsv = true
                                        try {
                                            val csv = viewModel.exportPhotoNotesCsv()
                                            if (csv == null) {
                                                Toast.makeText(context, "Belum ada foto dengan note untuk diekspor", Toast.LENGTH_SHORT).show()
                                            } else {
                                                pendingCsvContent = "\uFEFF$csv"
                                                csvDocumentLauncher.launch("FieldRecord_photo_notes.csv")
                                            }
                                        } catch (error: Exception) {
                                            Toast.makeText(context, "Gagal menyiapkan CSV: ${error.message ?: "kesalahan data"}", Toast.LENGTH_LONG).show()
                                        } finally {
                                            isPreparingCsv = false
                                        }
                                    }
                                }
                            ) {
                                Icon(Icons.Default.FileDownload, contentDescription = "Ekspor note foto ke CSV")
                            }
                            IconButton(onClick = { viewModel.previousMonth() }) {
                                Icon(Icons.Default.ChevronLeft, "Bulan sebelumnya")
                            }
                            IconButton(onClick = { viewModel.nextMonth() }) {
                                Icon(Icons.Default.ChevronRight, "Bulan berikutnya")
                            }
                        }
                    }
                }
            }
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                contentPadding = PaddingValues(4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.weight(1f).fillMaxWidth()
            ) {
                items(mediaTimeline, key = { it.key }) { media ->
                    when (media) {
                        is GalleryMediaItem.Photo -> {
                            val photo = media.photo
                            val selected = photo.id in selectedIds
                            val firstTag = remember(photo.note) { extractPhotoTags(photo.note).firstOrNull() }
                            val tagColor = if (firstTag == null) null else remember(firstTag) { tagColorStore.colorFor(firstTag) }
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .aspectRatio(1f)
                                    .border(
                                        width = if (selected) 3.dp else 0.dp,
                                        color = if (selected) GallerySelectionTheme.borderColor else Color.Transparent,
                                        shape = RoundedCornerShape(4.dp)
                                    )
                                    .combinedClickable(
                                        onClick = {
                                            if (selectionMode) {
                                                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                                selectedIds = if (selected) selectedIds - photo.id else selectedIds + photo.id
                                            } else {
                                                fullscreenPhotos = chronologicalPhotos
                                                fullscreenIndex = chronologicalPhotos.indexOfFirst { it.id == photo.id }.coerceAtLeast(0)
                                            }
                                        },
                                        onLongClick = {
                                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                            selectedIds = if (selected) selectedIds - photo.id else selectedIds + photo.id
                                        }
                                    ),
                                backgroundColor = if (selected) Color(0xFF673AB7).copy(alpha = 0.7f) else MaterialTheme.colors.surface,
                                elevation = 2.dp
                            ) {
                                Box(
                                    Modifier.fillMaxSize().background(
                                        if (selected) Color(0xFF673AB7).copy(alpha = 0.7f) else Color.Black
                                    )
                                ) {
                                    androidx.compose.foundation.Image(
                                        painter = rememberAsyncImagePainter(File(photo.filePath)),
                                        contentDescription = photo.fileName,
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Fit
                                    )
                                    if (firstTag != null && tagColor != null) {
                                        Box(
                                            modifier = Modifier
                                                .align(Alignment.BottomEnd)
                                                .padding(4.dp)
                                                .size(11.dp)
                                                .clip(RoundedCornerShape(3.dp))
                                                .background(Color(tagColor), RoundedCornerShape(3.dp))
                                                .border(0.75.dp, Color.White.copy(alpha = 0.9f), RoundedCornerShape(3.dp))
                                                .semantics { contentDescription = "Photo tagged #$firstTag" }
                                        )
                                    }
                                }
                            }
                        }
                        is GalleryMediaItem.Video -> {
                            GalleryVideoTile(
                                video = media.video,
                                onClick = {
                                    if (selectionMode) {
                                        Toast.makeText(context, "Pilih foto untuk aksi bagikan atau hapus", Toast.LENGTH_SHORT).show()
                                    } else {
                                        selectedVideo = File(media.video.filePath)
                                    }
                                },
                                onLongClick = {
                                    Toast.makeText(context, "Video dapat diputar dari galeri", Toast.LENGTH_SHORT).show()
                                }
                            )
                        }
                    }
                }
            }
        }

        if (showDeleteConfirm) {
            AlertDialog(
                onDismissRequest = { showDeleteConfirm = false },
                title = { Text("Hapus foto terpilih?") },
                text = { Text("${selectedIds.size} foto akan dihapus permanen dari galeri dan penyimpanan aplikasi.") },
                confirmButton = {
                    TextButton(onClick = {
                        val targets = loadedPhotos.filter { it.id in selectedIds }
                        showDeleteConfirm = false
                        scope.launch {
                            viewModel.deleteSelected(targets)
                            selectedIds = emptySet()
                            lazyPagingItems.refresh()
                        }
                    }) { Text("Hapus", color = MaterialTheme.colors.error) }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteConfirm = false }) { Text("Batal") }
                }
            )
        }

        selectedVideo?.let { video ->
            VideoPlayerDialog(file = video, onDismiss = { selectedVideo = null })
        }

        fullscreenPhotos?.let { photos ->
            PhotoFullscreenViewer(
                photos = photos,
                initialIndex = fullscreenIndex,
                knownTags = lazyPagingItems.itemSnapshotList.items.filterNotNull().flatMap { com.fieldrecord.domain.model.extractPhotoTags(it.note) }.toSet(),
                onSaveNote = { photoId, note ->
                    viewModel.updatePhotoNote(photoId, note)
                    lazyPagingItems.refresh()
                },
                onDismiss = { fullscreenPhotos = null }
            )
        }
    }
}

private fun showMultiShareDialog(context: android.content.Context, photos: List<FieldPhoto>) {
    if (photos.isEmpty()) return
    val dialog = android.app.AlertDialog.Builder(context)
        .setTitle("Pilih Opsi Berbagi (${photos.size} Foto)")
        .setItems(arrayOf("Share Foto & Note Saja", "Share Data dengan .field")) { _, which ->
            if (which == 0) {
                sharePhotosToWhatsApp(context, photos)
            } else {
                sharePhotosAsFieldFile(context, photos)
            }
        }
        .setOnCancelListener { }
        .create()
    dialog.setCanceledOnTouchOutside(true)
    dialog.show()
}

private fun sharePhotosAsFieldFile(context: android.content.Context, photos: List<FieldPhoto>) {
    val combinedData = photos.joinToString(separator = "\n---\n") { photo ->
        val photoFile = File(photo.filePath)
        val base64Photo = if (photoFile.exists()) {
            android.util.Base64.encodeToString(photoFile.readBytes(), android.util.Base64.NO_WRAP)
        } else ""
        val photographer = PhotoFileManager.photographerStampFromFilename(photo.fileName).orEmpty()
        "PhotoName=${photo.fileName}\nPhotographer=$photographer\nPhotoStorage=FieldRecord/Photo/${photo.fileName}\nLat=${photo.latitude}\nLon=${photo.longitude}\nTimestamp=${photo.timestamp}\nNote=${photo.note ?: ""}\nPhotoBase64=$base64Photo"
    }
    val file = File(context.cacheDir, "multivisit_${System.currentTimeMillis()}.field")
    file.writeText(combinedData)
    val fileUri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "application/octet-stream"
        putExtra(Intent.EXTRA_STREAM, fileUri)
        clipData = ClipData.newUri(context.contentResolver, file.name, fileUri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    try {
        context.startActivity(Intent.createChooser(intent, "Bagikan data .field"))
    } catch (e: Exception) {
        android.widget.Toast.makeText(context, "Gagal membagikan: ${e.message}", android.widget.Toast.LENGTH_LONG).show()
    }
}

private fun sharePhotosToWhatsApp(context: android.content.Context, photos: List<FieldPhoto>) {
    if (photos.isEmpty()) return
    val uris = photos.mapNotNull { photo ->
        val file = File(photo.filePath)
        if (!file.exists()) null else FileProvider.getUriForFile(
            context, "${context.packageName}.fileprovider", file
        )
    }
    if (uris.isEmpty()) return
    val locationLink = "https://fieldrecord.redmonkey.my.id/share?lat=" + photos.joinToString(separator = "|") { "${it.latitude},${it.longitude},${android.net.Uri.encode(it.note ?: "")}" }
    val photoStamps = photos.joinToString(separator = "\n\n") { photo ->
        val photographer = PhotoFileManager.photographerStampFromFilename(photo.fileName)
        buildString {
            append("Nama foto: ${photo.fileName}")
            if (!photographer.isNullOrBlank()) append("\nDiambil oleh: $photographer")
        }
    }
    val combinedText = "$locationLink\n\n$photoStamps"
    val intent = Intent(Intent.ACTION_SEND_MULTIPLE).apply {
        type = "image/jpeg"
        putParcelableArrayListExtra(Intent.EXTRA_STREAM, ArrayList(uris))
        putExtra(Intent.EXTRA_TEXT, combinedText)
        setPackage("com.whatsapp")
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    try {
        context.startActivity(intent)
    } catch (_: Exception) {
        val fallback = Intent.createChooser(intent, "Bagikan foto")
        fallback.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        context.startActivity(fallback)
    }
}
