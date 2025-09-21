package com.example.sparetimeapp.ui.onboarding

import android.app.AppOpsManager
import android.app.NotificationManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.text.TextUtils
import android.view.accessibility.AccessibilityManager
import com.example.sparetimeapp.focus.BlockerAccessibilityService

// --- USAGE ACCESS ---
fun isUsageAccessGranted(context: Context): Boolean {
    val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
    val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        appOps.unsafeCheckOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            android.os.Process.myUid(), context.packageName
        )
    } else {
        @Suppress("DEPRECATION")
        appOps.checkOpNoThrow(
            "android:get_usage_stats", android.os.Process.myUid(), context.packageName
        )
    }
    return mode == AppOpsManager.MODE_ALLOWED
}

fun openUsageAccessSettings(context: Context) {
    context.startActivity(
        Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    )
}

// --- OVERLAY (Draw over other apps) ---
fun isOverlayGranted(context: Context): Boolean =
    Settings.canDrawOverlays(context)

fun openOverlaySettings(context: Context) {
    val intent = Intent(
        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
        Uri.parse("package:${context.packageName}")
    ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    context.startActivity(intent)
}

// --- ACCESSIBILITY (Service aktiv?) ---
fun isAccessibilityEnabled(context: Context): Boolean {
    val am = context.getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager
    if (!am.isEnabled) return false

    val setting = Settings.Secure.getString(
        context.contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
    ) ?: return false

    val my = ComponentName(context, BlockerAccessibilityService::class.java)
    val full = "${my.packageName}/${BlockerAccessibilityService::class.java.name}"
    val flat = my.flattenToString() // com.app/.focus.BlockerAccessibilityService

    val splitter = TextUtils.SimpleStringSplitter(':').apply { setString(setting) }
    while (splitter.hasNext()) {
        val s = splitter.next()
        if (s.equals(full, true) || s.equals(flat, true) || s.endsWith(BlockerAccessibilityService::class.java.name, true)) {
            return true
        }
    }
    return false
}

fun openAccessibilitySettings(context: Context) {
    context.startActivity(
        Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    )
}

// --- NOTIFICATIONS (Android 13+) ---
fun needsPostNotificationPermission(): Boolean =
    Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU

fun isNotificationsEnabled(context: Context): Boolean {
    val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    return nm.areNotificationsEnabled()
}

fun openAppNotificationSettings(context: Context) {
    val intent = Intent()
        .setAction(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
        .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    context.startActivity(intent)
}