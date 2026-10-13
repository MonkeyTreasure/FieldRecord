package com.fieldrecord.location

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat


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

class LocationTrackingService : Service(), LocationListener {
    private var locationManager: LocationManager? = null


    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, notification())
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            stopSelf()
            return
        }
        locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        try {
            locationManager?.getLastKnownLocation(LocationManager.GPS_PROVIDER)?.let { cached ->
                if (isUsableGpsFix(cached)) {
                    LocationStore.save(this, cached)
                }
            }
            // Use the handset GNSS/GPS provider only. Network fixes may be much less precise
            // and must never replace the coordinates used by the map or captured photos.
            locationManager?.requestLocationUpdates(LocationManager.GPS_PROVIDER, 500L, 0f, this)
        } catch (_: SecurityException) {
            stopSelf()
        } catch (_: IllegalArgumentException) {
            // A device may not provide one of the location providers.
        }
    }

    override fun onLocationChanged(location: Location) {
        if (location.provider == LocationManager.GPS_PROVIDER && isUsableGpsFix(location)) {
            LocationStore.save(this, location)
        }
    }

    private fun isUsableGpsFix(location: Location): Boolean =
        location.latitude.isFinite() && location.longitude.isFinite() &&
            location.latitude in -90.0..90.0 && location.longitude in -180.0..180.0 &&
            !(location.latitude == 0.0 && location.longitude == 0.0) &&
            location.hasAccuracy() && location.accuracy <= MAX_GPS_ACCURACY_METERS
    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        locationManager?.removeUpdates(this)
        super.onDestroy()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(CHANNEL_ID, "Pelacakan lokasi", NotificationManager.IMPORTANCE_LOW)
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }

    private fun notification(): Notification = NotificationCompat.Builder(this, CHANNEL_ID)
        .setContentTitle("Field Record aktif")
        .setContentText("Memperbarui lokasi untuk peta dan foto")
        .setSmallIcon(com.fieldrecord.R.mipmap.ic_launcher)
        .setOngoing(true)
        .build()

    companion object {
        private const val CHANNEL_ID = "location_tracking"
        private const val NOTIFICATION_ID = 2104
        private const val MAX_GPS_ACCURACY_METERS = 100f

        fun start(context: Context) {
            val intent = Intent(context, LocationTrackingService::class.java)
            ContextCompat.startForegroundService(context, intent)
        }
    }
}
