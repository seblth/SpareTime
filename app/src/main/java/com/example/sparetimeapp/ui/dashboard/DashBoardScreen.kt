package com.example.sparetimeapp.ui.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.sparetimeapp.data.RulesRepo
import com.example.sparetimeapp.ui.theme.*
import androidx.compose.foundation.clickable
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.graphics.Color


@Composable
fun DashboardScreen(
    repo: RulesRepo,
    onOpenOnboarding: () -> Unit,
    onOpenRules: () -> Unit,
    onOpenDevTools: () -> Unit,
) {
    val vm = remember { DashboardViewModel(repo) }
    val totals   by vm.totals.collectAsState()
    val top      by vm.topByMinutes.collectAsState()
    val blocked  by vm.blockedApps.collectAsState()

    Scaffold(containerColor = MaterialTheme.colorScheme.background) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
            item {
                Box(Modifier.fillMaxWidth()) {
                    Text(
                        "SpareTime",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.align(Alignment.CenterStart)
                    )

                    // small white button (no icon/text)
                    Box(
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .size(18.dp)                  // make it as small/big as you like
                            .clip(CircleShape)
                            .background(Color.White)
                            .clickable(onClick = onOpenOnboarding)
                    )
                }
            }
            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    PillButtonOutlined("Dashboard", {}, Modifier.weight(1f))
                    PillButtonFilled("Set limits", onOpenRules, Modifier.weight(1f))
                }
            }
            item {
                Text(
                    "Today total: ${totals.minutes} Min · ${totals.accesses} Accesses · ${totals.blockedCount} blocked",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
            }

            // Limited Apps
            item { SectionTitle("Limited Apps") }
            item {
                Surface(
                    shape = MaterialTheme.shapes.large,
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 0.dp,
                    border = ButtonDefaults.outlinedButtonBorder.copy(
                        width = 1.dp,
                        brush = androidx.compose.ui.graphics.SolidColor(BlueOutline.copy(alpha = 0.25f))
                    ),
                ) {
                    Column(
                        Modifier.fillMaxWidth().padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        top.forEach { row ->
                            TopAppCard(
                                appLabel     = row.pkg.substringAfterLast('.').replaceFirstChar { it.uppercaseChar() },
                                pkg          = row.pkg,
                                minutesUsed  = row.stats.minutesUsed,
                                accesses     = row.stats.accessesUsed,
                                limitMinutes = row.limitMinutes,
                                limitAccesses= row.limitAccesses,
                                isBlocked    = row.isBlocked,
                                showProgress = true
                            )
                        }
                    }
                }
            }

            // Blocked Apps
            item { SectionTitle("Blocked Apps") }
            item {
                Surface(
                    shape = MaterialTheme.shapes.large,
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 0.dp,
                    border = ButtonDefaults.outlinedButtonBorder.copy(
                        width = 1.dp,
                        brush = androidx.compose.ui.graphics.SolidColor(BlueOutline.copy(alpha = 0.25f))
                    ),
                ) {
                    if (blocked.isEmpty()) {
                        Box(Modifier.fillMaxWidth().padding(16.dp)) {
                            Text("No App is blocked.", color = TextSecondary)
                        }
                    } else {
                        Column(
                            Modifier.fillMaxWidth().padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            blocked.forEach { row ->
                                TopAppCard(
                                    appLabel      = row.pkg.substringAfterLast('.').replaceFirstChar { it.uppercaseChar() },
                                    pkg           = row.pkg,
                                    minutesUsed   = row.stats.minutesUsed,
                                    accesses      = row.stats.accessesUsed,
                                    limitMinutes  = row.limitMinutes,
                                    limitAccesses = row.limitAccesses,
                                    isBlocked     = true,
                                    showProgress  = false   // unten nur Status/Texte
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/* ---------- Helper UI ---------- */

@Composable
private fun SectionTitle(text: String) {
    Text(text, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
}

@Composable
private fun PillButtonOutlined(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.height(64.dp),
        shape = MaterialTheme.shapes.extraLarge,
        border = ButtonDefaults.outlinedButtonBorder.copy(
            width = 2.dp,
            brush = androidx.compose.ui.graphics.SolidColor(BlueOutline)
        ),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = TextPrimary
        ),
        contentPadding = PaddingValues(horizontal = 24.dp)
    ) { Text(text, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold) }
}

@Composable
private fun PillButtonFilled(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Button(
        onClick = onClick,
        modifier = modifier.height(64.dp),
        shape = MaterialTheme.shapes.extraLarge,
        colors = ButtonDefaults.buttonColors(containerColor = PillBg, contentColor = TextPrimary),
        border = ButtonDefaults.outlinedButtonBorder.copy(
            width = 1.dp,
            brush = androidx.compose.ui.graphics.SolidColor(PillStroke)
        ),
        contentPadding = PaddingValues(horizontal = 24.dp)
    ) { Text(text, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold) }
}

@Composable
private fun TopAppCard(
    appLabel: String,
    pkg: String,
    minutesUsed: Int,
    accesses: Int,
    limitMinutes: Int?,
    limitAccesses: Int?,
    isBlocked: Boolean,
    showProgress: Boolean = true
) {
    Surface(
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 0.dp,
        border = ButtonDefaults.outlinedButtonBorder.copy(
            width = 1.dp,
            brush = androidx.compose.ui.graphics.SolidColor(PillStroke)
        ),
    ) {
        Column(Modifier.fillMaxWidth().padding(16.dp)) {

            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = MaterialTheme.shapes.extraLarge,
                    color = PillBg,
                    tonalElevation = 0.dp,
                    modifier = Modifier.size(28.dp)
                ) { Box(Modifier.fillMaxSize()) }

                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(appLabel, fontWeight = FontWeight.SemiBold)
                    Text(pkg, color = TextSecondary, style = MaterialTheme.typography.bodySmall)
                }
                StatusChip(if (isBlocked) "Blocked" else "Free", isBlocked)
            }

            Spacer(Modifier.height(12.dp))

            // Minuten-Progress (oben)
            val minutesProgress = if (!showProgress) null else limitMinutes?.let {
                if (it > 0) minutesUsed.coerceAtMost(it).toFloat() / it.toFloat() else null
            }
            if (minutesProgress != null) {
                LinearProgressIndicator(
                    progress = { minutesProgress },
                    modifier = Modifier.fillMaxWidth().height(6.dp),
                    trackColor = PillStroke,
                )
                Spacer(Modifier.height(4.dp))
            }

            // Zugriffs-Progress (unten)
            val accessesProgress = if (!showProgress) null else limitAccesses?.let {
                if (it > 0) accesses.coerceAtMost(it).toFloat() / it.toFloat() else null
            }
            if (accessesProgress != null) {
                LinearProgressIndicator(
                    progress = { accessesProgress },
                    modifier = Modifier.fillMaxWidth().height(6.dp),
                    trackColor = PillStroke,
                )
                Spacer(Modifier.height(8.dp))
            }

            // Infozeile
            val minutesInfo  = if (limitMinutes  != null && limitMinutes  > 0) "${minutesUsed}/${limitMinutes} m" else "${minutesUsed} m"
            val accessesInfo = if (limitAccesses != null && limitAccesses > 0) "${accesses}/${limitAccesses} Accesses" else "${accesses} Accesses"
            Text("$minutesInfo · $accessesInfo", color = TextSecondary, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun StatusChip(text: String, isBlocked: Boolean) {
    Surface(
        shape = MaterialTheme.shapes.extraLarge,
        color = (if (isBlocked) RedBad else GreenOK).copy(alpha = 0.15f),
        border = ButtonDefaults.outlinedButtonBorder.copy(
            width = 1.dp,
            brush = androidx.compose.ui.graphics.SolidColor(if (isBlocked) RedBad else GreenOK)
        )
    ) {
        Text(
            text,
            Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            color = TextPrimary,
            style = MaterialTheme.typography.labelMedium
        )
    }
}
