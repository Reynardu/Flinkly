package dev.reynardus.flinkly.ui.screens.tasks

import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.foundation.text.KeyboardOptions
import dev.reynardus.flinkly.data.local.entities.TaskEntity
import dev.reynardus.flinkly.data.remote.dto.CompletionDto

private val frequencyOptions = listOf(
    "DAILY" to "Täglich",
    "WEEKLY" to "Wöchentlich",
    "MONTHLY" to "Monatlich",
    "ONCE" to "Einmalig",
)

private val difficultyOptions = listOf(
    1 to "~5 Min.",
    2 to "~15 Min.",
    3 to "~30 Min.",
    4 to "~45 Min.",
    5 to "~60 Min.",
)

private fun difficultyToTime(difficulty: Int): String =
    difficultyOptions.firstOrNull { it.first == difficulty }?.second ?: "~15 Min."

private fun TaskEntity.isOpen(): Boolean {
    val nextDue = nextDueAt ?: return true
    return try {
        java.time.OffsetDateTime.parse(nextDue).toInstant().isBefore(java.time.Instant.now())
    } catch (_: Exception) {
        true
    }
}

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
    val completions by vm.completions.collectAsState()

    val openTasks = remember(tasks) { tasks.filter { it.isOpen() } }
    val doneTasks = remember(tasks) { tasks.filter { !it.isOpen() } }

    val recentCompletions = remember(completions, tasks) {
        completions
            .flatMap { (taskId, list) ->
                val title = tasks.find { it.id == taskId }?.title ?: "Aufgabe"
                list.map { Triple(title, it.user.displayName, it) }
            }
            .sortedByDescending { it.third.completedAt }
            .take(30)
    }

    var openSectionExpanded by rememberSaveable { mutableStateOf(true) }
    var doneSectionExpanded by rememberSaveable { mutableStateOf(false) }
    var historySectionExpanded by rememberSaveable { mutableStateOf(false) }

    // Warnung: Aufgabe zu früh erledigen
    vm.pendingEarlyCompleteTaskId?.let {
        AlertDialog(
            onDismissRequest = vm::dismissEarlyCompletion,
            title = { Text("Aufgabe bereits erledigt") },
            text = {
                Text(
                    "Diese Aufgabe ist erst ab ${vm.pendingEarlyCompleteDueDate ?: "einem späteren Datum"} " +
                        "wieder fällig. Möchtest du sie trotzdem jetzt als erledigt markieren?"
                )
            },
            confirmButton = {
                Button(onClick = vm::confirmEarlyCompletion) { Text("Trotzdem erledigen") }
            },
            dismissButton = {
                TextButton(onClick = vm::dismissEarlyCompletion) { Text("Abbrechen") }
            },
        )
    }

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
                actions = {
                    if (vm.isRefreshing) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                    } else {
                        IconButton(onClick = vm::refresh) {
                            Icon(Icons.Default.Refresh, contentDescription = "Aktualisieren")
                        }
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
                contentPadding = PaddingValues(bottom = 88.dp),
            ) {
                // ── Offene Aufgaben ──
                item {
                    SectionHeader(
                        title = "Offen",
                        count = openTasks.size,
                        expanded = openSectionExpanded,
                        onClick = { openSectionExpanded = !openSectionExpanded },
                    )
                }
                if (openSectionExpanded) {
                    if (openTasks.isEmpty()) {
                        item {
                            Text(
                                "Alle Aufgaben erledigt! 🎉",
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    } else {
                        items(openTasks, key = { it.id }) { task ->
                            TaskCard(
                                task = task,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                                onComplete = { vm.completeTask(task.id) },
                                onDelete = { vm.deleteTask(task.id) },
                            )
                        }
                    }
                }

                // ── Erledigte Aufgaben ──
                item {
                    SectionHeader(
                        title = "Erledigt",
                        count = doneTasks.size,
                        expanded = doneSectionExpanded,
                        onClick = { doneSectionExpanded = !doneSectionExpanded },
                    )
                }
                if (doneSectionExpanded) {
                    items(doneTasks, key = { "done_${it.id}" }) { task ->
                        TaskCard(
                            task = task,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                            onComplete = { vm.completeTask(task.id) },
                            onDelete = { vm.deleteTask(task.id) },
                            dimmed = true,
                        )
                    }
                }

                // ── Verlauf ──
                if (recentCompletions.isNotEmpty()) {
                    item {
                        SectionHeader(
                            title = "Verlauf",
                            count = recentCompletions.size,
                            expanded = historySectionExpanded,
                            onClick = { historySectionExpanded = !historySectionExpanded },
                            icon = { Icon(Icons.Default.History, contentDescription = null, modifier = Modifier.size(18.dp)) },
                        )
                    }
                    if (historySectionExpanded) {
                        items(recentCompletions, key = { (_, _, c) -> "hist_${c.id}" }) { (taskTitle, userName, completion) ->
                            CompletionHistoryItem(
                                taskTitle = taskTitle,
                                userName = userName,
                                completedAt = completion.completedAt,
                                points = completion.pointsEarned,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(
    title: String,
    count: Int,
    expanded: Boolean,
    onClick: () -> Unit,
    icon: (@Composable () -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                icon?.invoke()
                Text(
                    text = "$title ($count)",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Icon(
                if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                contentDescription = if (expanded) "Einklappen" else "Aufklappen",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

@Composable
private fun TaskCard(
    task: TaskEntity,
    modifier: Modifier = Modifier,
    onComplete: () -> Unit,
    onDelete: () -> Unit,
    dimmed: Boolean = false,
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

    val alpha = if (dimmed) 0.6f else 1f

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = alpha),
        ),
    ) {
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
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = alpha),
                    )
                    task.description?.let {
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = it,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = alpha),
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
                    Text(
                        text = difficultyToTime(task.difficulty),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("·", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "${task.points} Pkt.",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.secondary,
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("·", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = frequencyLabel(task.frequencyType),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (!dimmed) {
                    IconButton(onClick = onComplete) {
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = "Erledigt",
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
            }

            task.nextDueAt?.let { due ->
                val label = if (dimmed) "Wieder fällig ab: ${due.take(10)}" else "Fällig: ${due.take(10)}"
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = alpha),
                )
            }
        }
    }
}

@Composable
private fun CompletionHistoryItem(
    taskTitle: String,
    userName: String,
    completedAt: String,
    points: Int,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = taskTitle,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = "$userName · ${completedAt.take(10)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Text(
            text = "+$points Pkt.",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary,
        )
    }
    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), thickness = 0.5.dp)
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
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = vm.newDescription,
                    onValueChange = vm::onDescriptionChange,
                    label = { Text("Beschreibung (optional)") },
                    maxLines = 2,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                    modifier = Modifier.fillMaxWidth(),
                )

                Text("Geschätzte Zeit", style = MaterialTheme.typography.labelLarge)
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        difficultyOptions.take(3).forEach { (lvl, label) ->
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
                                    style = MaterialTheme.typography.labelMedium,
                                )
                            }
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        difficultyOptions.drop(3).forEach { (lvl, label) ->
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
                                    style = MaterialTheme.typography.labelMedium,
                                )
                            }
                        }
                        // Leerer Platzhalter für gleichmäßiges Layout
                        Spacer(Modifier.weight(1f))
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
    "ONCE" -> "einmalig"
    else -> type
}
