package com.example.sparetimeapp.ui.devtools

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.sparetimeapp.data.*
import com.example.sparetimeapp.data.util.DebugLog
import com.example.sparetimeapp.focus.Notifications
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun DevToolsScreen(
    repo: RulesRepo,
    onBack: () -> Unit,
    defaultPkg: String = "com.android.settings" // Emulator-freundlich
) {
    val scope = rememberCoroutineScope()

    // Paket-Index
    val packages by repo.packagesFlow().collectAsState(initial = emptyList())

    // Aktuelles Test-Paket
    var pkg by remember { mutableStateOf(defaultPkg) }

    // Live-Regel/Status
    val rule by repo.ruleFlow(pkg).collectAsState(initial = Rule(pkg, null, null, true))
    val stats by repo.todayStatsFlow(pkg).collectAsState(initial = TodayStats(0, 0, 0))
    val allowanceUntil by repo.allowanceUntilFlow(pkg).collectAsState(initial = 0L)

    // Edit-Felder
    var minTxt by remember { mutableStateOf("") }
    var accTxt by remember { mutableStateOf("") }
    var mode by remember { mutableStateOf("foreground") }
    var allowanceTxt by remember { mutableStateOf("") }

    LaunchedEffect(rule.pkg, rule.minutesLimit, rule.accessLimit, rule.countingMode, rule.allowanceMinutes) {
        minTxt = rule.minutesLimit?.toString() ?: ""
        accTxt = rule.accessLimit?.toString() ?: ""
        mode = rule.countingMode
        allowanceTxt = rule.allowanceMinutes?.toString() ?: ""
    }

    // 1s-Ticker für Countdown-Anzeige
    var now by remember { mutableStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) { while (true) { delay(1000); now = System.currentTimeMillis() } }

    val over = repo.isOverLimit(rule, stats)
    val blocked = repo.isBlockedNow(stats)
    val allowanceRemain = (allowanceUntil - now).coerceAtLeast(0L)

    // --- UI-Logger + global DebugLog feed ---
    val uiLogs = remember { mutableStateListOf<String>() }
    fun logLocal(msg: String) {
        val ts = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
        uiLogs.add(0, "[$ts] $msg")
        if (uiLogs.size > 300) uiLogs.removeAt(uiLogs.lastIndex)
    }
    val globalLogs by DebugLog.lines().collectAsState(initial = emptyList())

    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Dev Tools (M3)", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)

        // Paketliste
        if (packages.isNotEmpty()) {
            Text("Gespeicherte Pakete:")
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), contentPadding = PaddingValues(horizontal = 2.dp)) {
                items(packages, key = { it }) { p ->
                    OutlinedButton(onClick = { pkg = p }, enabled = pkg != p) { Text(p.takeLast(24)) }
                }
            }
        }

        OutlinedTextField(pkg, { pkg = it }, label = { Text("Paketname") }, singleLine = true, modifier = Modifier.fillMaxWidth())

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(minTxt, { minTxt = it }, label = { Text("Min/Tag (leer=∞)") }, singleLine = true, modifier = Modifier.weight(1f))
            OutlinedTextField(accTxt, { accTxt = it }, label = { Text("Zugriffe/Tag (leer=∞)") }, singleLine = true, modifier = Modifier.weight(1f))
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(mode, { mode = it }, label = { Text("Mode: foreground/allowance") }, singleLine = true, modifier = Modifier.weight(1f))
            OutlinedTextField(allowanceTxt, { allowanceTxt = it }, label = { Text("Allowance (Min/Zugriff)") }, singleLine = true, modifier = Modifier.weight(1f))
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = {
                scope.launch {
                    repo.setRuleAndReevaluate(
                        pkg = pkg.trim(),
                        minutesLimit = minTxt.trim().toIntOrNull(),
                        accessLimit = accTxt.trim().toIntOrNull(),
                        notifications = true,
                        countingMode = mode.trim().ifEmpty { "foreground" },
                        allowanceMinutes = allowanceTxt.trim().toIntOrNull()
                    )
                    logLocal("Regel gespeichert & neu bewertet für $pkg")
                }
            }) { Text("Regel speichern") }
            OutlinedButton(onClick = onBack) { Text("Zurück") }
        }

        HorizontalDivider()

        // Live-Status
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            StatRow("Minuten genutzt", "${stats.minutesUsed}/${rule.minutesLimit ?: "∞"}")
            StatRow("Zugriffe", "${stats.accessesUsed}/${rule.accessLimit ?: "∞"}")
            StatRow("Block aktiv", if (blocked) "ja → bis ${stats.blockedUntil.asTime()}" else "nein")
            StatRow("Über Limit?", if (over) "JA" else "nein")
            StatRow("Mode", rule.countingMode)
            StatRow("Allowance", rule.allowanceMinutes?.let { "$it min/Zugriff" } ?: "–")
            StatRow("Allowance läuft", if (allowanceUntil > 0L) "bis ${allowanceUntil.asTime()} (${allowanceRemain.msToPretty()})" else "–")

            val wouldBlock = blocked || over || (rule.countingMode == "allowance" && allowanceRemain <= 0L)
            Box(
                Modifier.fillMaxWidth().background(if (wouldBlock) Color(0x33FF5252) else Color(0x3327AE60)).padding(8.dp)
            ) { Text(if (wouldBlock) "WÜRDE BLOCKEN (jetzt)" else "WÜRDE FREIGEBEN (jetzt)") }
        }

        HorizontalDivider()

        // Simulation
        Text("Simulation:")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = { scope.launch { repo.incAccess(pkg); logLocal("+1 Zugriff für $pkg"); DebugLog.d("DEVTOOLS", "Simuliert: +1 Zugriff für $pkg") } }) { Text("+1 Zugriff") }
            OutlinedButton(onClick = { scope.launch { repo.incUsedMinute(pkg); logLocal("+1 Minute für $pkg") } }) { Text("+1 Minute") }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = {
                scope.launch {
                    val a = allowanceTxt.trim().toIntOrNull() ?: 5
                    repo.startAllowance(pkg, a)
                    logLocal("Allowance gestartet: ${a}m für $pkg")
                }
            }) { Text("Allowance starten") }
            OutlinedButton(onClick = { scope.launch { repo.clearAllowance(pkg); logLocal("Allowance gelöscht für $pkg") } }) { Text("Allowance löschen") }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = { scope.launch { repo.blockUntilMidnight(pkg); logLocal("Block bis 00:00 gesetzt für $pkg") } }) { Text("Block bis 00:00") }
            OutlinedButton(onClick = { scope.launch { repo.clearBlock(pkg); logLocal("Block gelöscht für $pkg") } }) { Text("Block löschen") }
            OutlinedButton(onClick = { scope.launch { repo.reevaluateBlockFor(pkg); logLocal("Neu bewertet für $pkg") } }) { Text("Neu bewerten") }
        }

        HorizontalDivider()

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = {
                DebugLog.d("DEVTOOLS", "Test-Log aus UI gedrückt")
                logLocal("Test-Log gesendet")
            }) { Text("Test-Log") }
        }

        // Logs: global (aus Services/Controller) + lokal (UI-Aktionen)
        Text("Logs:")
        LazyColumn(Modifier.weight(1f)) {
            items(globalLogs) { line -> Text(line, style = MaterialTheme.typography.bodySmall) }
            items(uiLogs) { line -> Text(line, style = MaterialTheme.typography.bodySmall, color = Color(0xFF2E7D32)) }
        }
    }
}

@Composable
private fun StatRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label)
        Text(value, fontWeight = FontWeight.SemiBold)
    }
}

private fun Long.asTime(): String {
    if (this <= 0) return "–"
    val sdf = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
    return sdf.format(Date(this))
}
private fun Long.msToPretty(): String {
    val s = this / 1000
    val m = s / 60
    val sec = s % 60
    return if (this <= 0) "0s" else "${m}m ${sec}s"
}

