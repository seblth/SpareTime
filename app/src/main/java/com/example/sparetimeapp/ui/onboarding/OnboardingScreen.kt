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
import com.example.sparetimeapp.ui.theme.*
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue



@Composable
private fun StatusDot(ok: Boolean) {
    val color = if (ok) GreenOK else RedBad
    Box(
        Modifier.size(12.dp)
            .background(color, shape = MaterialTheme.shapes.extraLarge)
    )
}

@Composable
private fun OutlinePill(text: String, onClick: () -> Unit, enabled: Boolean = true) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        shape = MaterialTheme.shapes.extraLarge,
        border = ButtonDefaults.outlinedButtonBorder.copy(
            width = 2.dp,
            brush = androidx.compose.ui.graphics.SolidColor(BlueOutline)
        ),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = androidx.compose.ui.graphics.Color.Transparent,
            contentColor = TextPrimary
        ),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
    ) {
        Text(text, style = MaterialTheme.typography.labelMedium)
    }
}

@Composable
private fun PermissionCard(
    title: String,
    ok: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 0.dp,
        border = ButtonDefaults.outlinedButtonBorder.copy(
            width = 1.dp,
            brush = androidx.compose.ui.graphics.SolidColor(BlueOutline.copy(alpha = 0.25f))
        )
    ) {
        Row(
            Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            StatusDot(ok)
            Spacer(Modifier.width(12.dp))
            Text(
                title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f)
            )
            OutlinePill(text = if (ok) "Allowed" else "Allow", onClick = onClick)
        }
    }
}

@Composable
private fun OnResume(recheck: () -> Unit) {
    val owner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    DisposableEffect(owner) {
        val obs = androidx.lifecycle.LifecycleEventObserver { _, e ->
            if (e == androidx.lifecycle.Lifecycle.Event.ON_RESUME) recheck()
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

    var usage  by remember { mutableStateOf(false) }
    var overlay by remember { mutableStateOf(false) }
    var acc    by remember { mutableStateOf(false) }
    var notif  by remember { mutableStateOf(false) }

    fun recheck() {
        usage  = isUsageAccessGranted(ctx)
        overlay = isOverlayGranted(ctx)
        acc    = isAccessibilityEnabled(ctx)
        notif  = isNotificationsEnabled(ctx)
    }

    LaunchedEffect(Unit) { recheck() }
    OnResume { recheck() }

    val notifLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> notif = if (granted) true else isNotificationsEnabled(ctx) }

    val allGreen = usage && overlay && acc && (!needsPostNotificationPermission() || notif)

    Scaffold(containerColor = MaterialTheme.colorScheme.background) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(padding)
                .padding(16.dp)
        ) {
            Text("Onboarding", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(12.dp))

            // --- Karten (mit Punkt) UNTEREINANDER ---
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                PermissionCard(
                    title = "Usage access",
                    ok = usage,
                    onClick = { openUsageAccessSettings(ctx) },
                    modifier = Modifier.fillMaxWidth()
                )
                PermissionCard(
                    title = "Accessibility",
                    ok = acc,
                    onClick = { openAccessibilitySettings(ctx) },
                    modifier = Modifier.fillMaxWidth()
                )
                PermissionCard(
                    title = "Overlay",
                    ok = overlay,
                    onClick = { openOverlaySettings(ctx) },
                    modifier = Modifier.fillMaxWidth()
                )
                PermissionCard(
                    title = "Notifications",
                    ok = if (needsPostNotificationPermission()) notif else true,
                    onClick = {
                        if (needsPostNotificationPermission())
                            notifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        else
                            openAppNotificationSettings(ctx)
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(Modifier.height(16.dp))

            val bannerText = if (allGreen) "All set :) Please Continue..." else "Activate all rights"
            Surface(
                color = MaterialTheme.colorScheme.surface,
                shape = MaterialTheme.shapes.medium,
                tonalElevation = 0.dp,
                border = ButtonDefaults.outlinedButtonBorder.copy(
                    width = 1.dp,
                    brush = androidx.compose.ui.graphics.SolidColor(
                        (if (allGreen) GreenOK else BlueOutline).copy(alpha = 0.25f)
                    )
                )
            ) {
                Text(
                    bannerText,
                    Modifier.padding(12.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(Modifier.weight(1f))

            Button(
                enabled = allGreen,
                onClick = onAllGranted,
                shape = MaterialTheme.shapes.extraLarge,
                colors = ButtonDefaults.buttonColors(
                    containerColor = PillBg,
                    contentColor = TextPrimary
                ),
                border = ButtonDefaults.outlinedButtonBorder.copy(
                    width = 1.dp,
                    brush = androidx.compose.ui.graphics.SolidColor(PillStroke)
                ),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
                modifier = Modifier.align(Alignment.End)
            ) { Text("Continue", fontWeight = FontWeight.Bold) }
        }
    }
}
