package com.fieldrecord.presentation.video

import android.media.MediaPlayer
import android.net.Uri
import android.widget.MediaController
import android.widget.VideoView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.FileProvider
import java.io.File

@Composable
internal fun VideoPlayerDialog(file: File, onDismiss: () -> Unit) {
    val context = LocalContext.current
    var playbackError by remember(file.absolutePath) { mutableStateOf<String?>(null) }
    val videoUri: Uri? = remember(file.absolutePath) {
        runCatching {
            if (file.isFile && file.length() > 0L) {
                FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            } else {
                null
            }
        }.getOrNull()
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(Modifier.fillMaxSize().background(Color.Black)) {
            if (videoUri != null) {
                val playerView = remember(file.absolutePath) {
                    VideoView(context).apply {
                        val controls = MediaController(context)
                        controls.setAnchorView(this)
                        setMediaController(controls)
                        setOnPreparedListener { player: MediaPlayer ->
                            player.isLooping = false
                            start()
                        }
                        setOnErrorListener { _, _, _ ->
                            playbackError = "Video tidak dapat diputar"
                            true
                        }
                        setVideoURI(videoUri)
                    }
                }
                AndroidView(
                    factory = { playerView },
                    modifier = Modifier.fillMaxSize()
                )
                DisposableEffect(playerView) {
                    onDispose { playerView.stopPlayback() }
                }
            } else {
                Text(
                    text = "File video tidak tersedia",
                    color = Color.White,
                    modifier = Modifier.align(Alignment.Center)
                )
            }

            IconButton(
                onClick = onDismiss,
                modifier = Modifier.align(Alignment.TopStart).padding(8.dp)
            ) {
                Icon(Icons.Default.Close, contentDescription = "Tutup pemutar video", tint = Color.White)
            }

            playbackError?.let { message ->
                Text(
                    text = message,
                    color = Color.White,
                    fontSize = 16.sp,
                    modifier = Modifier.align(Alignment.Center)
                        .background(Color.Black.copy(alpha = 0.78f))
                        .padding(16.dp)
                )
            }
        }
    }
}
