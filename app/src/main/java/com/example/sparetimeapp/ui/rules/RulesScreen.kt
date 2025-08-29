package com.example.sparetimeapp.ui.rules

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import com.example.sparetimeapp.data.*

@Composable
fun RulesScreen(
    repo: RulesRepo,
    onBack: () -> Unit
) {
    var pkg by remember { mutableStateOf("com.instagram.android") }
    var minTxt by remember { mutableStateOf("60") }
    var accTxt by remember { mutableStateOf("8") }
    var notif by remember { mutableStateOf(true) }
    val scope = rememberCoroutineScope()

    // Live anzeigen, was gespeichert ist
    val currentRule by repo.ruleFlow(pkg).collectAsState(
        initial = Rule(pkg, null, null, true)
    )

    Column(Modifier.padding(16.dp).fillMaxSize(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Rules (Test-UI)", style = MaterialTheme.typography.titleLarge)

        OutlinedTextField(pkg, { pkg = it }, label = { Text("Paketname") }, singleLine = true)
        OutlinedTextField(minTxt, { minTxt = it }, label = { Text("Minuten/Tag (leer = kein Limit)") }, singleLine = true)
        OutlinedTextField(accTxt, { accTxt = it }, label = { Text("Zugriffe/Tag (leer = kein Limit)") }, singleLine = true)

        Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
            Text("Benachrichtigungen")
            Spacer(Modifier.width(8.dp))
            Switch(checked = notif, onCheckedChange = { notif = it })
        }

        Button(onClick = {
            scope.launch {
                repo.setRule(
                    pkg = pkg,
                    minutesLimit = minTxt.toIntOrNull(),
                    accessLimit = accTxt.toIntOrNull(),
                    notifications = notif
                )
            }
        }) { Text("Speichern") }

        Divider(Modifier.padding(vertical = 8.dp))

        Text("Aktuelle Regel:")
        Text("Min-Limit: ${currentRule.minutesLimit ?: "–"}")
        Text("Access-Limit: ${currentRule.accessLimit ?: "–"}")
        Text("Notif: ${currentRule.notifications}")

        Spacer(Modifier.weight(1f)) // alles nach oben schieben
        Button(onClick = onBack) {
            Text("Zurück zum Dashboard")
        }
    }
}
