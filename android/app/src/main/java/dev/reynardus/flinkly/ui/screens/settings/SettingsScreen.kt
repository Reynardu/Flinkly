package dev.reynardus.flinkly.ui.screens.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import dev.reynardus.flinkly.data.remote.dto.HouseholdPauseDto

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onLogout: () -> Unit,
    vm: SettingsViewModel = hiltViewModel(),
) {
    val user by vm.user.collectAsState()
    val household by vm.household.collectAsState()
    val pauses by vm.pauses.collectAsState()
    val clipboard = LocalClipboardManager.current
    var showLogoutDialog by remember { mutableStateOf(false) }

    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = { Text("Abmelden?") },
            text = { Text("Du wirst aus deinem Konto abgemeldet.") },
            confirmButton = {
                Button(
                    onClick = { vm.logout(onLogout) },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                ) { Text("Abmelden") }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) { Text("Abbrechen") }
            },
        )
    }

    if (vm.showPauseDialog) {
        PauseDialog(vm = vm)
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Einstellungen") }) },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            user?.let { u ->
                item {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Profil", style = MaterialTheme.typography.titleMedium)
                            Spacer(Modifier.height(8.dp))
                            InfoRow("Name", u.displayName)
                            InfoRow("Punkte gesamt", "${u.totalPoints}")
                            InfoRow("Längster Streak", "${u.longestStreak} Tage")
                        }
                    }
                }
            }

            vm.userSecret?.let { secret ->
                item {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Login-Code", style = MaterialTheme.typography.titleMedium)
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = "Bewahre deinen Login-Code sicher auf.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Spacer(Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Surface(
                                    color = MaterialTheme.colorScheme.surfaceVariant,
                                    shape = MaterialTheme.shapes.small,
                                    modifier = Modifier.weight(1f),
                                ) {
                                    Text(
                                        text = if (vm.showSecret) secret else "•".repeat(secret.length.coerceAtMost(24)),
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                        style = MaterialTheme.typography.bodyMedium,
                                    )
                                }
                                IconButton(onClick = vm::toggleSecretVisibility) {
                                    Icon(
                                        imageVector = if (vm.showSecret) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                        contentDescription = null,
                                    )
                                }
                                IconButton(onClick = { clipboard.setText(AnnotatedString(secret)) }) {
                                    Icon(Icons.Default.ContentCopy, contentDescription = "Kopieren")
                                }
                            }
                        }
                    }
                }
            }

            household?.let { h ->
                item {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Haushalt", style = MaterialTheme.typography.titleMedium)
                            Spacer(Modifier.height(8.dp))
                            InfoRow("Name", h.name)
                            InfoRow("Mitglieder", "${h.members.size}")
                            Spacer(Modifier.height(12.dp))

                            vm.inviteLink?.let { link ->
                                Surface(
                                    color = MaterialTheme.colorScheme.surfaceVariant,
                                    shape = MaterialTheme.shapes.small,
                                    modifier = Modifier.fillMaxWidth(),
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        Text(
                                            text = link,
                                            style = MaterialTheme.typography.bodySmall,
                                            modifier = Modifier.weight(1f),
                                        )
                                        IconButton(
                                            onClick = { clipboard.setText(AnnotatedString(link)) },
                                            modifier = Modifier.size(32.dp),
                                        ) {
                                            Icon(Icons.Default.ContentCopy, contentDescription = "Kopieren", modifier = Modifier.size(18.dp))
                                        }
                                    }
                                }
                                Spacer(Modifier.height(8.dp))
                            }

                            OutlinedButton(
                                onClick = vm::generateInviteLink,
                                enabled = !vm.isGeneratingLink,
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                if (vm.isGeneratingLink) {
                                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                                } else {
                                    Text("Einladungslink erstellen")
                                }
                            }
                        }
                    }
                }
            }

            // Haushaltspausen
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column {
                                Text("Haushaltspausen", style = MaterialTheme.typography.titleMedium)
                                Text(
                                    text = "Urlaub, Feiertage oder besondere Tage",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            IconButton(onClick = vm::openPauseDialog) {
                                Icon(Icons.Default.Add, contentDescription = "Pause hinzufügen", tint = MaterialTheme.colorScheme.primary)
                            }
                        }

                        if (pauses.isEmpty()) {
                            Spacer(Modifier.height(8.dp))
                            Text(
                                text = "Keine Pausen geplant",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }

            items(pauses, key = { it.id }) { pause ->
                PauseCard(pause = pause, onDelete = { vm.deletePause(pause.id) })
            }

            item {
                Button(
                    onClick = { showLogoutDialog = true },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Abmelden")
                }
            }
        }
    }
}

@Composable
private fun PauseCard(pause: HouseholdPauseDto, onDelete: () -> Unit) {
    var showDeleteDialog by remember { mutableStateOf(false) }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Pause löschen?") },
            text = { Text("${pause.startDate} – ${pause.endDate}") },
            confirmButton = {
                TextButton(onClick = { showDeleteDialog = false; onDelete() }) {
                    Text("Löschen", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("Abbrechen") }
            },
        )
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("🏖", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.size(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "${pause.startDate} – ${pause.endDate}",
                    style = MaterialTheme.typography.bodyMedium,
                )
                pause.reason?.let {
                    Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            IconButton(onClick = { showDeleteDialog = true }) {
                Icon(Icons.Default.Delete, contentDescription = "Löschen", tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun PauseDialog(vm: SettingsViewModel) {
    AlertDialog(
        onDismissRequest = vm::dismissPauseDialog,
        title = { Text("Haushaltspause planen") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "Während einer Pause wird der Streak nicht unterbrochen. Aufgaben können freiwillig erledigt werden.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                OutlinedTextField(
                    value = vm.pauseStartDate,
                    onValueChange = vm::onStartDateChange,
                    label = { Text("Startdatum (YYYY-MM-DD)") },
                    placeholder = { Text("2026-07-01") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = vm.pauseEndDate,
                    onValueChange = vm::onEndDateChange,
                    label = { Text("Enddatum (YYYY-MM-DD)") },
                    placeholder = { Text("2026-07-14") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = vm.pauseReason,
                    onValueChange = vm::onReasonChange,
                    label = { Text("Grund (optional)") },
                    placeholder = { Text("Sommerurlaub") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                vm.pauseError?.let {
                    Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = vm::savePause,
                enabled = !vm.isSavingPause && vm.pauseStartDate.isNotBlank() && vm.pauseEndDate.isNotBlank(),
            ) {
                if (vm.isSavingPause) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
                } else {
                    Text("Speichern")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = vm::dismissPauseDialog) { Text("Abbrechen") }
        },
    )
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyMedium)
    }
}
