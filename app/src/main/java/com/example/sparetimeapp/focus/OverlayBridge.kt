package com.example.sparetimeapp.focus

import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.Looper
import android.provider.Settings
import android.view.Gravity
import android.view.LayoutInflater
import android.view.WindowManager
import android.widget.Button
import android.widget.FrameLayout
import android.widget.TextView
import com.example.sparetimeapp.R
import com.example.sparetimeapp.data.util.DebugLog
import java.lang.ref.WeakReference
import android.os.Handler

object OverlayBridge {

    // WeakRefs vermeiden Leaks
    @Volatile private var rootRef: WeakReference<FrameLayout>? = null
    @Volatile private var wmRef: WeakReference<WindowManager>? = null
    private val lock = Any()

    // Main-Thread Handler
    private val mainHandler = Handler(Looper.getMainLooper())

    fun isShowing(): Boolean = rootRef?.get() != null

    fun show(context: Context, pkg: String, reason: String = "Tageslimit erreicht") {
        val appCtx = context.applicationContext

        if (!Settings.canDrawOverlays(appCtx)) {
            DebugLog.d("OVERLAY", "Missing overlay permission")
            return
        }

        mainHandler.post {
            synchronized(lock) {
                if (rootRef?.get() != null) {
                    DebugLog.d("OVERLAY", "already showing")
                    return@synchronized
                }

                val windowType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                    WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                else
                    WindowManager.LayoutParams.TYPE_PHONE

                val lp = WindowManager.LayoutParams(
                    WindowManager.LayoutParams.MATCH_PARENT,
                    WindowManager.LayoutParams.MATCH_PARENT,
                    windowType,
                    // keine NOT_FOCUSABLE-Flags -> Overlay fängt Touches
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                    PixelFormat.TRANSLUCENT
                ).apply {
                    gravity = Gravity.TOP or Gravity.START
                }

                val frame = FrameLayout(appCtx)
                val view = LayoutInflater.from(appCtx).inflate(R.layout.overlay_block, frame, false)
                frame.addView(
                    view,
                    FrameLayout.LayoutParams(
                        FrameLayout.LayoutParams.MATCH_PARENT,
                        FrameLayout.LayoutParams.MATCH_PARENT
                    )
                )

                // Texte
                view.findViewById<TextView>(R.id.title)?.text = "Blocked"
                view.findViewById<TextView>(R.id.subtitle)?.text =
                    "${resolveAppLabel(appCtx, pkg) ?: pkg}\n$reason"

                // Button
                view.findViewById<Button>(R.id.btnClose)?.setOnClickListener {
                    goHome(appCtx)
                    dismiss(appCtx)
                }

                val wm = appCtx.getSystemService(Context.WINDOW_SERVICE) as WindowManager
                wm.addView(frame, lp)

                rootRef = WeakReference(frame)
                wmRef   = WeakReference(wm)

                DebugLog.d("OVERLAY", "show (xml, main) for $pkg")
            }
        }
    }

    fun dismiss(context: Context) {
        val appCtx = context.applicationContext
        mainHandler.post {
            synchronized(lock) {
                val frame = rootRef?.get()
                val wm = wmRef?.get() ?: (appCtx.getSystemService(Context.WINDOW_SERVICE) as WindowManager)
                if (frame != null) {
                    try {
                        wm.removeViewImmediate(frame)
                    } catch (t: Throwable) {
                        DebugLog.d("OVERLAY", "dismiss remove failed: ${t.message}")
                    }
                }
                rootRef = null
                wmRef = null
                DebugLog.d("OVERLAY", "dismiss (main)")
            }
        }
    }

    private fun goHome(context: Context) {
        val i = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_HOME)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(i)
    }

    private fun resolveAppLabel(context: Context, pkg: String): String? = try {
        val pm = context.packageManager
        pm.getApplicationLabel(pm.getApplicationInfo(pkg, 0))?.toString()
    } catch (_: Throwable) { null }
}
