package com.example.sparetimeapp.ui.onboarding

import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import android.provider.Settings
import android.util.Log
import com.example.sparetimeapp.focus.BlockerAccessibilityService

@Composable
private fun OnResume(recheck: () -> Unit) {
    val owner = LocalLifecycleOwner.current
    DisposableEffect(owner) {
        val obs = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) recheck()
        }
        owner.lifecycle.addObserver(obs)
        onDispose { owner.lifecycle.removeObserver(obs) }
    }
}

@Composable
fun OnboardingScreen(
    onAllGranted: () -> Unit = {}
) {
    val ctx = LocalContext.current

    // Recompute, wenn wir zurück aus den Settings kommen:
    var usage by remember { mutableStateOf(isUsageAccessGranted(ctx)) }
    var overlay by remember { mutableStateOf(isOverlayGranted(ctx)) }
    var acc by remember { mutableStateOf(isAccessibilityEnabled(ctx)) }
    var notif by remember { mutableStateOf(isNotificationsEnabled(ctx)) }

    fun recheck() {
        usage = isUsageAccessGranted(ctx)
        overlay = isOverlayGranted(ctx)
        acc    = isAccessibilityEnabled(ctx)
        notif  = isNotificationsEnabled(ctx)
        Log.d("ACC", Settings.Secure.getString(ctx.contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES) ?: "none")
        Log.d("ACC", ComponentName(ctx, BlockerAccessibilityService::class.java).flattenToString())
    }

    LaunchedEffect(Unit) { recheck() }

    OnResume { recheck() }

    // Runtime-Permission Launcher (nur Android 13+)
    val notifLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        notif = if (granted) true else isNotificationsEnabled(ctx)
    }

    val allGreen = usage && overlay && acc && (!needsPostNotificationPermission() || notif)

    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Onboarding (Test-UI)", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        InfoRow(
            title = "Usage",
            subtitle = "Track App-Usage",
            ok = usage
        ) {
            openUsageAccessSettings(ctx)
        }
        InfoRow(
            title = "Overlay",
            subtitle = "Block-Screen for limited apps",
            ok = overlay
        ) {
            openOverlaySettings(ctx)
        }
        InfoRow(
            title = "Accessibility",
            subtitle = "Track App-Start",
            ok = acc
        ) {
            openAccessibilitySettings(ctx)
        }
        InfoRow(
            title = "Notifications",
            subtitle = "Notify that limit is reached",
            ok = if (needsPostNotificationPermission()) notif else true
        ) {
            if (needsPostNotificationPermission()) {
                // Erst versuchen, Runtime Permission zu holen
                notifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            } else {
                openAppNotificationSettings(ctx)
            }
        }

        Spacer(Modifier.height(12.dp))
        Button(
            enabled = allGreen,
            onClick = onAllGranted,
            modifier = Modifier.align(Alignment.End)
        ) { Text("Continue") }

        if (!allGreen) {
            AssistiveBanner("Please grant all permissions.")
        } else {
            AssistiveBanner("All set :) Please Continue...", ok = true)
        }
    }
}

@Composable
private fun InfoRow(title: String, subtitle: String, ok: Boolean, onClick: () -> Unit) {
    Surface(
        shape = MaterialTheme.shapes.medium,
        border = ButtonDefaults.outlinedButtonBorder,
        tonalElevation = 0.dp
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            StatusDot(ok = ok)
            Column(Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.SemiBold)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            OutlinedButton(onClick = onClick) { Text(if (ok) "Access granted" else "Grant Access") }
        }
    }
}

@Composable
private fun StatusDot(ok: Boolean) {
    val color = if (ok) Color(0xFF55D187) else Color(0xFFF9C74F)
    Box(
        Modifier
            .size(12.dp)
            .background(color, shape = MaterialTheme.shapes.extraLarge)
    )
}

@Composable
private fun AssistiveBanner(text: String, ok: Boolean = false) {
    val border = if (ok) Color(0x3355D187) else Color(0x33F9C74F)
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
        shape = MaterialTheme.shapes.medium,
        tonalElevation = 0.dp,
        border = ButtonDefaults.outlinedButtonBorder.copy(brush = androidx.compose.ui.graphics.SolidColor(border))    ) {
        Text(text, Modifier.padding(12.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

