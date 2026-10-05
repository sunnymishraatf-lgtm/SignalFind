package com.signalfind.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.signalfind.data.db.RecoveryEvent
import com.signalfind.ui.AppScaffold
import com.signalfind.ui.viewmodel.HistoryViewModel
import com.signalfind.util.TimeFormat
import java.util.Locale

@Composable
fun HistoryScreen(
    vm: HistoryViewModel,
    onBack: () -> Unit
) {
    val events by vm.events.collectAsState()
    var confirmClear by remember { mutableStateOf(false) }

    AppScaffold(
        title = "Recovery history",
        onBack = onBack,
        actions = {
            if (events.isNotEmpty()) {
                IconButton(onClick = { confirmClear = true }) {
                    Icon(Icons.Filled.Delete, contentDescription = "Clear all")
                }
            }
        }
    ) { pad ->
        if (events.isEmpty()) {
            Column(
                modifier = Modifier
                    .padding(pad)
                    .padding(32.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("No recovery commands yet.", style = MaterialTheme.typography.bodyLarge)
                Spacer(Modifier.height(8.dp))
                Text(
                    "Handled and rejected commands will appear here with their reply status.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .padding(pad)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(events, key = { it.id }) { event ->
                    HistoryRow(event = event, onDelete = { vm.delete(event) })
                }
            }
        }
    }

    if (confirmClear) {
        AlertDialog(
            onDismissRequest = { confirmClear = false },
            title = { Text("Clear history?") },
            text = { Text("All locally stored recovery events will be deleted. This cannot be undone.") },
            confirmButton = {
                TextButton(onClick = {
                    vm.clearAll()
                    confirmClear = false
                }) { Text("Clear all") }
            },
            dismissButton = {
                TextButton(onClick = { confirmClear = false }) { Text("Cancel") }
            }
        )
    }
}

private fun replyStatusLabel(status: String): String = when (status) {
    "SENT" -> "Sent"
    "FAILED" -> "Failed"
    "PIN_WRONG" -> "Wrong PIN"
    "MALFORMED" -> "Malformed"
    "PREVIEW" -> "Preview"
    else -> status.lowercase().replaceFirstChar(Char::uppercase)
}

@Composable
private fun HistoryRow(event: RecoveryEvent, onDelete: () -> Unit) {
    val stateColor = when (event.locationState) {
        "LIVE" -> Color(0xFF1B8A5A)
        "CACHED" -> Color(0xFF9A6B00)
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(event.command, fontWeight = FontWeight.Bold)
                    AssistChip(
                        onClick = {},
                        label = { Text(event.locationState) },
                        colors = AssistChipDefaults.assistChipColors(labelColor = stateColor)
                    )
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    "${TimeFormat.dateTime(event.timestamp)} • from ${event.senderMasked}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                val locLine = when {
                    event.latitude != null && event.longitude != null ->
                        // Locale.US: coordinates must always use '.' even on
                        // devices whose locale uses ',' as decimal separator.
                        "%.5f, %.5f".format(
                            Locale.US, event.latitude, event.longitude
                        ) + (event.accuracyM?.let { "  ±${it.toInt()}m" } ?: "")
                    else -> null
                }
                locLine?.let {
                    Text(it, style = MaterialTheme.typography.bodySmall)
                }
                Text(
                    "Reply: ${replyStatusLabel(event.replyStatus)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (event.replyStatus == "SENT")
                        Color(0xFF1B8A5A) else MaterialTheme.colorScheme.error
                )
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Filled.Delete, contentDescription = "Delete")
            }
        }
    }
}
