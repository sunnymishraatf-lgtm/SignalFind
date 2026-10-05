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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.signalfind.data.db.TrustedNumber
import com.signalfind.ui.AppScaffold
import com.signalfind.ui.SectionHeader
import com.signalfind.ui.WideButton
import com.signalfind.ui.viewmodel.TrustedNumbersViewModel
import com.signalfind.util.TimeFormat

@Composable
fun TrustedNumbersScreen(
    vm: TrustedNumbersViewModel,
    onBack: () -> Unit
) {
    val numbers by vm.numbers.collectAsState()
    val trustedOnly by vm.trustedOnly.collectAsState()
    val error by vm.error.collectAsState()
    var showAdd by remember { mutableStateOf(false) }

    AppScaffold(
        title = "Trusted numbers",
        onBack = onBack,
        actions = {},
        content = { pad ->
            Column(
                modifier = Modifier
                    .padding(pad)
                    .padding(16.dp)
            ) {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .padding(16.dp)
                            .fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text("Only accept trusted numbers", fontWeight = FontWeight.SemiBold)
                            Text(
                                "Recommended. Commands from unknown senders are ignored.",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                        Switch(checked = trustedOnly, onCheckedChange = vm::setTrustedOnly)
                    }
                }
                Spacer(Modifier.height(12.dp))
                SectionHeader("Recovery numbers (${numbers.size})")
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(numbers, key = { it.id }) { entry ->
                        TrustedRow(entry = entry, onDelete = { vm.remove(entry) })
                    }
                }
                Spacer(Modifier.height(8.dp))
                WideButton("Add number", onClick = { showAdd = true }, icon = Icons.Filled.Add)
                error?.let {
                    Spacer(Modifier.height(8.dp))
                    Text(it, color = MaterialTheme.colorScheme.error)
                }
            }
        }
    )

    if (showAdd) {
        AddNumberDialog(
            onDismiss = { showAdd = false; vm.clearError() },
            onAdd = { number, label ->
                vm.add(number, label)
                showAdd = false
            }
        )
    }
}

@Composable
private fun TrustedRow(entry: TrustedNumber, onDelete: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(entry.label, fontWeight = FontWeight.SemiBold)
                Text(entry.number, style = MaterialTheme.typography.bodyMedium)
                Text(
                    "Added ${TimeFormat.dateTime(entry.addedAt)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Filled.Delete, contentDescription = "Remove")
            }
        }
    }
}

@Composable
private fun AddNumberDialog(onDismiss: () -> Unit, onAdd: (String, String) -> Unit) {
    var number by remember { mutableStateOf("") }
    var label by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add trusted number") },
        text = {
            Column {
                OutlinedTextField(
                    value = number,
                    onValueChange = { number = it },
                    label = { Text("Phone number") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = label,
                    onValueChange = { label = it },
                    label = { Text("Label") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onAdd(number, label.ifBlank { "Recovery phone" }) }) {
                Text("Add")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
