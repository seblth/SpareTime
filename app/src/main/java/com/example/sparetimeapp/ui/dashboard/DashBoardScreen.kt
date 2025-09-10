package com.example.sparetimeapp.ui.dashboard

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.sparetimeapp.data.RulesRepo

@Composable
fun DashboardScreen(
    repo: RulesRepo,
    onOpenOnboarding: () -> Unit,
    onOpenRules: () -> Unit,
    onOpenDevTools: () -> Unit,
) {
    val vm = remember { DashboardViewModel(repo) }
    val totals by vm.totals.collectAsState()
    val top by vm.topByMinutes.collectAsState()

    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Dashboard (Test-UI)", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)

        Text("Heute gesamt: ${totals.minutes} Min · ${totals.accesses} Zugriffe · ${totals.blockedCount} geblockt")

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = onOpenOnboarding) { Text("Onboarding") }
            Button(onClick = onOpenRules) { Text("Regeln") }
            Button(onClick = onOpenDevTools) { Text("Dev Tools") }
        }

        Divider()

        Text("Top Apps (Minuten heute)", fontWeight = FontWeight.SemiBold)
        LazyColumn(Modifier.weight(1f)) {
            items(top) { row ->
                ListItem(
                    headlineContent = { Text(row.pkg) },
                    supportingContent = { Text("${row.stats.minutesUsed} min · ${row.stats.accessesUsed} Zugriffe") }
                )
                Divider()
            }
        }
    }
}