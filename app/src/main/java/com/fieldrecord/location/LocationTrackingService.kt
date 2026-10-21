package com.fieldrecord.location

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.fieldrecord.MainActivity
import com.fieldrecord.R
import com.fieldrecord.data.local.dao.TrackDao
import com.fieldrecord.data.local.entity.TrackPointEntity
import com.fieldrecord.data.local.entity.TrackSessionEntity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject

object LocationStore {
    private const val PREFS = "live_location"
    private const val LAT = "latitude"
    private const val LON = "longitude"
    private const val TIME = "timestamp"
    private const val ACCURACY = "accuracy"

    fun save(context: Context, location: Location) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putString(LAT, location.latitude.toString())
            .putString(LON, location.longitude.toString())
            .putLong(TIME, location.time)
            .putFloat(ACCURACY, if (location.hasAccuracy()) location.accuracy else -1f)
            .putFloat("bearing", if (location.hasBearing()) location.bearing else -1f)
            .putFloat("speed", if (location.hasSpeed()) location.speed else -1f)
            .apply()
    }

    fun latest(context: Context): Location? {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val lat = prefs.getString(LAT, null)?.toDoubleOrNull() ?: return null
        val lon = prefs.getString(LON, null)?.toDoubleOrNull() ?: return null
        if (lat !in -90.0..90.0 || lon !in -180.0..180.0 || (lat == 0.0 && lon == 0.0)) return null
        return Location("fieldrecord").apply {
            latitude = lat
            longitude = lon
            time = prefs.getLong(TIME, 0L)
            if (prefs.getFloat(ACCURACY, -1f) >= 0f) accuracy = prefs.getFloat(ACCURACY, -1f)
            val bearingVal = prefs.getFloat("bearing", -1f)
            if (bearingVal >= 0f) bearing = bearingVal
            val speedVal = prefs.getFloat("speed", -1f)
            if (speedVal >= 0f) speed = speedVal
        }
    }
}

@AndroidEntryPoint
class LocationTrackingService : Service(), LocationListener {
    @Inject lateinit var trackDao: TrackDao

