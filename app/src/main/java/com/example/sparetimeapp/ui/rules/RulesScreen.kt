@file:OptIn(ExperimentalMaterial3Api::class)
package com.example.sparetimeapp.ui.rules

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.sparetimeapp.data.*
import com.example.sparetimeapp.ui.theme.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.firstOrNull
import androidx.compose.material3.ExperimentalMaterial3Api
import android.content.Intent
import android.content.pm.PackageManager
import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.compose.ui.platform.LocalContext
import android.os.Build
import android.content.pm.ApplicationInfo
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable





@Composable
fun RulesScreen(
    repo: RulesRepo,
    onBackToDashboard: () -> Unit
) {
    val scope = rememberCoroutineScope()



    // All known packages
    val packages by repo.packagesFlow().collectAsState(initial = emptyList())

    // Which package is selected in the editor (default: none)
    var selectedPkg by remember { mutableStateOf<String?>(null) }

    // Current rule of the selected package
    val selectedRule by produceState<Rule?>(initialValue = null, selectedPkg) {
        value = selectedPkg?.let { pkg ->
            // collectOnce: simple way for this screen – updates when pkg changes
            repo.ruleFlow(pkg).firstOrNull()
        }
    }

    

    // Editor state (filled when a package is selected)
    var minTxt by remember { mutableStateOf("") }
    var accTxt by remember { mutableStateOf("") }
    var notif  by remember { mutableStateOf(true) }

    // When selection or rule changes, push values into the editor
    LaunchedEffect(selectedPkg, selectedRule) {
        if (selectedRule != null) {
            minTxt = selectedRule?.minutesLimit?.toString() ?: ""
            accTxt = selectedRule?.accessLimit?.toString() ?: ""
            notif  = selectedRule?.notifications ?: true
        } else {
            minTxt = ""; accTxt = ""; notif = true
        }
    }

    // bottom sheet for “New”
    var showPicker by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val fieldsEnabled = packages.isNotEmpty()
    var showConfirm by remember { mutableStateOf(false) }


    val ctx = LocalContext.current
    var allApps by remember { mutableStateOf<List<AppEntry>>(emptyList()) }

    val pickerApps by remember(allApps, packages) {
        derivedStateOf { allApps.filter { it.packageName !in packages } }
    }

    // delete
    var showDeleteConfirm by remember { mutableStateOf(false) }


    LaunchedEffect(showPicker) {
        if (showPicker && allApps.isEmpty()) {
            allApps = loadLaunchableApps(ctx, includeSystem = true) // YouTube ist System-App → true!
        }
    }
    


   Scaffold(
    containerColor = MaterialTheme.colorScheme.background,
    floatingActionButton = {
        ExtendedFloatingActionButton(onClick = { showPicker = true }) { Text("New") }
    }
    ) { padding ->
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(padding),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
                
    ) {
        // Titel + Tabs
        item {
            Text("SpareTime", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(16.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                PillButtonFilled(text = "Dashboard", onClick = onBackToDashboard, modifier = Modifier.weight(1f))
                PillButtonOutlined(text = "Set limits", onClick = { /* already hier */ }, modifier = Modifier.weight(1f))
            }
            Spacer(Modifier.height(16.dp))
            Text("Limits", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        }

        // Paket-Liste (jede Zeile als Lazy-Item -> gesamte Seite scrollt)
        if (packages.isEmpty()) {
            item {
                Surface(
                    shape = MaterialTheme.shapes.medium,
                    color  = MaterialTheme.colorScheme.surface,
                    tonalElevation = 0.dp,
                    border = ButtonDefaults.outlinedButtonBorder.copy(
                        width = 1.dp,
                        brush = androidx.compose.ui.graphics.SolidColor(BlueOutline.copy(alpha = 0.15f))
                    )
                ) {
                    Text("No Apps saved.", Modifier.padding(12.dp), color = TextSecondary)
                }
            }
        } else {
            items(packages, key = { it }) { p ->
                val rule by repo.ruleFlow(p).collectAsState(initial = Rule(p, null, null, true))
                LimitRow(
                    pkg = p,
                    minutes = rule.minutesLimit,
                    accesses = rule.accessLimit,
                    selected = (p == selectedPkg),
                    onClick = { selectedPkg = p }
                )
            }
        }

        // Editor oder Hinweis – auch als Lazy-Item
        item {
            if (selectedPkg != null) {
                Text("Edit: $selectedPkg", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)

                Surface(
                    shape = MaterialTheme.shapes.large,
                    color  = MaterialTheme.colorScheme.surface,
                    tonalElevation = 0.dp,
                    border = ButtonDefaults.outlinedButtonBorder.copy(
                        width = 1.dp,
                        brush = androidx.compose.ui.graphics.SolidColor(PillStroke)
                    ),
                ) {
                    Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedTextField(
                            value = minTxt, onValueChange = { minTxt = it },
                            label = { Text("Min/Day") }, singleLine = true, modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = accTxt, onValueChange = { accTxt = it },
                            label = { Text("Access/Day") }, singleLine = true, modifier = Modifier.fillMaxWidth()
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Notifications")
                            Spacer(Modifier.width(8.dp))
                            Switch(checked = notif, onCheckedChange = { notif = it })
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp),
                                modifier = Modifier.fillMaxWidth()
                        ) {
                            // Save
                            Button(
                                onClick = {
                                    val p = selectedPkg ?: return@Button
                                    scope.launch {
                                        repo.setRuleAndReevaluate(
                                            pkg = p,
                                            minutesLimit = minTxt.trim().toIntOrNull(),
                                            accessLimit  = accTxt.trim().toIntOrNull(),
                                            notifications = notif
                                        )
                                    }
                                },
                                shape = MaterialTheme.shapes.extraLarge,
                                colors = ButtonDefaults.buttonColors(containerColor = PillBg, contentColor = TextPrimary),
                                border = ButtonDefaults.outlinedButtonBorder.copy(
                                    width = 1.dp,
                                    brush = androidx.compose.ui.graphics.SolidColor(PillStroke)
                                )
                            ) { Text("Save", fontWeight = FontWeight.Bold) }

                            // Delete (rot)
                            Button(
                                onClick = { showDeleteConfirm = true },
                                shape = MaterialTheme.shapes.extraLarge,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.error,
                                    contentColor   = MaterialTheme.colorScheme.onError
                                )
                            ) { Text("Delete") }

                            Spacer(Modifier.weight(1f))

                            TextButton(onClick = { selectedPkg = null }) { Text("Close") }
                        }
                    }
                }
            } else {
                Surface(
                    shape = MaterialTheme.shapes.medium,
                    color  = MaterialTheme.colorScheme.surface,
                    tonalElevation = 0.dp,
                    border = ButtonDefaults.outlinedButtonBorder.copy(
                        width = 1.dp,
                        brush = androidx.compose.ui.graphics.SolidColor(BlueOutline.copy(alpha = 0.15f))
                    )
                ) {
                    Text("Choose App to change settings.", Modifier.padding(12.dp), color = TextSecondary)
                }
            }
        }
    }



            // ---- “New” bottom sheet: list all editable packages (from repo) ----
            if (showPicker) {
                ModalBottomSheet(
                    onDismissRequest = { showPicker = false },
                    sheetState = sheetState
                ) {
                    Text("All Apps", Modifier.padding(16.dp), style = MaterialTheme.typography.titleMedium)

                    if (pickerApps.isEmpty()) {
                        // Einfacher Composable-Zweig (kein Lazy-Scope nötig)
                        Box(Modifier.fillMaxWidth().padding(16.dp)) {
                            Text("All installed Apps selected.")
                        }
                    } else {
                        // Jetzt im Lazy-Scope NUR items{} verwenden
                        LazyColumn(
                            modifier = Modifier.fillMaxWidth(),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(pickerApps, key = { it.packageName }) { app ->
                                ListItem(
                                    headlineContent   = { Text(app.label) },
                                    supportingContent = { Text(app.packageName) },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            selectedPkg = app.packageName   // Auswahl setzen
                                            showPicker = false
                                        }
                                )
                                Divider()
                            }
                        }
                    }
                }
            }
        }
            
        
    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete App?") },
            text  = { Text(selectedPkg ?: "") },
            confirmButton = {
                TextButton(onClick = {
                    val p = selectedPkg
                    showDeleteConfirm = false
                    if (p != null) {
                        scope.launch {
                            repo.deletePackage(p)      // ← entfernt die Regel + Paket aus dem Index
                            selectedPkg = null         // Editor schließen
                            minTxt = ""; accTxt = ""; notif = true
                        }
                    }
                }) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) { Text("Cancel") }
            }
        )
        
    }

}



