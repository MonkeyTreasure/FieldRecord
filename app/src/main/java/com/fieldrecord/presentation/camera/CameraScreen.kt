package com.fieldrecord.presentation.camera

import com.fieldrecord.LocalAppIconColor
import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.view.OrientationEventListener
import android.view.Surface
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.FocusMeteringAction
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.core.ZoomState
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateCentroidSize
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.FlashAuto
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Grid3x3
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.HighQuality
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Exposure
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import com.fieldrecord.domain.model.FieldPhoto
import com.fieldrecord.presentation.photo.PhotoFullscreenViewer
import com.fieldrecord.location.LocationStore
import com.fieldrecord.domain.repository.FieldPhotoRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.delay
import javax.inject.Inject
import coil.compose.rememberAsyncImagePainter
import java.io.File

@HiltViewModel
class CameraViewModel @Inject constructor(
    private val repository: FieldPhotoRepository
) : ViewModel() {
    
    fun capturePhoto(
        context: Context,
        imageCapture: ImageCapture,
        latitude: Double,
        longitude: Double,
        bearing: Float,
        targetRotation: Int,
        onSuccess: (FieldPhoto) -> Unit,
        onError: (String) -> Unit
    ) {
        val userName = context.getSharedPreferences("field_record_prefs", Context.MODE_PRIVATE)
            .getString("photographer_name", "")
        val outputFile = PhotoFileManager.getOutputFile(userName)
        
        val view = (context as? android.app.Activity)?.window?.decorView
        view?.performHapticFeedback(android.view.HapticFeedbackConstants.LONG_PRESS)
        val outputOptions = ImageCapture.OutputFileOptions.Builder(outputFile).build()
        
        imageCapture.takePicture(
            outputOptions,
            androidx.core.content.ContextCompat.getMainExecutor(context),
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    CoroutineScope(Dispatchers.IO).launch {
                        val saved = PhotoFileManager.saveLocationWithExifAndWatermark(
                            context, outputFile, latitude, longitude, bearing, targetRotation
                        )
                        if (!saved) {
                            kotlinx.coroutines.withContext(Dispatchers.Main) {
                                onError("Gagal menyimpan metadata foto")
                            }
                            return@launch
                        }
                        val photo = FieldPhoto(
                            id = 0,
                            filePath = outputFile.absolutePath,
                            fileName = outputFile.name,
                            latitude = latitude,
                            longitude = longitude,
                            bearing = bearing,
                            timestamp = System.currentTimeMillis()
                        )
                        val id = repository.insertPhoto(photo)
                        kotlinx.coroutines.withContext(Dispatchers.Main) {
                            onSuccess(photo.copy(id = id))
                        }
                    }
                }
                override fun onError(exc: ImageCaptureException) {
                    onError(exc.message ?: "Capture failed")
                }
            }
        )
    }

    suspend fun updatePhotoNote(photoId: Long, note: String?) {
        repository.updatePhotoNote(photoId, note)
    }
}

