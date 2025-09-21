package com.example.sparetimeapp.focus

import android.accessibilityservice.AccessibilityService
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.view.accessibility.AccessibilityEvent
import com.example.sparetimeapp.data.RulesRepo
import com.example.sparetimeapp.data.SettingsStore
import com.example.sparetimeapp.data.util.DebugLog

class BlockerAccessibilityService : AccessibilityService() {

    private lateinit var repo: RulesRepo
    private lateinit var session: SessionController

    private var lastPkg: String? = null
    private var lastPkgChangedAt: Long = 0L
    private var homePkgs: Set<String> = emptySet()

    private val screenReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                Intent.ACTION_SCREEN_OFF -> { session.onAppGone(); DebugLog.d("ACC", "SCREEN_OFF") }
                Intent.ACTION_SCREEN_ON  -> { lastPkgChangedAt = System.currentTimeMillis(); DebugLog.d("ACC", "SCREEN_ON") }
            }
        }
    }

    private fun defaultHomePackages(): Set<String> {
        val i = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
        val pm = packageManager
        val list = pm.queryIntentActivities(i, 0)
        return list.map { it.activityInfo.packageName }.toSet()
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        repo = RulesRepo(SettingsStore(applicationContext), applicationContext)
        session = SessionController(applicationContext, repo)
        val filter = IntentFilter().apply { addAction(Intent.ACTION_SCREEN_OFF); addAction(Intent.ACTION_SCREEN_ON) }
        registerReceiver(screenReceiver, filter)
        homePkgs = defaultHomePackages()
        DebugLog.d("ACC", "service connected; homePkgs=$homePkgs")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // akzeptiere beide relevanten Typen
        when (event?.eventType) {
            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED,
            AccessibilityEvent.TYPE_WINDOWS_CHANGED,
            AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED -> { /* continue */ }
            else -> return
        }

        val pkg = event.packageName?.toString() ?: return

        // Eigene App + System ignorieren
        if (pkg == packageName || pkg == "android" || pkg.startsWith("com.android.systemui")) return

        // Homescreen/Launcher ignorieren (explizit + dynamisch ermittelt)
        val knownLaunchers = setOf(
            "com.google.android.apps.nexuslauncher",
            "com.android.launcher3",
            "com.sec.android.app.launcher",
            "com.miui.home"
        )
        if (pkg in homePkgs || pkg in knownLaunchers) {
            // WICHTIG: Ticker aus + Debounce-Reset, damit Rückkehr nicht verschluckt wird
            session.onAppGone()
            lastPkg = null
            lastPkgChangedAt = System.currentTimeMillis()
            DebugLog.d("ACC", "home/launcher → stop ticker & reset lastPkg")
            return
        }

        val now = System.currentTimeMillis()

        // Entprellen: nur bei echtem Paketwechsel
        if (pkg == lastPkg && now - lastPkgChangedAt < 800L) return

        if (pkg != lastPkg) {
            lastPkg = pkg
            lastPkgChangedAt = now
            DebugLog.d("ACC", "switch → $pkg (type=${event.eventType})")
            session.onAppOpened(pkg)
        }
    }

    override fun onInterrupt() { /* no-op */ }

    override fun onDestroy() {
        unregisterReceiver(screenReceiver)
        session.onAppGone()
        DebugLog.d("ACC", "service destroyed")
        super.onDestroy()
    }
}
