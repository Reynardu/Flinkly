package dev.reynardus.flinkly.ui.screens.scoreboard

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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

private val periods = listOf(
    "week" to "Woche",
    "month" to "Monat",
    "all_time" to "Gesamt",
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScoreboardScreen(vm: ScoreboardViewModel = hiltViewModel()) {
    val scoreboard by vm.scoreboard.collectAsState()
    val isLoading by vm.isLoading.collectAsState()
    val currentUserId by vm.currentUserId.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Punkte") },
                actions = {
                    IconButton(onClick = vm::refresh) {
                        Icon(Icons.Default.Refresh, contentDescription = "Aktualisieren")
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
        ) {
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                periods.forEach { (value, label) ->
                    FilterChip(
                        selected = vm.selectedPeriod == value,
                        onClick = { vm.selectPeriod(value) },
                        label = { Text(label) },
                    )
                }
            }
            Spacer(Modifier.height(8.dp))

            if (isLoading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                val entries = scoreboard?.entries ?: emptyList()
                if (entries.isEmpty()) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Noch keine Daten", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(bottom = 16.dp),
                    ) {
                        itemsIndexed(entries) { index, entry ->
                            val isMe = entry.user.id == currentUserId
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = if (isMe)
                                    CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                                    )
                                else
                                    CardDefaults.cardColors(),
                            ) {
                                Row(
                                    modifier = Modifier.padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Text(
                                        text = rankEmoji(index + 1),
                                        style = MaterialTheme.typography.titleLarge,
                                        modifier = Modifier.width(36.dp),
                                    )
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = entry.user.displayName + if (isMe) " (du)" else "",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = if (isMe) FontWeight.Bold else FontWeight.Normal,
                                        )
                                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            Text(
                                                text = "${entry.tasksCompleted} Aufgaben",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            )
                                            if (entry.user.currentStreak > 0) {
                                                Text(
                                                    text = "🔥 ${entry.user.currentStreak}",
                                                    style = MaterialTheme.typography.bodySmall,
                                                )
                                            }
                                        }
                                    }
                                    Text(
                                        text = "${entry.points} Pkt.",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.Bold,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun rankEmoji(rank: Int) = when (rank) {
    1 -> "🥇"
    2 -> "🥈"
    3 -> "🥉"
    else -> "$rank."
}