    private var locationManager: LocationManager? = null
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val trackMutex = Mutex()
    @Volatile private var activeSessionId: Long? = null
    @Volatile private var isTrackPaused = false
    @Volatile private var activeSegmentIndex = 0
    @Volatile private var lastTrackFixSubmittedAt = 0L

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startAsLocationForegroundService(notification())
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            stopSelf()
            return
        }
        locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        try {
            locationManager?.getLastKnownLocation(LocationManager.GPS_PROVIDER)?.let { cached ->
                if (isUsableGpsFix(cached)) LocationStore.save(this, cached)
            }
            locationManager?.requestLocationUpdates(LocationManager.GPS_PROVIDER, 500L, 0f, this)
        } catch (_: SecurityException) {
            stopSelf()
        } catch (_: IllegalArgumentException) {
            // A device may not provide one of the location providers.
        }
        serviceScope.launch { restoreActiveTrack() }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START_TRACKING -> serviceScope.launch { startTrack() }
            ACTION_PAUSE_TRACKING -> serviceScope.launch { pauseTrack() }
            ACTION_RESUME_TRACKING -> serviceScope.launch { resumeTrack() }
            ACTION_STOP_TRACKING -> serviceScope.launch { finishTrack() }
        }
        return START_STICKY
    }

    override fun onLocationChanged(location: Location) {
        if (location.provider != LocationManager.GPS_PROVIDER || !isUsableGpsFix(location)) return
        LocationStore.save(this, location)
        val sessionId = activeSessionId ?: return
        if (isTrackPaused) return
        if (location.time - lastTrackFixSubmittedAt < MIN_PROCESS_INTERVAL_MILLIS) return
        lastTrackFixSubmittedAt = location.time
        val fix = TrackFix(
            latitude = location.latitude,
            longitude = location.longitude,
            timestamp = location.time,
            accuracyMeters = location.accuracy,
            speedMetersPerSecond = if (location.hasSpeed() && location.speed.isFinite()) location.speed.coerceAtLeast(0f) else 0f
        )
        serviceScope.launch {
            trackMutex.withLock {
                if (activeSessionId != sessionId || isTrackPaused) return@withLock
                val segment = activeSegmentIndex
                val previous = trackDao.getLastPoint(sessionId, segment)
                if (!shouldRecordTrackFix(previous, fix)) return@withLock
                val point = TrackPointEntity(
                    sessionId = sessionId,
                    segmentIndex = segment,
                    latitude = fix.latitude,
                    longitude = fix.longitude,
                    timestamp = fix.timestamp,
                    accuracyMeters = fix.accuracyMeters,
                    speedMetersPerSecond = fix.speedMetersPerSecond
                )
                val segmentDistance = previous?.let {
                    trackDistanceMeters(it.latitude, it.longitude, point.latitude, point.longitude)
                } ?: 0.0
                trackDao.appendPoint(point, segmentDistance)
            }
        }
    }

    private suspend fun restoreActiveTrack() = trackMutex.withLock {
        val session = trackDao.getActiveSession()
        activeSessionId = session?.id
        isTrackPaused = session?.status == TrackSessionEntity.STATUS_PAUSED
        activeSegmentIndex = session?.segmentIndex ?: 0
        if (session != null) updateForegroundNotification()
    }

    private suspend fun startTrack() = trackMutex.withLock {
        val existing = trackDao.getActiveSession()
        if (existing != null) {
            activeSessionId = existing.id
            isTrackPaused = existing.status == TrackSessionEntity.STATUS_PAUSED
            activeSegmentIndex = existing.segmentIndex
        } else {
            val now = System.currentTimeMillis()
            activeSessionId = trackDao.insertSession(TrackSessionEntity(startedAt = now))
            activeSegmentIndex = 0
            isTrackPaused = false
        }
        lastTrackFixSubmittedAt = 0L
        updateForegroundNotification()
    }

    private suspend fun pauseTrack() = trackMutex.withLock {
        val sessionId = activeSessionId ?: trackDao.getActiveSession()?.id ?: return@withLock
        val session = trackDao.getSession(sessionId) ?: return@withLock
        if (session.status != TrackSessionEntity.STATUS_RECORDING) return@withLock
        trackDao.updateSession(
            session.copy(status = TrackSessionEntity.STATUS_PAUSED, pauseStartedAt = System.currentTimeMillis())
        )
        isTrackPaused = true
        updateForegroundNotification()
    }

    private suspend fun resumeTrack() = trackMutex.withLock {
        val sessionId = activeSessionId ?: trackDao.getActiveSession()?.id ?: return@withLock
        val session = trackDao.getSession(sessionId) ?: return@withLock
        if (session.status != TrackSessionEntity.STATUS_PAUSED) return@withLock
        val now = System.currentTimeMillis()
        val pausedAt = session.pauseStartedAt ?: now
        val updated = session.copy(
            status = TrackSessionEntity.STATUS_RECORDING,
            pausedDurationMillis = session.pausedDurationMillis + (now - pausedAt).coerceAtLeast(0L),
            pauseStartedAt = null,
            segmentIndex = session.segmentIndex + 1
        )
        trackDao.updateSession(updated)
        activeSegmentIndex = updated.segmentIndex
        activeSessionId = sessionId
        isTrackPaused = false
        lastTrackFixSubmittedAt = 0L
        updateForegroundNotification()
    }

    private suspend fun finishTrack() = trackMutex.withLock {
        val sessionId = activeSessionId ?: trackDao.getActiveSession()?.id ?: return@withLock
        val session = trackDao.getSession(sessionId) ?: return@withLock
        val now = System.currentTimeMillis()
        val pausedMillis = session.pausedDurationMillis +
            if (session.status == TrackSessionEntity.STATUS_PAUSED) (now - (session.pauseStartedAt ?: now)).coerceAtLeast(0L) else 0L
        trackDao.updateSession(
            session.copy(
                status = TrackSessionEntity.STATUS_COMPLETED,
                endedAt = now,
                pausedDurationMillis = pausedMillis,
                pauseStartedAt = null
            )
        )
        activeSessionId = null
        isTrackPaused = false
        activeSegmentIndex = 0
        updateForegroundNotification()
    }

    private fun isUsableGpsFix(location: Location): Boolean =
        location.latitude.isFinite() && location.longitude.isFinite() &&
            location.latitude in -90.0..90.0 && location.longitude in -180.0..180.0 &&
            !(location.latitude == 0.0 && location.longitude == 0.0) &&
            location.hasAccuracy() && location.accuracy <= MAX_GPS_ACCURACY_METERS

    private fun startAsLocationForegroundService(notification: Notification) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun updateForegroundNotification() {
        startAsLocationForegroundService(notification())
    }

    private fun notification(): Notification {
        val recording = activeSessionId != null && !isTrackPaused
        val paused = activeSessionId != null && isTrackPaused
        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(when {
                recording -> "Merekam track FieldRecord"
                paused -> "Track FieldRecord dijeda"
                else -> "FieldRecord aktif"
            })
            .setContentText(when {
                recording -> "GPS tetap merekam saat aplikasi berada di latar belakang"
                paused -> "Pelacakan dijeda · ketuk lanjutkan atau buka aplikasi"
                else -> "Memperbarui lokasi untuk peta dan foto"
            })
            .setSmallIcon(R.mipmap.ic_launcher)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setContentIntent(openAppPendingIntent())

        if (recording) {
            builder.addAction(
                android.R.drawable.ic_media_pause,
                "Jeda",
                servicePendingIntent(ACTION_PAUSE_TRACKING, REQUEST_PAUSE)
            )
        } else if (paused) {
            builder.addAction(
                android.R.drawable.ic_media_play,
                "Lanjutkan",
                servicePendingIntent(ACTION_RESUME_TRACKING, REQUEST_RESUME)
            )
        }
        if (recording || paused) {
            builder.addAction(
                android.R.drawable.ic_menu_close_clear_cancel,
                "Simpan & hentikan",
                servicePendingIntent(ACTION_STOP_TRACKING, REQUEST_STOP)
            )
        }
        return builder.build()
    }

    private fun openAppPendingIntent(): PendingIntent {
        val intent = Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
        return PendingIntent.getActivity(this, REQUEST_OPEN_APP, intent, pendingIntentFlags())
    }

    private fun servicePendingIntent(action: String, requestCode: Int): PendingIntent {
        val intent = Intent(this, LocationTrackingService::class.java).setAction(action)
        return PendingIntent.getService(this, requestCode, intent, pendingIntentFlags())
    }

    private fun pendingIntentFlags(): Int =
        PendingIntent.FLAG_UPDATE_CURRENT or if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(CHANNEL_ID, "Pelacakan lokasi", NotificationManager.IMPORTANCE_LOW)
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        locationManager?.removeUpdates(this)
        serviceScope.cancel()
        super.onDestroy()
    }

    companion object {
        private const val CHANNEL_ID = "location_tracking"
        private const val NOTIFICATION_ID = 2104
        private const val MAX_GPS_ACCURACY_METERS = 100f
        private const val MIN_PROCESS_INTERVAL_MILLIS = 1_000L
        private const val ACTION_START_TRACKING = "com.fieldrecord.action.START_TRACKING"
        private const val ACTION_PAUSE_TRACKING = "com.fieldrecord.action.PAUSE_TRACKING"
        private const val ACTION_RESUME_TRACKING = "com.fieldrecord.action.RESUME_TRACKING"
        private const val ACTION_STOP_TRACKING = "com.fieldrecord.action.STOP_TRACKING"
        private const val REQUEST_OPEN_APP = 2105
        private const val REQUEST_PAUSE = 2106
        private const val REQUEST_RESUME = 2107
        private const val REQUEST_STOP = 2108

        fun start(context: Context) {
            val intent = Intent(context, LocationTrackingService::class.java)
            ContextCompat.startForegroundService(context, intent)
        }

        fun startTrackRecording(context: Context) = sendAction(context, ACTION_START_TRACKING)
        fun pauseTrackRecording(context: Context) = sendAction(context, ACTION_PAUSE_TRACKING)
        fun resumeTrackRecording(context: Context) = sendAction(context, ACTION_RESUME_TRACKING)
        fun stopTrackRecording(context: Context) = sendAction(context, ACTION_STOP_TRACKING)

        private fun sendAction(context: Context, action: String) {
            val intent = Intent(context, LocationTrackingService::class.java).setAction(action)
            ContextCompat.startForegroundService(context, intent)
        }
    }
}
