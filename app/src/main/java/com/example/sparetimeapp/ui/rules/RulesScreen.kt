package com.example.sparetimeapp.ui.rules

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.shape.RoundedCornerShape
import com.example.sparetimeapp.data.*
import kotlinx.coroutines.launch

@Composable
fun RulesScreen(
    repo: RulesRepo,
    onBack: () -> Unit
) {
    var pkg by remember { mutableStateOf("com.instagram.android") }
    var minTxt by remember { mutableStateOf("") }
    var accTxt by remember { mutableStateOf("") }
    var notif by remember { mutableStateOf(true) }
    val scope = rememberCoroutineScope()

    // gespeicherte Pakete (Index)
    val packages by repo.packagesFlow().collectAsState(initial = emptyList())
    // aktuelle Regel (wechselt mit pkg)
    val currentRule by repo.ruleFlow(pkg).collectAsState(initial = Rule(pkg, null, null, true))

    // Felder aus aktueller Rule füllen
    LaunchedEffect(currentRule.pkg, currentRule.minutesLimit, currentRule.accessLimit, currentRule.notifications) {
        minTxt = currentRule.minutesLimit?.toString() ?: ""
        accTxt = currentRule.accessLimit?.toString() ?: ""
        notif = currentRule.notifications
    }

    var showConfirm by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text("Rules (Test-UI)", style = MaterialTheme.typography.titleLarge)

        if (packages.isNotEmpty()) {
            Text("Gespeicherte Pakete:")
            PackagePicker(
                packages = packages,
                selected = pkg,
                onSelect = { pkg = it }
            )
            Spacer(Modifier.height(8.dp))
        }

        HorizontalDivider()

        OutlinedTextField(
            value = pkg,
            onValueChange = { pkg = it },
            label = { Text("Paketname") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = minTxt,
                onValueChange = { minTxt = it },
                label = { Text("Minuten/Tag (leer = kein Limit)") },
                singleLine = true,
                modifier = Modifier.weight(1f)
            )
            OutlinedTextField(
                value = accTxt,
                onValueChange = { accTxt = it },
                label = { Text("Zugriffe/Tag (leer = kein Limit)") },
                singleLine = true,
                modifier = Modifier.weight(1f)
            )
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Benachrichtigungen")
            Spacer(Modifier.width(8.dp))
            Switch(checked = notif, onCheckedChange = { notif = it })
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Button(onClick = {
                scope.launch {
                    repo.setRuleAndReevaluate(
                        pkg = pkg.trim(),
                        minutesLimit = minTxt.trim().toIntOrNull(),
                        accessLimit  = accTxt.trim().toIntOrNull(),
                        notifications = notif
                        // mode/allowance nur, wenn du die Felder im RulesScreen anbietest
                    )

                }
            }) { Text("Speichern") }

            OutlinedButton(onClick = onBack) { Text("Zurück zum Dashboard") }

            Spacer(Modifier.weight(1f)) // schiebt Delete nach rechts

            DangerButton(onClick = { showConfirm = true })
        }

        HorizontalDivider(Modifier.padding(vertical = 8.dp))

        Text("Gelesene Regel:")
        Text("• Minutenlimit: ${currentRule.minutesLimit?.toString() ?: "–"}")
        Text("• Access-Limit: ${currentRule.accessLimit?.toString() ?: "–"}")
        Text("• Notifications: ${if (currentRule.notifications) "Ja" else "Nein"}")
    }

    if (showConfirm) {
        AlertDialog(
            onDismissRequest = { showConfirm = false },
            title = { Text("Regel löschen?") },
            text = { Text("Regel und heutige Nutzungswerte entfernen:\n${'$'}pkg") },
            confirmButton = {
                TextButton(onClick = {
                    showConfirm = false
                    scope.launch {
                        val deleting = pkg
                        repo.deletePackage(deleting)
                        // UI-State aktualisieren: auf verbleibendes Paket wechseln
                        val remaining = packages.filter { it != deleting }
                        pkg = remaining.firstOrNull() ?: ""
                        // Felder leeren, falls nichts mehr übrig ist
                        if (pkg.isEmpty()) {
                            minTxt = ""; accTxt = ""; notif = true
                        }
                    }
                }) { Text("Löschen") }
            },
            dismissButton = { TextButton(onClick = { showConfirm = false }) { Text("Abbrechen") } }
        )
    }
}

@Composable
private fun PackagePicker(
    packages: List<String>,
    selected: String,
    onSelect: (String) -> Unit,
    shape: Shape = RoundedCornerShape(20.dp)
) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(end = 8.dp)
    ) {
        items(packages, key = { it }) { p ->
            OutlinedButton(
                onClick = { onSelect(p) },
                enabled = selected != p,
                shape = shape,
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text(p.takeLast(24), maxLines = 1)
            }
        }
    }
}

@Composable
private fun DangerButton(
    onClick: () -> Unit,
    text: String = "Löschen",
) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.error,
            contentColor   = MaterialTheme.colorScheme.onError
        ),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .heightIn(min = 40.dp)                // fixe, normale Button-Höhe
            .defaultMinSize(minWidth = 96.dp),    // verhindert zu schmale/„vertikale“ Buttons
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp)
    ) {
        Text(text, maxLines = 1, softWrap = false)  // niemals umbrechen
    }
}