@Composable
fun CameraScreen(
    onPhotoCaptured: (FieldPhoto) -> Unit
) {
    val viewModel: CameraViewModel = hiltViewModel()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val configuration = LocalConfiguration.current
    val viewfinderLandscape = configuration.screenWidthDp > configuration.screenHeightDp
    val previewView = remember {
        PreviewView(context).apply { implementationMode = PreviewView.ImplementationMode.COMPATIBLE }
    }
    var imageCapture by remember { mutableStateOf<ImageCapture?>(null) }
    var previewUseCase by remember { mutableStateOf<Preview?>(null) }
    var camera by remember { mutableStateOf<Camera?>(null) }
    var cameraProviderRef by remember { mutableStateOf<ProcessCameraProvider?>(null) }
    var zoomState by remember { mutableStateOf<ZoomState?>(null) }
    var zoomRatio by remember { mutableFloatStateOf(1f) }
    var flashSetting by remember { mutableStateOf(FlashSetting.OFF) }
    var lensFacing by remember { mutableIntStateOf(CameraSelector.LENS_FACING_BACK) }
    val preferences = remember { context.getSharedPreferences("camera_controls", android.content.Context.MODE_PRIVATE) }
    var timerSeconds by remember { mutableIntStateOf(preferences.getInt("timer_seconds", 0)) }
    var prioritizeQuality by remember { mutableStateOf(preferences.getBoolean("quality", false)) }
    var gridEnabled by remember { mutableStateOf(preferences.getBoolean("grid", false)) }
    var exposureIndex by remember { mutableIntStateOf(0) }
    var showExposureControl by remember { mutableStateOf(false) }
    var focusPoint by remember { mutableStateOf<Offset?>(null) }
    var countdown by remember { mutableIntStateOf(0) }
    var isCapturing by remember { mutableStateOf(false) }
    var isVideoMode by remember { mutableStateOf(false) }
    var isCameraBound by remember { mutableStateOf(false) }
    var targetRotation by remember {
        mutableIntStateOf(previewView.display?.rotation ?: Surface.ROTATION_0)
    }
    var unitId by remember { mutableStateOf("SBYE") }
    var photographerName by remember { mutableStateOf("") }
    var watermarkTextColor by remember { mutableStateOf("#0000FF") }
    var watermarkDatePattern by remember { mutableStateOf("dd.MM.yyyy, HH:mm") }
    var watermarkBackgroundColor by remember { mutableStateOf("#FFFFFF") }
    var watermarkBackgroundTransparent by remember { mutableStateOf(false) }
    var watermarkBackgroundAlpha by remember { mutableFloatStateOf(0.45f) }
    var showUnitSettings by remember { mutableStateOf(false) }

    val captureScope = rememberCoroutineScope()
    val availableZoomPresets = zoomState?.let { supportedZoomPresets(it.minZoomRatio, it.maxZoomRatio) }.orEmpty()

    LaunchedEffect(context) {
        val prefs = context.getSharedPreferences("field_record_prefs", Context.MODE_PRIVATE)
        unitId = prefs.getString("watermark_line1", prefs.getString("unit_id", "SBYE")) ?: "SBYE"
        photographerName = prefs.getString("photographer_name", "") ?: ""
        watermarkTextColor = prefs.getString("watermark_text_color", "#0000FF") ?: "#0000FF"
        watermarkDatePattern = prefs.getString("watermark_date_pattern", "dd.MM.yyyy, HH:mm") ?: "dd.MM.yyyy, HH:mm"
        watermarkBackgroundColor = prefs.getString("watermark_background_color", "#FFFFFF") ?: "#FFFFFF"
        watermarkBackgroundTransparent = prefs.getBoolean("watermark_background_transparent", false)
        watermarkBackgroundAlpha = prefs.getFloat("watermark_background_alpha", 0.45f)
    }
    var lastPhoto by remember { mutableStateOf<FieldPhoto?>(null) }
    var showLastPhoto by remember { mutableStateOf(false) }
    var cameraPhotos by remember { mutableStateOf<List<FieldPhoto>>(emptyList()) }
    var fullscreenPhotoIndex by remember { mutableIntStateOf(0) }
    val cameraViewerPhotos = (cameraPhotos.ifEmpty { listOfNotNull(lastPhoto) }).sortedBy { it.timestamp }
    var gpsLocation by remember { mutableStateOf(LocationStore.latest(context)) }
    var compassDegrees by remember { mutableFloatStateOf(Float.NaN) }
    var horizonRoll by remember { mutableFloatStateOf(0f) }
    var horizonPitch by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(context) {
        while (true) {
            gpsLocation = LocationStore.latest(context)
            delay(1_000L)
        }
    }

    DisposableEffect(context, targetRotation) {
        val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val sensor = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                val matrix = FloatArray(9)
                val adjusted = FloatArray(9)
                SensorManager.getRotationMatrixFromVector(matrix, event.values)
                val axes = when (targetRotation) {
                    Surface.ROTATION_90 -> SensorManager.AXIS_Y to SensorManager.AXIS_MINUS_X
                    Surface.ROTATION_180 -> SensorManager.AXIS_MINUS_X to SensorManager.AXIS_MINUS_Y
                    Surface.ROTATION_270 -> SensorManager.AXIS_MINUS_Y to SensorManager.AXIS_X
                    else -> SensorManager.AXIS_X to SensorManager.AXIS_Y
                }
                SensorManager.remapCoordinateSystem(matrix, axes.first, axes.second, adjusted)
                val orientation = FloatArray(3)
                SensorManager.getOrientation(adjusted, orientation)
                compassDegrees = (Math.toDegrees(orientation[0].toDouble()).toFloat() + 360f) % 360f
                horizonRoll = Math.toDegrees(orientation[1].toDouble()).toFloat()
                horizonPitch = Math.toDegrees(orientation[2].toDouble()).toFloat()
            }
            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
        }
        if (sensor != null) sensorManager.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_UI)
        onDispose { sensorManager.unregisterListener(listener) }
    }

    LaunchedEffect(context) {
        val existing = withContext(Dispatchers.IO) { PhotoFileManager.findPhotos(context) }
        cameraPhotos = existing
        if (lastPhoto == null) lastPhoto = existing.firstOrNull()
    }

    DisposableEffect(context) {
        val listener = object : OrientationEventListener(context) {
            override fun onOrientationChanged(orientation: Int) {
                if (orientation == ORIENTATION_UNKNOWN) return
                targetRotation = when (orientation) {
                    in 45..134 -> Surface.ROTATION_270
                    in 135..224 -> Surface.ROTATION_180
                    in 225..314 -> Surface.ROTATION_90
                    else -> Surface.ROTATION_0
                }
            }
        }
        listener.enable()
        onDispose { listener.disable() }
    }

    LaunchedEffect(targetRotation) {
        imageCapture?.targetRotation = targetRotation
    }

    LaunchedEffect(flashSetting, imageCapture) {
        imageCapture?.flashMode = flashSetting.toImageCaptureFlashMode()
    }

    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }

    LaunchedEffect(previewView) {
        if (previewUseCase == null) {
            previewUseCase = Preview.Builder()
                .build()
                .also { it.setSurfaceProvider(previewView.surfaceProvider) }
        }
    }
    LaunchedEffect(previewUseCase, lensFacing, prioritizeQuality, lifecycleOwner) {
        val preview = previewUseCase ?: return@LaunchedEffect
        val provider = cameraProviderFuture.get()
        cameraProviderRef = provider
        val oldCapture = imageCapture
        val previousLens = camera?.cameraInfo?.lensFacing ?: lensFacing
        try {
            if (isCameraBound) provider.unbind(*listOfNotNull(preview, oldCapture).toTypedArray())
            val capture = ImageCapture.Builder()
                .setTargetRotation(targetRotation)
                .setCaptureMode(if (prioritizeQuality) ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY else ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                .setFlashMode(flashSetting.toImageCaptureFlashMode())
                .build()
            val selector = CameraSelector.Builder().requireLensFacing(lensFacing).build()
            val boundCamera = provider.bindToLifecycle(lifecycleOwner, selector, preview, capture)
            isCameraBound = true
            camera = boundCamera
            imageCapture = capture
            zoomState = boundCamera.cameraInfo.zoomState.value
            zoomRatio = zoomState?.zoomRatio ?: 1f
            exposureIndex = boundCamera.cameraInfo.exposureState.exposureCompensationIndex
        } catch (e: Exception) {
            camera = null
            isCameraBound = false
            if (lensFacing != previousLens) {
                android.util.Log.e("FieldRecordCamera", "Camera switch failed; restoring previous lens", e)
                android.widget.Toast.makeText(context, "Kamera gagal beralih; mengembalikan kamera sebelumnya", android.widget.Toast.LENGTH_SHORT).show()
                lensFacing = previousLens
            } else {
                android.util.Log.e("FieldRecordCamera", "Camera bind failed", e)
                android.widget.Toast.makeText(context, "Kamera tidak dapat dibuka: ${e.message ?: "periksa izin/perangkat"}", android.widget.Toast.LENGTH_LONG).show()
            }
        }
    }

    DisposableEffect(lifecycleOwner) {
        onDispose {
            cameraProviderRef?.unbindAll()
            isCameraBound = false
        }
    }

    LaunchedEffect(focusPoint) {
        if (focusPoint != null) {
            delay(1_200L)
            focusPoint = null
        }
    }
    
    val startCapture: () -> Unit = {
        if (!isCapturing && countdown == 0) {
            captureScope.launch {
                for (remaining in timerSeconds downTo 1) {
                    countdown = remaining
                    delay(1_000L)
                }
                countdown = 0
                val location = LocationStore.latest(context)
                if (location == null || System.currentTimeMillis() - location.time > 120_000L) {
                    android.widget.Toast.makeText(context, "Menunggu sinyal lokasi GPS yang valid", android.widget.Toast.LENGTH_SHORT).show()
                } else {
                    val capture = imageCapture
                    if (capture == null) {
                        android.widget.Toast.makeText(context, "Kamera belum siap", android.widget.Toast.LENGTH_SHORT).show()
                    } else {
                        isCapturing = true
                        // Bearing: kompas (arah kamera) lebih akurat saat diam; GPS bearing hanya valid saat bergerak
                        val bearingToSave = when {
                            compassDegrees.isFinite() -> compassDegrees
                            location.hasBearing() -> location.bearing
                            else -> -1f
                        }
                        viewModel.capturePhoto(
                            context, capture, location.latitude, location.longitude,
                            bearingToSave, targetRotation, onSuccess = { photo ->
                                isCapturing = false
                                lastPhoto = photo
                                cameraPhotos = listOf(photo) + cameraPhotos.filterNot { it.filePath == photo.filePath }
                                onPhotoCaptured(photo)
                            },
                            onError = { message ->
                                isCapturing = false
                                android.widget.Toast.makeText(context, message, android.widget.Toast.LENGTH_LONG).show()
                            }
                        )
                    }
                }
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // Viewfinder centered
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(if (viewfinderLandscape) 4f / 3f else 3f / 4f)
                .clip(RoundedCornerShape(20.dp))
                .align(Alignment.Center)
                .offset(y = (-55).dp)
                .pointerInput(camera) {
                    detectTapGestures { tap ->
                        camera?.let { activeCamera ->
                            focusPoint = tap
                            val point = previewView.meteringPointFactory.createPoint(tap.x, tap.y)
                            val action = FocusMeteringAction.Builder(
                                point,
                                FocusMeteringAction.FLAG_AF or FocusMeteringAction.FLAG_AE or FocusMeteringAction.FLAG_AWB
                            ).build()
                            activeCamera.cameraControl.startFocusAndMetering(action)
                        }
                    }
                }
                .pointerInput(camera) {
                    awaitEachGesture {
                        awaitFirstDown(requireUnconsumed = false)
                        do {
                            val event = awaitPointerEvent()
                            val oldSpan = event.calculateCentroidSize(useCurrent = false)
                            val newSpan = event.calculateCentroidSize(useCurrent = true)
                            val state = zoomState
                            if (state != null && oldSpan > 0f && newSpan > 0f) {
                                val target = clampZoomRatio(zoomRatio * newSpan / oldSpan, state.minZoomRatio, state.maxZoomRatio)
                                if (target != zoomRatio) {
                                    zoomRatio = target
                                    camera?.cameraControl?.setZoomRatio(target)
                                }
                            }
                            event.changes.forEach { it.consume() }
                        } while (event.changes.any { it.pressed })
                    }
                }
        ) {
            AndroidView(
                factory = { previewView },
                modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(20.dp))
            )
            if (gridEnabled) {
                Canvas(Modifier.fillMaxSize()) {
                    val gridColor = Color.White.copy(alpha = 0.55f)
                    for (fraction in listOf(1f / 3f, 2f / 3f)) {
                        drawLine(gridColor, Offset(size.width * fraction, 0f), Offset(size.width * fraction, size.height), strokeWidth = 1.dp.toPx())
                        drawLine(gridColor, Offset(0f, size.height * fraction), Offset(size.width, size.height * fraction), strokeWidth = 1.dp.toPx())
                    }
                }
            }
            val focusColor = MaterialTheme.colors.primary
            focusPoint?.let { point ->
                Canvas(Modifier.fillMaxSize()) {
                    drawCircle(focusColor, radius = 27.dp.toPx(), center = point, style = Stroke(width = 2.dp.toPx()))
                    drawLine(focusColor, Offset(point.x - 7.dp.toPx(), point.y), Offset(point.x + 7.dp.toPx(), point.y), strokeWidth = 2.dp.toPx())
                    drawLine(focusColor, Offset(point.x, point.y - 7.dp.toPx()), Offset(point.x, point.y + 7.dp.toPx()), strokeWidth = 2.dp.toPx())
                }
            }
            CameraTelemetryOverlay(gpsLocation, compassDegrees, Modifier.fillMaxSize())
            // Horizon level overlay
            if (kotlin.math.abs(horizonRoll) < 5f && kotlin.math.abs(horizonPitch) < 5f) {
                Canvas(Modifier.fillMaxSize().padding(16.dp)) {
                    val width = size.width
                    val midY = size.height / 2
                    drawLine(Color.Green, androidx.compose.ui.geometry.Offset(0f, midY), androidx.compose.ui.geometry.Offset(width, midY), strokeWidth = 2.dp.toPx())
                }
            }
            zoomState?.let {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(end = 12.dp, bottom = 12.dp)
                        .size(42.dp)
                        .background(MaterialTheme.colors.primary.copy(alpha = 0.9f), CircleShape)
                        .border(1.5.dp, Color.White, CircleShape)
                        .clickable {
                        val view = (context as? android.app.Activity)?.window?.decorView
                        view?.performHapticFeedback(android.view.HapticFeedbackConstants.LONG_PRESS)
                        if (availableZoomPresets.isNotEmpty()) {
                                val currentIndex = availableZoomPresets.indexOfFirst { kotlin.math.abs(it - zoomRatio) < 0.1f }
                                val nextIndex = if (currentIndex >= 0) (currentIndex + 1) % availableZoomPresets.size else 0
                                val target = availableZoomPresets[nextIndex]
                                zoomRatio = target
                                camera?.cameraControl?.setZoomRatio(target)
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "${String.format(java.util.Locale.US, "%.1f", zoomRatio)}×",
                        color = Color.White,
                        style = MaterialTheme.typography.caption
                    )
                }
            }
            if (countdown > 0) {
                Text(
                    countdown.toString(),
                    color = Color.White,
                    style = MaterialTheme.typography.h2,
                    modifier = Modifier.align(Alignment.Center).background(MaterialTheme.colors.primary.copy(alpha = 0.85f), CircleShape).padding(24.dp)
                )
            }
        }

        Column(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Column(Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(0.dp)
                ) {
                    IconButton(onClick = {
                        val view = (context as? android.app.Activity)?.window?.decorView
                        view?.performHapticFeedback(android.view.HapticFeedbackConstants.CONTEXT_CLICK)
                        flashSetting = nextFlashSetting(flashSetting)
                        imageCapture?.flashMode = flashSetting.toImageCaptureFlashMode()
                    }) {
                        Icon(
                            imageVector = when (flashSetting) {
                                FlashSetting.OFF -> Icons.Default.FlashOff
                                FlashSetting.AUTO -> Icons.Default.FlashAuto
                                FlashSetting.ON -> Icons.Default.FlashOn
                            },
                            contentDescription = "Flash ${flashSetting.name}",
                            tint = LocalAppIconColor.current
                        )
                    }
                    IconButton(onClick = {
                        timerSeconds = when (timerSeconds) { 0 -> 3; 3 -> 10; else -> 0 }
                        preferences.edit().putInt("timer_seconds", timerSeconds).apply()
                    }) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.Timer, contentDescription = "Timer ${timerSeconds}s", tint = if (timerSeconds > 0) MaterialTheme.colors.primary else LocalAppIconColor.current)
                            if (timerSeconds > 0) Text("${timerSeconds}", color = MaterialTheme.colors.onSurface, style = MaterialTheme.typography.overline)
                        }
                    }
                    IconButton(onClick = { gridEnabled = !gridEnabled; preferences.edit().putBoolean("grid", gridEnabled).apply() }) {
                        Icon(Icons.Default.Grid3x3, contentDescription = if (gridEnabled) "Grid aktif" else "Grid nonaktif", tint = if (gridEnabled) MaterialTheme.colors.primary else LocalAppIconColor.current)
                    }
                    IconButton(onClick = { prioritizeQuality = !prioritizeQuality; preferences.edit().putBoolean("quality", prioritizeQuality).apply() }) {
                        Icon(if (prioritizeQuality) Icons.Default.HighQuality else Icons.Default.Speed, contentDescription = if (prioritizeQuality) "Kualitas tinggi" else "Mode cepat", tint = if (prioritizeQuality) MaterialTheme.colors.primary else LocalAppIconColor.current)
                    }
                    IconButton(onClick = { showExposureControl = !showExposureControl }) {
                        Icon(Icons.Default.Exposure, contentDescription = "Exposure", tint = if (showExposureControl) MaterialTheme.colors.primary else LocalAppIconColor.current)
                    }
                    IconButton(onClick = { showUnitSettings = true }) {
                        Icon(Icons.Default.Settings, "Pengaturan kamera", tint = LocalAppIconColor.current)
                    }
                }
                if (showExposureControl) {
                    camera?.cameraInfo?.exposureState?.let { exposure ->
                        val minEv = exposure.exposureCompensationRange.lower
                        val maxEv = exposure.exposureCompensationRange.upper
                        if (minEv < maxEv) {
                            Text("EV ${if (exposureIndex > 0) "+" else ""}$exposureIndex", color = MaterialTheme.colors.onSurface)
                            Slider(
                                value = exposureIndex.toFloat().coerceIn(minEv.toFloat(), maxEv.toFloat()),
                                onValueChange = { exposureIndex = it.toInt() },
                                onValueChangeFinished = { camera?.cameraControl?.setExposureCompensationIndex(exposureIndex) },
                                valueRange = minEv.toFloat()..maxEv.toFloat(),
                                modifier = Modifier.width(180.dp).height(32.dp),
                                colors = SliderDefaults.colors(thumbColor = MaterialTheme.colors.primary, activeTrackColor = MaterialTheme.colors.primary)
                            )
                        } else Text("EV tidak didukung", color = Color.White)
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                lastPhoto?.let { photo ->
                    Image(
                        painter = rememberAsyncImagePainter(File(photo.filePath)),
                        contentDescription = "Lihat foto terakhir",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.size(56.dp).clip(CircleShape)
                            .clickable {
                                fullscreenPhotoIndex = cameraViewerPhotos.lastIndex.coerceAtLeast(0)
                                showLastPhoto = true
                            }
                    )
                } ?: IconButton(onClick = {}) {
                    Icon(Icons.Default.PhotoLibrary, null, tint = LocalAppIconColor.current)
                }
                
                Button(
                    onClick = {
                        if (!isVideoMode) {
                            startCapture()
                        } else {
                            android.widget.Toast.makeText(context, "Mode video segera aktif", android.widget.Toast.LENGTH_SHORT).show()
                        }
                    },
                    enabled = !isCapturing && countdown == 0 && imageCapture != null,
                    modifier = Modifier.size(80.dp).border(4.dp, MaterialTheme.colors.primary, CircleShape),
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(backgroundColor = if (isVideoMode) Color.Red else MaterialTheme.colors.primary)
                ) { }
                IconButton(
                    onClick = {
                        val nextLens = if (lensFacing == CameraSelector.LENS_FACING_BACK) CameraSelector.LENS_FACING_FRONT else CameraSelector.LENS_FACING_BACK
                        val provider = cameraProviderRef
                        val selector = CameraSelector.Builder().requireLensFacing(nextLens).build()
                        if (provider != null && provider.hasCamera(selector)) lensFacing = nextLens
                        else android.widget.Toast.makeText(context, "Kamera depan/belakang tidak tersedia", android.widget.Toast.LENGTH_SHORT).show()
                    }
                ) { Icon(Icons.Default.Cameraswitch, "Ganti kamera", tint = LocalAppIconColor.current) }
            }
        }
        if (showLastPhoto) {
            PhotoFullscreenViewer(
                photos = cameraViewerPhotos,
                initialIndex = fullscreenPhotoIndex,
                knownTags = cameraPhotos.flatMap { com.fieldrecord.domain.model.extractPhotoTags(it.note) }.toSet(),
                onSaveNote = { photoId, note ->
                    viewModel.updatePhotoNote(photoId, note)
                    cameraPhotos = cameraPhotos.map { if (it.id == photoId) it.copy(note = note) else it }
                    lastPhoto = lastPhoto?.let { if (it.id == photoId) it.copy(note = note) else it }
                },
                onDismiss = { showLastPhoto = false }
            )
        }

        if (showUnitSettings) {
            var inputLine1 by remember { mutableStateOf(unitId) }
            var inputPhotographerName by remember { mutableStateOf(photographerName) }
            var inputTextColor by remember { mutableStateOf(watermarkTextColor) }
            var inputDatePattern by remember { mutableStateOf(watermarkDatePattern) }
            var inputBackgroundColor by remember { mutableStateOf(watermarkBackgroundColor) }
            var inputBackgroundTransparent by remember { mutableStateOf(watermarkBackgroundTransparent) }
            var inputBackgroundAlpha by remember { mutableFloatStateOf(watermarkBackgroundAlpha) }
            var dateMenuExpanded by remember { mutableStateOf(false) }
            val datePatterns = listOf("dd.MM.yyyy, HH:mm", "dd/MM/yyyy HH:mm", "yyyy-MM-dd HH:mm", "dd.MM.yyyy", "HH:mm")
            Surface(Modifier.fillMaxSize().zIndex(10f), color = MaterialTheme.colors.surface) {
                Column(Modifier.fillMaxSize().padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = { showUnitSettings = false }) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Kembali", tint = LocalAppIconColor.current)
                        }
                        Text("Pengaturan Watermark", style = MaterialTheme.typography.h6, color = MaterialTheme.colors.onSurface)
                    }
                    Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(vertical = 8.dp)) {
                        Text("Text baris 1", style = MaterialTheme.typography.subtitle2, color = MaterialTheme.colors.onSurface)
                        OutlinedTextField(value = inputLine1, onValueChange = { inputLine1 = it }, singleLine = true, modifier = Modifier.fillMaxWidth(), placeholder = { Text("Contoh: SBYE") })
                        Spacer(Modifier.height(16.dp))
                        Text("Nama pengguna / pengambil foto", style = MaterialTheme.typography.subtitle2, color = MaterialTheme.colors.onSurface)
                        OutlinedTextField(
                            value = inputPhotographerName,
                            onValueChange = { inputPhotographerName = it },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = { Text("Contoh: Novianto Syahmud") }
                        )
                        Text(
                            "Nama ini menjadi stamp pada nama file dan data berbagi.",
                            style = MaterialTheme.typography.caption,
                            color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f)
                        )
                        Spacer(Modifier.height(12.dp))
                        Text("Warna font", style = MaterialTheme.typography.subtitle2, color = MaterialTheme.colors.onSurface)
                        OutlinedTextField(value = inputTextColor, onValueChange = { inputTextColor = it }, singleLine = true, modifier = Modifier.fillMaxWidth(), label = { Text("Hex, contoh #0000FF") })
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 8.dp)) {
                            listOf("#0000FF", "#000000", "#FFFFFF", "#2E7D32", "#FFB300", "#FF0000").forEach { hex ->
                                Box(
                                    Modifier.size(30.dp).clip(CircleShape).background(Color(android.graphics.Color.parseColor(hex)))
                                        .border(if (inputTextColor.equals(hex, true)) 3.dp else 1.dp, Color.White, CircleShape)
                                        .clickable { inputTextColor = hex }
                                )
                            }
                        }
                        Spacer(Modifier.height(16.dp))
                        Text("Format tanggal / waktu — baris 2", style = MaterialTheme.typography.subtitle2, color = MaterialTheme.colors.onSurface)
                        Box {
                            OutlinedButton(onClick = { dateMenuExpanded = true }) { Text(inputDatePattern) }
                            DropdownMenu(expanded = dateMenuExpanded, onDismissRequest = { dateMenuExpanded = false }) {
                                datePatterns.forEach { pattern -> DropdownMenuItem(onClick = { inputDatePattern = pattern; dateMenuExpanded = false }) { Text(pattern) } }
                            }
                        }
                        Spacer(Modifier.height(16.dp))
                        Text("Latar watermark", style = MaterialTheme.typography.subtitle2, color = MaterialTheme.colors.onSurface)
                        OutlinedTextField(value = inputBackgroundColor, onValueChange = { inputBackgroundColor = it }, singleLine = true, modifier = Modifier.fillMaxWidth(), label = { Text("Warna hex, contoh #FFFFFF") })
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(checked = inputBackgroundTransparent, onCheckedChange = { inputBackgroundTransparent = it })
                            Text("Gunakan latar transparan", color = MaterialTheme.colors.onSurface)
                        }
                        if (inputBackgroundTransparent) {
                            Text("Opasitas ${"%.0f".format(inputBackgroundAlpha * 100)}%", style = MaterialTheme.typography.caption, color = MaterialTheme.colors.onSurface)
                            Slider(value = inputBackgroundAlpha, onValueChange = { inputBackgroundAlpha = it }, valueRange = 0.1f..1f)
                        }
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        TextButton(onClick = { showUnitSettings = false }) { Text("Batal") }
                        Spacer(Modifier.width(8.dp))
                        Button(
                            onClick = {
                                val validTextColor = runCatching { android.graphics.Color.parseColor(inputTextColor); inputTextColor }.getOrDefault(watermarkTextColor)
                                val validBackgroundColor = runCatching { android.graphics.Color.parseColor(inputBackgroundColor); inputBackgroundColor }.getOrDefault(watermarkBackgroundColor)
                                unitId = inputLine1.ifBlank { "SBYE" }
                                photographerName = inputPhotographerName.trim()
                                watermarkTextColor = validTextColor
                                watermarkDatePattern = inputDatePattern
                                watermarkBackgroundColor = validBackgroundColor
                                watermarkBackgroundTransparent = inputBackgroundTransparent
                                watermarkBackgroundAlpha = inputBackgroundAlpha
                                context.getSharedPreferences("field_record_prefs", Context.MODE_PRIVATE).edit()
                                    .putString("unit_id", unitId)
                                    .putString("watermark_line1", unitId)
                                    .putString("photographer_name", photographerName)
                                    .putString("watermark_text_color", watermarkTextColor)
                                    .putString("watermark_date_pattern", watermarkDatePattern)
                                    .putString("watermark_background_color", watermarkBackgroundColor)
                                    .putBoolean("watermark_background_transparent", watermarkBackgroundTransparent)
                                    .putFloat("watermark_background_alpha", watermarkBackgroundAlpha)
                                    .apply()
                                showUnitSettings = false
                            },
                            colors = ButtonDefaults.buttonColors(backgroundColor = MaterialTheme.colors.primary)
                        ) { Text("Simpan", color = MaterialTheme.colors.onPrimary) }
                    }
                }
            }
        }
    }
}

