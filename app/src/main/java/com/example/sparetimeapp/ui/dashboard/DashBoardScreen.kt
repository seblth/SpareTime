package com.example.sparetimeapp.ui.dashboard

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.sparetimeapp.data.*
import kotlinx.coroutines.flow.firstOrNull

@Composable
fun DashboardScreen(
    repo: RulesRepo,
    onOpenAppList: () -> Unit,
    onOpenDevTools: () -> Unit,
    onOpenOnboarding: () -> Unit,
) {
    val packages by repo.packagesFlow().collectAsState(initial = emptyList())
    val totals by produceState(initialValue = 0 to 0, packages) {
        var minutes = 0
        var accesses = 0
        for (pkg in packages) {
            val s = repo.todayStatsFlow(pkg).firstOrNull() ?: continue
            minutes += s.minutesUsed
            accesses += s.accessesUsed
        }
        value = minutes to accesses
    }

    Column(
        Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Dashboard", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)

        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp)) {
                Text("Heute gesamt", style = MaterialTheme.typography.titleMedium)
                Text("${totals.first} Minuten · ${totals.second} Zugriffe")
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = onOpenAppList) { Text("Limit setzen") }
            OutlinedButton(onClick = onOpenDevTools) { Text("DevTools") }
            OutlinedButton(onClick = onOpenOnboarding) { Text("Onboarding") }
        }

        HorizontalDivider()

        Text("Aktive Limits", style = MaterialTheme.typography.titleMedium)
        LazyColumn(Modifier.weight(1f)) {
            items(packages) { pkg ->
                val rule by repo.ruleFlow(pkg).collectAsState(initial = null)
                if (rule != null) {
                    Card(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                        Column(Modifier.padding(12.dp)) {
                            Text(pkg, fontWeight = FontWeight.SemiBold)
                            Text("Min/Tag: ${rule!!.minutesLimit ?: "∞"}, Zugriffe: ${rule!!.accessLimit ?: "∞"}")
                        }
                    }
                }
            }
        }
    }
}
