package com.example.sparetimeapp.ui.applist

import android.annotation.SuppressLint
import android.content.pm.PackageManager
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@SuppressLint("QueryPermissionsNeeded")
@Composable
fun AppListScreen(
    pm: PackageManager,
    onAppSelected: (String) -> Unit,
    onBack: () -> Unit
) {
    val apps = remember {
        pm.getInstalledApplications(0)
            .filter { pm.getLaunchIntentForPackage(it.packageName) != null }
            .sortedBy { pm.getApplicationLabel(it).toString() }
    }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Apps", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            TextButton(onClick = onBack) { Text("Zurück") }
        }
        HorizontalDivider(Modifier.padding(vertical = 8.dp))

        LazyColumn {
            items(apps) { app ->
                val name = pm.getApplicationLabel(app).toString()
                ListItem(
                    headlineContent = { Text(name) },
                    supportingContent = { Text(app.packageName) },
                    modifier = Modifier.clickable { onAppSelected(app.packageName) }
                )
                HorizontalDivider()
            }
        }
    }
}
