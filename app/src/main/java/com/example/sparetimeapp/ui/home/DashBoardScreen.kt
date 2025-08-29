// ui/dashboard/DashboardScreen.kt
package com.example.sparetimeapp.ui.dashboard

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.Divider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun DashboardScreen(
    onOpenOnboarding: () -> Unit,
    onOpenRules: () -> Unit,
    // Optional: onOpenDevTools: () -> Unit = {}
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Dashboard (Test-UI)")
        Divider()

        // 👉 Testing Navigation
        Text("Navigation (Testing)")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = onOpenOnboarding) { Text("Zu Onboarding") }
            Button(onClick = onOpenRules)      { Text("Zu Regeln") }
            // Optional: Button(onClick = onOpenDevTools) { Text("Dev-Tools") }
        }

        // Hier kommen später deine KPIs/Listen rein (M6)
    }
}