/* -------------------- Row for a single package in list -------------------- */

@Composable
private fun LimitRow(
    pkg: String,
    minutes: Int?,
    accesses: Int?,
    selected: Boolean,
    onClick: () -> Unit
) {
    val label = pkg.substringAfterLast('.').replaceFirstChar { it.uppercaseChar() }
    Surface(
        shape = MaterialTheme.shapes.large,
        color  = MaterialTheme.colorScheme.surface,
        tonalElevation = 0.dp,
        border = ButtonDefaults.outlinedButtonBorder.copy(
            width = 1.dp,
            brush = androidx.compose.ui.graphics.SolidColor(if (selected) BlueOutline else PillStroke)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // little avatar
            Surface(
                shape = MaterialTheme.shapes.extraLarge,
                color = PillBg,
                modifier = Modifier.size(28.dp)
            ) { Box(Modifier.fillMaxSize()) }

            Spacer(Modifier.width(12.dp))

            Column(Modifier.weight(1f)) {
                Text(label, fontWeight = FontWeight.SemiBold)
                Text(pkg, color = TextSecondary, style = MaterialTheme.typography.bodySmall)
            }

            // show current limits (read only)
            AssistChip(text = minutes?.toString() ?: "–", label = "Min/Day")
            Spacer(Modifier.width(8.dp))
            AssistChip(text = accesses?.toString() ?: "–", label = "Access/Day")
        }
    }
}

