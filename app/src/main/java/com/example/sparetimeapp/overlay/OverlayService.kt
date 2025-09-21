package com.example.sparetimeapp.overlay

import android.app.Service
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.util.Log
import android.view.*
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import com.example.sparetimeapp.R


class OverlayService : Service() {

    private lateinit var windowManager: WindowManager
    private var overlayView: View? = null
    
    @Volatile private var suppressUntilMs = 0L
    @Volatile private var lastPkgShown: String? = null

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_SHOW_BLOCK -> {
                val pkg = intent.getStringExtra(EXTRA_PKG).orEmpty()
                val now = System.currentTimeMillis()

                // 1) kurz blockieren, falls gerade geschlossen
                if (now < suppressUntilMs) {
                    Log.d(TAG, "Suppressed SHOW_BLOCK (cooldown)")
                    return START_NOT_STICKY
                }

                // 2) Duplikate vermeiden
                if (overlayView != null && lastPkgShown == pkg) {
                    Log.d(TAG, "Overlay already visible for $pkg")
                    return START_NOT_STICKY
                }

                // 3) Homescreen/Launcher nicht blocken
                if (pkg.isBlank() || pkg == "com.android.launcher" || pkg == "com.google.android.apps.nexuslauncher") {
                    Log.d(TAG, "Ignore launcher/home")
                    return START_NOT_STICKY
                }

                lastPkgShown = pkg
                showOverlay(pkg)
            }
            ACTION_HIDE_BLOCK -> {
                removeOverlay()
                // Kurzer Cooldown, damit Watcher nicht sofort neu triggert
                suppressUntilMs = System.currentTimeMillis() + 1500L
                lastPkgShown = null
                stopSelf()
            }
        }
        return START_NOT_STICKY
    }


    private fun showOverlay(blockedPkg: String) {
        if (!Settings.canDrawOverlays(this)) {
            Toast.makeText(this, "Overlay-permission is missing", Toast.LENGTH_SHORT).show()
            return
        }
        if (overlayView != null) {
            // Bereits sichtbar -> optional Inhalt updaten
            overlayView?.findViewById<TextView>(R.id.subtitle)
            return
        }

        val inflater = getSystemService(LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val view = inflater.inflate(R.layout.overlay_block, null)

        view.findViewById<TextView>(R.id.subtitle)
        
            
        // Button zum Schließen
        view.findViewById<Button>(R.id.btnClose)?.setOnClickListener {
            val homeIntent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_HOME)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        startActivity(homeIntent)

         // Overlay weg
        removeOverlay()
        stopSelf()
        }

        // Tipp: Wenn du Touch/Keys komplett "schlucken" willst, lass NOT_TOUCH_MODAL weg
        // und nimm die View fokusierbar (kein FLAG_NOT_FOCUSABLE).
        // Hier: Fokusierbar, damit darunter nichts ankommt.
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else
                @Suppress("DEPRECATION")
                WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            // Für Geräte mit Notch:
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                layoutInDisplayCutoutMode =
                    WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            }
        }

        try {
            windowManager.addView(view, params)
            overlayView = view

            // Optional: Schließen per Tap auf den Hintergrund
            view.setOnTouchListener { _, _ -> /* Touch konsumieren, nichts durchlassen */ true }
        } catch (e: Exception) {
            Log.e(TAG, "addView failed", e)
            Toast.makeText(this, "Overlay could not be shown", Toast.LENGTH_SHORT).show()
        }
    }

     private fun removeOverlay() {
        overlayView?.let {
            runCatching { windowManager.removeView(it) }
        }
        overlayView = null
    }


    override fun onDestroy() {
        removeOverlay()
        super.onDestroy()
    }

    companion object {
        const val TAG = "OverlayService"
        const val ACTION_SHOW_BLOCK = "SHOW_BLOCK"
        const val ACTION_HIDE_BLOCK = "HIDE_BLOCK"
        const val EXTRA_PKG = "pkg"
    }
}

