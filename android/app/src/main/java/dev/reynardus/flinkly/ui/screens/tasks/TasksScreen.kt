package dev.reynardus.flinkly.ui.screens.tasks

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import dev.reynardus.flinkly.data.local.entities.TaskEntity

private val frequencyOptions = listOf(
    "DAILY" to "Täglich",
    "WEEKLY" to "Wöchentlich",
    "MONTHLY" to "Monatlich",
    "ONE_TIME" to "Einmalig",
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TasksScreen(
    roomId: Int,
    roomName: String,
    onBack: () -> Unit,
    vm: TasksViewModel = hiltViewModel(),
) {
    LaunchedEffect(roomId) { vm.init(roomId) }

    val tasks by vm.tasks.collectAsState()

    if (vm.showCreateDialog) {
        CreateTaskDialog(vm = vm)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(roomName, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Zurück")
                    }
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = vm::openCreateDialog) {
                Icon(Icons.Default.Add, contentDescription = "Aufgabe hinzufügen")
            }
        },
    ) { padding ->
        if (tasks.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Keine Aufgaben", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "Tippe auf + um eine Aufgabe hinzuzufügen",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(tasks, key = { it.id }) { task ->
                    TaskCard(
                        task = task,
                        onComplete = { vm.completeTask(task.id) },
                        onDelete = { vm.deleteTask(task.id) },
                    )
                }
            }
        }
    }
}

@Composable
private fun TaskCard(
    task: TaskEntity,
    onComplete: () -> Unit,
    onDelete: () -> Unit,
) {
    var showDeleteDialog by remember { mutableStateOf(false) }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Aufgabe löschen?") },
            text = { Text("\"${task.title}\" wird dauerhaft gelöscht.") },
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

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = task.title,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    task.description?.let {
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = it,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
                IconButton(onClick = { showDeleteDialog = true }) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Löschen",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    repeat(task.difficulty.coerceIn(1, 3)) {
                        Icon(
                            Icons.Default.Star,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(16.dp),
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "${task.points} Pkt.",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = frequencyLabel(task.frequencyType),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                IconButton(onClick = onComplete) {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = "Erledigt",
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
            }

            task.nextDueAt?.let { due ->
                Text(
                    text = "Fällig: ${due.take(10)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CreateTaskDialog(vm: TasksViewModel) {
    var frequencyExpanded by remember { mutableStateOf(false) }
    val selectedFreqLabel = frequencyOptions.firstOrNull { it.first == vm.newFrequencyType }?.second ?: "Wöchentlich"

    AlertDialog(
        onDismissRequest = vm::dismissCreateDialog,
        title = { Text("Neue Aufgabe") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = vm.newTitle,
                    onValueChange = vm::onTitleChange,
                    label = { Text("Titel") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = vm.newDescription,
                    onValueChange = vm::onDescriptionChange,
                    label = { Text("Beschreibung (optional)") },
                    maxLines = 2,
                    modifier = Modifier.fillMaxWidth(),
                )

                Text("Schwierigkeitsgrad", style = MaterialTheme.typography.labelLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(1 to "Leicht", 2 to "Mittel", 3 to "Schwer").forEach { (lvl, label) ->
                        TextButton(
                            onClick = { vm.onDifficultyChange(lvl) },
                            modifier = Modifier.weight(1f),
                        ) {
                            Text(
                                label,
                                color = if (vm.newDifficulty == lvl)
                                    MaterialTheme.colorScheme.primary
                                else
                                    MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }

                ExposedDropdownMenuBox(
                    expanded = frequencyExpanded,
                    onExpandedChange = { frequencyExpanded = it },
                ) {
                    OutlinedTextField(
                        value = selectedFreqLabel,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Wiederholung") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(frequencyExpanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(),
                    )
                    ExposedDropdownMenu(
                        expanded = frequencyExpanded,
                        onDismissRequest = { frequencyExpanded = false },
                    ) {
                        frequencyOptions.forEach { (value, label) ->
                            DropdownMenuItem(
                                text = { Text(label) },
                                onClick = { vm.onFrequencyChange(value); frequencyExpanded = false },
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("Automatisch wiederholen", style = MaterialTheme.typography.bodyMedium)
                    Switch(checked = vm.newAutoRepeat, onCheckedChange = vm::onAutoRepeatChange)
                }

                vm.error?.let {
                    Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = vm::createTask,
                enabled = !vm.isCreating && vm.newTitle.isNotBlank(),
            ) {
                if (vm.isCreating) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary,
                    )
                } else {
                    Text("Erstellen")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = vm::dismissCreateDialog) { Text("Abbrechen") }
        },
    )
}

private fun frequencyLabel(type: String) = when (type) {
    "DAILY" -> "täglich"
    "WEEKLY" -> "wöchentlich"
    "MONTHLY" -> "monatlich"
    "ONE_TIME" -> "einmalig"
    else -> type
}