@Composable
private fun AssistChip(text: String, label: String) {
    Surface(
        shape = MaterialTheme.shapes.extraLarge,
        color  = MaterialTheme.colorScheme.surface,
        border = ButtonDefaults.outlinedButtonBorder.copy(
            width = 1.dp,
            brush = androidx.compose.ui.graphics.SolidColor(BlueOutline)
        )
    ) {
        Row(Modifier.padding(horizontal = 10.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(text)
        }
    }
}

/* -------------------- Pills (same style as Dashboard) -------------------- */

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



data class AppEntry(val label: String, val packageName: String)

private suspend fun loadLaunchableApps(
    ctx: Context,
    includeSystem: Boolean = true
): List<AppEntry> = withContext(Dispatchers.IO) {
    val pm = ctx.packageManager
    val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
    val resolve = if (Build.VERSION.SDK_INT >= 33) {
        pm.queryIntentActivities(intent, PackageManager.ResolveInfoFlags.of(0))
    } else {
        @Suppress("DEPRECATION")
        pm.queryIntentActivities(intent, 0)
    }

    resolve.mapNotNull { ri ->
        val ai = ri.activityInfo?.applicationInfo ?: return@mapNotNull null
        val isSystem = (ai.flags and ApplicationInfo.FLAG_SYSTEM) != 0
        if (!includeSystem && isSystem) return@mapNotNull null

        AppEntry(
            label = ri.loadLabel(pm)?.toString().orEmpty().ifBlank { ai.packageName },
            packageName = ri.activityInfo.packageName
        )
    }
    .distinctBy { it.packageName }
    .sortedBy { it.label.lowercase() }
}