@Composable
private fun CameraTelemetryOverlay(
    location: android.location.Location?,
    compassDegrees: Float,
    modifier: Modifier = Modifier
) {
    val freshLocation = location?.takeIf { System.currentTimeMillis() - it.time <= 120_000L }
    val locationLabel = freshLocation?.let {
        "GPS  ${String.format(java.util.Locale.US, "%.6f", it.latitude)}, ${String.format(java.util.Locale.US, "%.6f", it.longitude)}  ±${if (it.hasAccuracy()) "${it.accuracy.toInt()}m" else "?"}"
    } ?: "GPS  Menunggu lokasi..."
    val cardinal = if (compassDegrees.isFinite()) {
        listOf("N", "NE", "E", "SE", "S", "SW", "W", "NW")[((compassDegrees + 22.5f) / 45f).toInt() % 8]
    } else "—"
    val compassLabel = "KOMPAS  $cardinal  ${if (compassDegrees.isFinite()) "${compassDegrees.toInt()}°" else "—°"}"
    Box(modifier = modifier) {
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 12.dp, bottom = 12.dp)
                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colors.surface.copy(alpha = 0.85f))
                .padding(horizontal = 10.dp, vertical = 7.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = locationLabel,
                color = MaterialTheme.colors.onSurface,
                style = MaterialTheme.typography.caption
            )
            Text(
                text = compassLabel,
                color = MaterialTheme.colors.onSurface,
                style = MaterialTheme.typography.caption
            )
        }
    }
}
