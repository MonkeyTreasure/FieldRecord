package com.fieldrecord.presentation.video

import android.media.MediaMetadataRetriever
import android.media.MediaPlayer
import android.net.Uri
import android.widget.MediaController
import android.widget.VideoView
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import androidx.core.content.FileProvider
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun VideoPlayerDialog(
    file: File,
    onDismiss: () -> Unit,
    onSwipeLeft: (() -> Unit)? = null,
    onSwipeRight: (() -> Unit)? = null
) {
    val context = LocalContext.current
    var isFullscreen by remember(file.absolutePath) { mutableStateOf(false) }
    val videoZoomState = remember(file.absolutePath) { mutableStateOf(MediaZoomTransform()) }
    val videoViewportSize = remember(file.absolutePath) { mutableStateOf(IntSize.Zero) }
    val videoTransformState = rememberTransformableState { zoomChange, panChange, _ ->
        val viewport = videoViewportSize.value
        videoZoomState.value = updateMediaZoom(
            current = videoZoomState.value,
            zoomChange = zoomChange,
            panX = panChange.x,
            panY = panChange.y,
            centroidX = viewport.width / 2f,
            centroidY = viewport.height / 2f,
            viewportWidth = viewport.width.toFloat(),
            viewportHeight = viewport.height.toFloat()
        )
    }
    var dragDistance by remember(file.absolutePath) { mutableStateOf(0f) }
    val dialogWindow = (LocalView.current.parent as? DialogWindowProvider)?.window
    DisposableEffect(dialogWindow, isFullscreen) {
        val window = dialogWindow
        if (window == null) {
            onDispose { }
        } else {
            val controller = WindowInsetsControllerCompat(window, window.decorView)
            val systemBars = WindowInsetsCompat.Type.systemBars()
            val wereSystemBarsVisible = ViewCompat.getRootWindowInsets(window.decorView)
                ?.isVisible(systemBars) ?: true
            if (isFullscreen) {
                controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                controller.hide(systemBars)
            }
            onDispose {
                if (isFullscreen && wereSystemBarsVisible) controller.show(systemBars)
            }
        }
    }
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

    val videoAspectRatio by produceState(initialValue = 16f / 9f, key1 = file.absolutePath) {
        value = withContext(Dispatchers.IO) { readVideoAspectRatio(file) }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = !isFullscreen
        )
    ) {
        val density = LocalDensity.current
        val backgroundInteraction = remember { MutableInteractionSource() }
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
                .clickable(
                    interactionSource = backgroundInteraction,
                    indication = null,
                    onClick = onDismiss
                )
                .pointerInput(file.absolutePath, onSwipeLeft, onSwipeRight) {
                    detectHorizontalDragGestures(
                        onDragEnd = {
                            if (shouldNavigateGallerySwipe(videoZoomState.value.scale) && kotlin.math.abs(dragDistance) > 60f) {
                                if (dragDistance < 0f) onSwipeLeft?.invoke() else onSwipeRight?.invoke()
                            }
                            dragDistance = 0f
                        },
                        onHorizontalDrag = { _, amount ->
                            if ((onSwipeLeft != null || onSwipeRight != null) && shouldNavigateGallerySwipe(videoZoomState.value.scale)) {
                                dragDistance += amount
                            }
                        }
                    )
                }
        ) {
            val aspect = videoAspectRatio.takeIf { it.isFinite() && it > 0f } ?: (16f / 9f)
            val maxWidthPx = constraints.maxWidth.toFloat()
            val maxHeightPx = constraints.maxHeight.toFloat()
            val playerWidthPx = minOf(maxWidthPx, maxHeightPx * aspect).coerceAtLeast(1f)
            val playerHeightPx = (playerWidthPx / aspect).coerceAtLeast(1f)
            val playerWidth = with(density) { playerWidthPx.toDp() }
            val playerHeight = with(density) { playerHeightPx.toDp() }

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
                    modifier = Modifier
                        .size(width = playerWidth, height = playerHeight)
                        .align(Alignment.Center)
                        .onSizeChanged { videoViewportSize.value = it }
                        .transformable(
                            state = videoTransformState,
                            canPan = { !shouldNavigateGallerySwipe(videoZoomState.value.scale) }
                        )
                        .graphicsLayer {
                            scaleX = videoZoomState.value.scale
                            scaleY = videoZoomState.value.scale
                            translationX = videoZoomState.value.offsetX
                            translationY = videoZoomState.value.offsetY
                        }
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
            IconButton(
                onClick = { isFullscreen = !isFullscreen },
                modifier = Modifier.align(Alignment.TopEnd).padding(8.dp)
            ) {
                Icon(
                    imageVector = if (isFullscreen) Icons.Default.FullscreenExit else Icons.Default.Fullscreen,
                    contentDescription = if (isFullscreen) "Keluar dari layar penuh" else "Layar penuh",
                    tint = Color.White
                )
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

private fun readVideoAspectRatio(file: File): Float {
    if (!file.isFile || file.length() == 0L) return 16f / 9f
    val retriever = MediaMetadataRetriever()
    return try {
        retriever.setDataSource(file.absolutePath)
        val width = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)?.toIntOrNull() ?: 0
        val height = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)?.toIntOrNull() ?: 0
        val rotation = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION)?.toIntOrNull() ?: 0
        val displayWidth = if (rotation % 180 != 0) height else width
        val displayHeight = if (rotation % 180 != 0) width else height
        if (displayWidth > 0 && displayHeight > 0) displayWidth.toFloat() / displayHeight else 16f / 9f
    } catch (_: Exception) {
        16f / 9f
    } finally {
        runCatching { retriever.release() }
    }
}
