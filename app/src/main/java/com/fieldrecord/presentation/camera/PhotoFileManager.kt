package com.fieldrecord.presentation.camera

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color as AndroidColor
import android.graphics.Paint
import android.graphics.Typeface
import android.widget.Toast
import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.media.ExifInterface
import androidx.camera.core.ImageProxy
import com.fieldrecord.domain.model.FieldPhoto
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object PhotoFileManager {
    private const val ROOT_FOLDER = "FieldRecord"
    private const val PHOTO_FOLDER = "Photo"
    private const val VIDEO_FOLDER = "Video"
    private const val EXTENSION = ".jpg"
    private const val VIDEO_EXTENSION = ".mp4"
    private val filenamePattern = Regex("^IMG_(\\d{6})_(\\d{3,})(?:_([\\p{L}\\p{N}_-]+))?\\.jpg$", RegexOption.IGNORE_CASE)
    private val videoFilenamePattern = Regex("^VID_(\\d{6})_(\\d{3,})\\.mp4$", RegexOption.IGNORE_CASE)

    /** USB-visible shared internal storage: /storage/emulated/0/FieldRecord/Photo. */
    fun getStorageDir(): File =
        File(Environment.getExternalStorageDirectory(), "$ROOT_FOLDER/$PHOTO_FOLDER")

    /** USB-visible shared internal storage: /storage/emulated/0/FieldRecord/Video. */
    fun getVideoStorageDir(): File =
        File(Environment.getExternalStorageDirectory(), "$ROOT_FOLDER/$VIDEO_FOLDER")

    /** Create a collision-safe monthly sequence in the form VID_YYMMdd_xxx.mp4. */
    @Synchronized
    fun getOutputVideoFile(): File {
        val dir = getVideoStorageDir().apply { check(isDirectory || mkdirs()) }
        val dateStamp = SimpleDateFormat("yyMMdd", Locale.US).format(Date())
        val monthlyFilenames = (dir.listFiles() ?: emptyArray()).map { it.name }
        val next = nextMonthlyVideoSequence(dateStamp, monthlyFilenames)
        return File(dir, formatVideoFilename(dateStamp, next))
    }

    internal fun nextMonthlyVideoSequence(dateStamp: String, filenames: Iterable<String>): Int {
        require(dateStamp.matches(Regex("\\d{6}")))
        val monthPrefix = dateStamp.take(4)
        val monthlySequences = filenames.mapNotNull { name ->
            videoFilenamePattern.matchEntire(name)?.let { match ->
                if (match.groupValues[1].startsWith(monthPrefix)) {
                    match.groupValues[2].toIntOrNull()
                } else null
            }
        }
        return maxOf(monthlySequences.size, monthlySequences.maxOrNull() ?: 0) + 1
    }

    internal fun formatVideoFilename(dateStamp: String, sequence: Int): String {
        require(dateStamp.matches(Regex("\\d{6}")))
        require(sequence > 0)
        return "VID_${dateStamp}_${String.format(Locale.US, "%03d", sequence)}$VIDEO_EXTENSION"
    }

    /** Create a collision-safe monthly sequence in the form IMG_YYMMdd_xxx.jpg. */
    @Synchronized
    fun getOutputFile(userName: String? = null): File {
        val dir = getStorageDir().apply { check(isDirectory || mkdirs()) }
        val dateStamp = SimpleDateFormat("yyMMdd", Locale.US).format(Date())
        val monthlyFilenames = (dir.listFiles() ?: emptyArray()).map { it.name }
        val next = nextMonthlySequence(dateStamp, monthlyFilenames)
        return File(dir, formatFilename(dateStamp, next, userName))
    }

    /** Create a collision-safe monthly sequence based on a timestamp for .field import. */
    @Synchronized
    fun getOutputFileForTimestamp(
        timestamp: Long,
        existingFilenames: MutableList<String>,
        userName: String? = null,
        preferredFileName: String? = null
    ): File {
        val dir = getStorageDir().apply { check(isDirectory || mkdirs()) }
        val dateStamp = SimpleDateFormat("yyMMdd", Locale.US).format(Date(timestamp))
        val allFilenames = ((dir.listFiles() ?: emptyArray()).map { it.name } + existingFilenames).toMutableSet()
        val safePreferredName = preferredFileName?.takeIf { filenamePattern.matches(it) }
        val filename = if (safePreferredName != null) {
            val preferred = requireNotNull(safePreferredName)
            var candidate = preferred
            var collisionIndex = 1
            while (candidate in allFilenames) {
                candidate = "${preferred.substringBeforeLast('.')}_$collisionIndex$EXTENSION"
                collisionIndex++
            }
            candidate
        } else {
            val next = nextMonthlySequence(dateStamp, allFilenames)
            formatFilename(dateStamp, next, userName)
        }
        existingFilenames.add(filename)
        return File(dir, filename)
    }

    internal fun nextMonthlySequence(dateStamp: String, filenames: Iterable<String>): Int {
        require(dateStamp.matches(Regex("\\d{6}")))
        val monthPrefix = dateStamp.take(4)
        val monthlySequences = filenames.mapNotNull { name ->
            filenamePattern.matchEntire(name)?.let { match ->
                if (match.groupValues[1].startsWith(monthPrefix)) {
                    match.groupValues[2].toIntOrNull()
                } else null
            }
        }
        return maxOf(monthlySequences.size, monthlySequences.maxOrNull() ?: 0) + 1
    }

    /** Create a collision-safe monthly photo name, optionally stamped with the configured user. */
    internal fun formatFilename(dateStamp: String, sequence: Int, userName: String? = null): String {
        require(dateStamp.matches(Regex("\\d{6}")))
        require(sequence > 0)
        val safeUserName = sanitizeUserNameForFilename(userName)
        val userStamp = if (safeUserName.isBlank()) "" else "_$safeUserName"
        return "IMG_${dateStamp}_${String.format(Locale.US, "%03d", sequence)}$userStamp$EXTENSION"
    }

    internal fun sanitizeUserNameForFilename(userName: String?): String = userName.orEmpty()
        .trim()
        .replace(Regex("[^\\p{L}\\p{N}_-]+"), "_")
        .trim('_', '-')
        .take(40)

    fun photographerStampFromFilename(fileName: String): String? =
        filenamePattern.matchEntire(fileName)?.groupValues?.getOrNull(3)?.takeIf { it.isNotBlank() }?.replace('_', ' ')

    /** Scan active and known legacy folders, newest first, for camera preview initialization. */
    fun findPhotos(context: Context): List<FieldPhoto> {
        val candidates = listOf(
            getStorageDir(),
            File(context.filesDir, "Field Records"),
            File(context.filesDir, "FieldRecord/Photo")
        ).distinct().flatMap { folder ->
            folder.listFiles()?.filter { it.isFile && it.extension.equals("jpg", true) }.orEmpty()
        }.distinctBy { it.absolutePath }
        return candidates.sortedByDescending { it.lastModified() }.mapNotNull(::readPhotoMetadata)
    }

    fun findLatestPhoto(context: Context): FieldPhoto? = findPhotos(context).firstOrNull()

    fun findVideos(): List<File> = getVideoStorageDir().listFiles()
        ?.filter { it.isFile && videoFilenamePattern.matches(it.name) }
        ?.sortedByDescending { it.lastModified() }
        .orEmpty()

    fun findLatestVideo(): File? = findVideos().firstOrNull()

    private fun readPhotoMetadata(file: File): FieldPhoto? {
        var latitude = 0.0
        var longitude = 0.0
        var bearing = 0f
        runCatching {
            val exif = ExifInterface(file.absolutePath)
            val coords = FloatArray(2)
            if (exif.getLatLong(coords)) {
                latitude = coords[0].toDouble()
                longitude = coords[1].toDouble()
            }
            bearing = exif.getAttribute(ExifInterface.TAG_GPS_IMG_DIRECTION)?.toFloatOrNull() ?: -1f
        }
        return FieldPhoto(
            id = 0,
            filePath = file.absolutePath,
            fileName = file.name,
            latitude = latitude,
            longitude = longitude,
            bearing = bearing,
            timestamp = file.lastModified()
        )
    }

    fun cropToAspectRatio(bitmap: Bitmap, isLandscape: Boolean): Bitmap {
        val targetRatio = if (isLandscape) 4f / 3f else 3f / 4f
        val currentRatio = bitmap.width.toFloat() / bitmap.height.toFloat()
        var newWidth = bitmap.width
        var newHeight = bitmap.height
        var x = 0
        var y = 0
        if (currentRatio > targetRatio) {
            newWidth = (bitmap.height * targetRatio).toInt()
            x = (bitmap.width - newWidth) / 2
        } else if (currentRatio < targetRatio) {
            newHeight = (bitmap.width / targetRatio).toInt()
            y = (bitmap.height - newHeight) / 2
        }
        return Bitmap.createBitmap(bitmap, x, y, newWidth, newHeight)
    }

    /** Save GPS metadata and apply watermark with white bar and blue text. */
    fun saveLocationWithExifAndWatermark(
        context: Context,
        file: File,
        latitude: Double,
        longitude: Double,
        bearing: Float,
        targetRotation: Int = 0
    ): Boolean {
        return try {
            val raw = BitmapFactory.decodeFile(file.absolutePath) ?: return false
            
            // Force exact orientation matching the physical sensor/UI targetRotation
            // targetRotation: 0 = ROTATION_0 (Portrait), 1 = ROTATION_90 (Landscape), 2 = ROTATION_180 (Rev Portrait), 3 = ROTATION_270 (Rev Landscape)
            val isTargetPortrait = (targetRotation == 0 || targetRotation == 2)
            val isRawLandscape = raw.width > raw.height
            
            val rotationDegrees = when {
                isTargetPortrait && isRawLandscape -> 90f
                !isTargetPortrait && !isRawLandscape -> 90f
                else -> 0f
            }
            
            android.util.Log.d("PhotoFileManager", "targetRotation: $targetRotation, isTargetPortrait: $isTargetPortrait, isRawLandscape: $isRawLandscape, rotationDegrees: $rotationDegrees")

            val rotationMatrix = android.graphics.Matrix().apply {
                if (rotationDegrees != 0f) postRotate(rotationDegrees)
            }
            
            val original = Bitmap.createBitmap(raw, 0, 0, raw.width, raw.height, rotationMatrix, true)
            if (original != raw) raw.recycle()

            val prefs = context.getSharedPreferences("field_record_prefs", Context.MODE_PRIVATE)
            val unitId = prefs.getString("watermark_line1", prefs.getString("unit_id", "SBYE")) ?: "SBYE"
            val textColor = runCatching {
                AndroidColor.parseColor(prefs.getString("watermark_text_color", "#0000FF"))
            }.getOrDefault(AndroidColor.BLUE)
            val backgroundColor = runCatching {
                AndroidColor.parseColor(prefs.getString("watermark_background_color", "#FFFFFF"))
            }.getOrDefault(AndroidColor.WHITE)
            val transparentBackground = prefs.getBoolean("watermark_background_transparent", false)
            val backgroundAlpha = (prefs.getFloat("watermark_background_alpha", 0.45f).coerceIn(0f, 1f) * 255).toInt()
            val datePattern = prefs.getString("watermark_date_pattern", "dd.MM.yyyy, HH:mm") ?: "dd.MM.yyyy, HH:mm"
            
            val dateStamp = runCatching { SimpleDateFormat(datePattern, Locale.US).format(Date()) }
                .getOrElse { SimpleDateFormat("dd.MM.yyyy, HH:mm", Locale.US).format(Date()) }
            val hasBearing = bearing.isFinite() && bearing >= 0f && bearing <= 360f
            val cardinal = if (hasBearing) {
                listOf("N", "NE", "E", "SE", "S", "SW", "W", "NW")[((bearing + 22.5f) / 45f).toInt() % 8]
            } else null
            // Use the landscape image's height (the shorter edge) as the shared watermark-bar basis.
            // This keeps the same bar thickness for portrait and landscape captures.
            val barHeight = watermarkBarHeightPx(original.width, original.height).toInt().coerceAtLeast(1)
            // Keep the original photo dimensions; overlay watermark bar inside the bottom of the image.
            val watermarked = Bitmap.createBitmap(original.width, original.height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(watermarked)
            canvas.drawBitmap(original, 0f, 0f, null)
            
            val barPaint = Paint().apply {
                color = backgroundColor
                alpha = if (transparentBackground) backgroundAlpha else 255
            }
            val barTop = (original.height - barHeight).toFloat()
            canvas.drawRect(0f, barTop, original.width.toFloat(), original.height.toFloat(), barPaint)
            
            val textPaint = Paint().apply {
                color = textColor
                textSize = barHeight * 0.25f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                isAntiAlias = true
            }
            
            val line1 = unitId
            val line2 = dateStamp
            val line3 = if (hasBearing) {
                val bearingText = String.format(Locale.US, "%d°", bearing.toInt())
                "${String.format(Locale.US, "%.5f", latitude)}, ${String.format(Locale.US, "%.5f", longitude)} (${cardinal} - ${bearingText})"
            } else {
                "${String.format(Locale.US, "%.5f", latitude)}, ${String.format(Locale.US, "%.5f", longitude)}"
            }
            
            val xMargin = original.width * 0.03f
            
            // Draw the three watermark lines inside the bottom overlay bar.
            val startY = barTop + textPaint.textSize * 1.05f
            val lineSpacing = textPaint.textSize * 1.1f
            
            canvas.drawText(line1, xMargin, startY, textPaint)
            canvas.drawText(line2, xMargin, startY + lineSpacing, textPaint)
            canvas.drawText(line3, xMargin, startY + lineSpacing * 2, textPaint)
            
            FileOutputStream(file).use { out ->
                watermarked.compress(Bitmap.CompressFormat.JPEG, 95, out)
            }
            
            val exif = ExifInterface(file.absolutePath)
            if (latitude in -90.0..90.0 && longitude in -180.0..180.0 && !(latitude == 0.0 && longitude == 0.0)) {
                exif.setAttribute(ExifInterface.TAG_GPS_LATITUDE, convertToDMS(latitude))
                exif.setAttribute(ExifInterface.TAG_GPS_LATITUDE_REF, if (latitude >= 0) "N" else "S")
                exif.setAttribute(ExifInterface.TAG_GPS_LONGITUDE, convertToDMS(longitude))
                exif.setAttribute(ExifInterface.TAG_GPS_LONGITUDE_REF, if (longitude >= 0) "E" else "W")
            }
            if (hasBearing) {
                exif.setAttribute(ExifInterface.TAG_GPS_IMG_DIRECTION, String.format(Locale.US, "%.2f", bearing))
                exif.setAttribute(ExifInterface.TAG_GPS_IMG_DIRECTION_REF, "T")
            }
            exif.setAttribute(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL.toString())
            exif.saveAttributes()
            
            original.recycle()
            watermarked.recycle()
            true
        } catch (_: Exception) { false }
    }

    private fun convertToDMS(coord: Double): String {
        val absCoord = kotlin.math.abs(coord)
        val degrees = absCoord.toInt()
        val minutes = ((absCoord - degrees) * 60).toInt()
        val seconds = (absCoord - degrees - minutes / 60.0) * 3600
        return String.format(Locale.US, "%d/1,%d/1,%.0f/100", degrees, minutes, seconds * 100)
    }

    fun imageProxyToBitmap(image: ImageProxy): Bitmap? = null

    fun shareToWhatsApp(context: Context, imageUri: Uri, lat: Double, lon: Double, note: String) {
        val mapsUrl = "https://maps.google.com/?q=$lat,$lon"
        val text = "📍 *FieldRecord Share*\n\n📝 Catatan: $note\n🌐 Lokasi: $mapsUrl"
        
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "image/*"
            putExtra(Intent.EXTRA_STREAM, imageUri)
            putExtra(Intent.EXTRA_TEXT, text)
            setPackage("com.whatsapp")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        try {
            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "WhatsApp tidak terinstal", Toast.LENGTH_SHORT).show()
        }
    }
}
