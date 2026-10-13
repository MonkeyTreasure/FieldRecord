package com.fieldrecord.location

class SimpleKalmanFilter(private var q: Float = 0.03f) {
    private var x: Float = 0f // estimated value
    private var p: Float = 1f // estimation error covariance
    private var k: Float = 0f // kalman gain
    private var initialized: Boolean = false

    fun update(measurement: Float, accuracy: Float): Float {
        if (!initialized) {
            x = measurement
            p = accuracy * accuracy
            initialized = true
            return x
        }
        
        // Prediction
        p += q

        // Measurement Update
        val r = accuracy * accuracy
        k = p / (p + r)
        x += k * (measurement - x)
        p *= (1f - k)

        return x
    }
}

class LocationKalmanFilter {
    private val latFilter = SimpleKalmanFilter(0.00001f)
    private val lonFilter = SimpleKalmanFilter(0.00001f)

    fun filter(lat: Double, lon: Double, accuracy: Float): Pair<Double, Double> {
        val filteredLat = latFilter.update(lat.toFloat(), accuracy)
        val filteredLon = lonFilter.update(lon.toFloat(), accuracy)
        return Pair(filteredLat.toDouble(), filteredLon.toDouble())
    }
}
