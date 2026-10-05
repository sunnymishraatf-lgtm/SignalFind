package com.signalfind.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.signalfind.ui.SectionHeader
import com.signalfind.ui.StatusRow
import com.signalfind.ui.WideButton
import com.signalfind.ui.nav.Routes
import com.signalfind.ui.viewmodel.DashboardViewModel
import com.signalfind.util.TimeFormat

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    vm: DashboardViewModel,
    onNavigate: (String) -> Unit
) {
    val state by vm.state.collectAsState()
    val lastEvent by vm.lastEvent.collectAsState()
    LaunchedEffect(Unit) { vm.refresh() }

    Scaffold(
        topBar = { TopAppBar(title = { Text("SignalFind", fontWeight = FontWeight.Bold) }) }
    ) { pad ->
        LazyColumn(
            modifier = Modifier
                .padding(pad)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = if (state.recoveryEnabled)
                            MaterialTheme.colorScheme.primaryContainer
                        else
                            MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = if (state.recoveryEnabled)
                            MaterialTheme.colorScheme.onPrimaryContainer
                        else
                            MaterialTheme.colorScheme.onSurfaceVariant
                    )
                ) {
                    Column(Modifier.padding(20.dp)) {
                        Text(
                            if (state.recoveryEnabled) "OFFLINE RECOVERY READY"
                            else "RECOVERY OFF",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "Your phone can respond to authenticated recovery SMS " +
                                "commands when cellular SMS service is available.",
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Lost Mode", fontWeight = FontWeight.SemiBold)
                            Switch(
                                checked = state.recoveryEnabled,
                                onCheckedChange = vm::setRecoveryEnabled
                            )
                        }
                    }
                }
            }

            item { SectionHeader("Device status") }
            item {
                StatusRow(
                    "Finder status",
                    state.recoveryEnabled,
                    if (state.recoveryEnabled) "Active" else "Off"
                )
            }
            item {
                StatusRow(
                    "Location permission",
                    state.fineLocationGranted && state.backgroundLocationGranted,
                    if (state.fineLocationGranted && state.backgroundLocationGranted) "Enabled"
                    else "Missing"
                )
            }
            item {
                StatusRow(
                    "SMS recovery",
                    state.smsGranted,
                    if (state.smsGranted) "Enabled" else "Missing"
                )
            }
            item {
                StatusRow(
                    "Trusted numbers", null,
                    "${state.trustedCount} number${if (state.trustedCount == 1) "" else "s"}"
                )
            }
            item {
                StatusRow(
                    "Battery", null,
                    if (state.batteryPct >= 0) "${state.batteryPct}%" else "—"
                )
            }
            item {
                val e = lastEvent
                StatusRow(
                    "Last command", null,
                    if (e == null) "No commands yet"
                    else "${e.command} · ${e.locationState} · ${TimeFormat.time(e.timestamp)}"
                )
            }

            item { SectionHeader("Actions") }
            item {
                WideButton(
                    "Test recovery", { onNavigate(Routes.TEST) },
                    icon = Icons.AutoMirrored.Filled.Send
                )
            }
            item {
                WideButton(
                    "Trusted numbers", { onNavigate(Routes.TRUSTED) },
                    icon = Icons.Filled.Person
                )
            }
            item {
                WideButton(
                    "Recovery history", { onNavigate(Routes.HISTORY) },
                    icon = Icons.AutoMirrored.Filled.List
                )
            }
            item {
                WideButton(
                    "Diagnostics", { onNavigate(Routes.DIAGNOSTICS) },
                    icon = Icons.Filled.Build
                )
            }
            item {
                WideButton(
                    "Offline map", { onNavigate(Routes.OFFLINE_MAP) },
                    icon = Icons.Filled.Place
                )
            }
            item {
                WideButton(
                    "Settings", { onNavigate(Routes.SETTINGS) },
                    icon = Icons.Filled.Settings
                )
            }
        }
    }
}
