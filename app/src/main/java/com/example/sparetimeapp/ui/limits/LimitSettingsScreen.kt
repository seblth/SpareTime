package com.example.sparetimeapp.ui.limits

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.sparetimeapp.data.RulesRepo
import kotlinx.coroutines.launch

@Composable
fun LimitSettingsScreen(
    repo: RulesRepo,
    pkg: String,
    onBack: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var minutes by remember { mutableStateOf("") }
    var accesses by remember { mutableStateOf("") }

    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("Limits für $pkg", style = MaterialTheme.typography.titleLarge)

        OutlinedTextField(
            value = minutes,
            onValueChange = { minutes = it },
            label = { Text("Minuten pro Tag (leer=∞)") },
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = accesses,
            onValueChange = { accesses = it },
            label = { Text("Zugriffe pro Tag (leer=∞)") },
            modifier = Modifier.fillMaxWidth()
        )

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = {
                scope.launch {
                    repo.setRuleAndReevaluate(
                        pkg = pkg,
                        minutesLimit = minutes.toIntOrNull(),
                        accessLimit = accesses.toIntOrNull(),
                        notifications = true
                    )
                    onBack()
                }
            }) { Text("Speichern") }

            OutlinedButton(onClick = onBack) { Text("Abbrechen") }
        }
    }
}
