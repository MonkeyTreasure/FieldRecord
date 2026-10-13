
package com.example.agriops

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class CameraWidgetReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        Log.d("AgriOpsCameraWidget", "Widget button pressed.")
        // Here you could start the camera activity directly if you expose a native Android Activity.
    }
}
