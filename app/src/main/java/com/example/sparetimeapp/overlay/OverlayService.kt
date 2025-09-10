package com.example.sparetimeapp.overlay

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log
import android.widget.Toast

class OverlayService : Service() {

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_SHOW_BLOCK -> {
                val pkg = intent.getStringExtra(EXTRA_PKG).orEmpty()
                Log.d(TAG, "SHOW_BLOCK for $pkg")
                // MVP-Stub: wir zeigen erstmal nur einen Toast (M4: echte Overlay-View)
                Toast.makeText(this, "Block: $pkg", Toast.LENGTH_SHORT).show()
                // TODO(M4): Fullscreen-Overlay via WindowManager + TYPE_APPLICATION_OVERLAY
            }
            else -> Log.d(TAG, "Unknown action=${intent?.action}")
        }
        return START_NOT_STICKY
    }

    companion object {
        const val TAG = "OverlayService"
        const val ACTION_SHOW_BLOCK = "SHOW_BLOCK"
        const val EXTRA_PKG = "pkg"
    }
}
