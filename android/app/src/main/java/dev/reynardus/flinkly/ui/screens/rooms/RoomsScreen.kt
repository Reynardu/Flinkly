package dev.reynardus.flinkly.ui.screens.rooms

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import dev.reynardus.flinkly.data.local.entities.RoomEntity

private val iconOptions = listOf("🏠", "🍳", "🛁", "🛏", "🚗", "🌿", "📦", "🏢", "🧹", "🪴")
private val colorOptions = listOf(
    "#4CAF50", "#2196F3", "#FF9800", "#E91E63",
    "#9C27B0", "#F44336", "#009688", "#FF5722",
    "#3F51B5", "#FFC107",
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoomsScreen(
    onRoomClick: (roomId: Int, roomName: String) -> Unit,
    vm: RoomsViewModel = hiltViewModel(),
) {
    val rooms by vm.rooms.collectAsState()

    if (vm.showCreateDialog) {
        CreateRoomDialog(vm = vm)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Räume") },
                actions = {
                    if (vm.isRefreshing) {
                        CircularProgressIndicator(
                            modifier = Modifier
                                .size(24.dp)
                                .padding(end = 4.dp),
                            strokeWidth = 2.dp,
                        )
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
                Icon(Icons.Default.Add, contentDescription = "Raum hinzufügen")
            }
        },
    ) { padding ->
        if (rooms.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Noch keine Räume", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "Tippe auf + um einen Raum hinzuzufügen",
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
                items(rooms, key = { it.id }) { room ->
                    RoomCard(
                        room = room,
                        onClick = { onRoomClick(room.id, room.name) },
                        onDelete = { vm.deleteRoom(room.id) },
                    )
                }
            }
        }
    }
}

@Composable
private fun RoomCard(
    room: RoomEntity,
    onClick: () -> Unit,
    onDelete: () -> Unit,
) {
    var showDeleteDialog by remember { mutableStateOf(false) }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Raum löschen?") },
            text = { Text("\"${room.name}\" und alle zugehörigen Aufgaben werden gelöscht.") },
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
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val color = runCatching { Color(android.graphics.Color.parseColor(room.color)) }
                .getOrDefault(Color(0xFF4CAF50))
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(color.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center,
            ) {
                Text(room.icon, style = MaterialTheme.typography.titleLarge)
            }
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = room.name,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = "${room.openTaskCount} offen · ${room.taskCount} gesamt",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            IconButton(onClick = { showDeleteDialog = true }) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Löschen",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun CreateRoomDialog(vm: RoomsViewModel) {
    AlertDialog(
        onDismissRequest = vm::dismissCreateDialog,
        title = { Text("Neuer Raum") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = vm.newRoomName,
                    onValueChange = vm::onNameChange,
                    label = { Text("Name") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                    modifier = Modifier.fillMaxWidth(),
                )

                Text("Symbol", style = MaterialTheme.typography.labelLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    iconOptions.take(5).forEach { icon ->
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(
                                    if (vm.newRoomIcon == icon)
                                        MaterialTheme.colorScheme.primaryContainer
                                    else
                                        MaterialTheme.colorScheme.surfaceVariant,
                                )
                                .clickable { vm.onIconChange(icon) },
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(icon)
                        }
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    iconOptions.drop(5).forEach { icon ->
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(
                                    if (vm.newRoomIcon == icon)
                                        MaterialTheme.colorScheme.primaryContainer
                                    else
                                        MaterialTheme.colorScheme.surfaceVariant,
                                )
                                .clickable { vm.onIconChange(icon) },
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(icon)
                        }
                    }
                }

                Text("Farbe", style = MaterialTheme.typography.labelLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    colorOptions.take(5).forEach { hex ->
                        val color = runCatching { Color(android.graphics.Color.parseColor(hex)) }
                            .getOrDefault(Color.Gray)
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(color)
                                .clickable { vm.onColorChange(hex) },
                        ) {
                            if (vm.newRoomColor == hex) {
                                Box(
                                    Modifier
                                        .fillMaxSize()
                                        .background(Color.Black.copy(alpha = 0.3f), CircleShape)
                                )
                            }
                        }
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    colorOptions.drop(5).forEach { hex ->
                        val color = runCatching { Color(android.graphics.Color.parseColor(hex)) }
                            .getOrDefault(Color.Gray)
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(color)
                                .clickable { vm.onColorChange(hex) },
                        ) {
                            if (vm.newRoomColor == hex) {
                                Box(
                                    Modifier
                                        .fillMaxSize()
                                        .background(Color.Black.copy(alpha = 0.3f), CircleShape)
                                )
                            }
                        }
                    }
                }

                vm.error?.let {
                    Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = vm::createRoom,
                enabled = !vm.isCreating && vm.newRoomName.isNotBlank(),
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
