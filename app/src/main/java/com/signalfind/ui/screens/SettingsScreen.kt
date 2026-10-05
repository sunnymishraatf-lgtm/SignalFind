package com.signalfind.ui.screens

import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.signalfind.data.SettingsRepository
import com.signalfind.system.PermissionHelper
import com.signalfind.ui.AppScaffold
import com.signalfind.ui.SectionHeader
import com.signalfind.ui.WideButton
import com.signalfind.ui.viewmodel.SettingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    vm: SettingsViewModel,
    onBack: () -> Unit
) {
    val state by vm.state.collectAsState()
    val subscriptions by vm.subscriptions.collectAsState()
    val message by vm.message.collectAsState()
    val context = LocalContext.current

    var showPinDialog by remember { mutableStateOf(false) }
    var keywordDraft by remember(state.keyword) { mutableStateOf(state.keyword) }
    var simExpanded by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) { vm.refresh() }

    AppScaffold(title = "Settings", onBack = onBack) { pad ->
        Column(
            modifier = Modifier
                .padding(pad)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            SectionHeader("Recovery")
            SettingSwitch(
                title = "Lost Mode (SMS recovery)",
                subtitle = "Answer authenticated SMS commands",
                checked = state.recoveryEnabled,
                onChecked = vm::setRecoveryEnabled
            )
            SettingSwitch(
                title = "Only trusted numbers",
                subtitle = "Ignore commands from unknown senders",
                checked = state.trustedOnly,
                onChecked = vm::setTrustedOnly
            )

            SectionHeader("Security")
            WideButton(
                if (state.hasPin) "Change recovery PIN" else "Set recovery PIN",
                onClick = { showPinDialog = true }
            )
            OutlinedTextField(
                value = keywordDraft,
                onValueChange = { keywordDraft = it.uppercase().filter(Char::isLetterOrDigit).take(16) },
                label = { Text("Emergency keyword (alias for LOCATION)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            WideButton("Save keyword", onClick = { vm.setKeyword(keywordDraft) })

            SectionHeader("Reply SIM (dual SIM)")
            if (!state.simPermission) {
                Text(
                    "Grant Phone permission to choose which SIM sends replies. " +
                        "Without it, the system default SIM is used.",
                    style = MaterialTheme.typography.bodyMedium
                )
                WideButton("Open app settings", onClick = {
                    val intent = Intent(
                        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                        android.net.Uri.fromParts("package", context.packageName, null)
                    ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(intent)
                })
            } else {
                ExposedDropdownMenuBox(
                    expanded = simExpanded,
                    onExpandedChange = { simExpanded = !simExpanded }
                ) {
                    val current = subscriptions.firstOrNull { it.subscriptionId == state.preferredSubId }
                    OutlinedTextField(
                        value = current?.let { "${it.displayName} (${it.carrierName})" }
                            ?: "System default",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("SIM for replies") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(simExpanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                    )
                    ExposedDropdownMenu(expanded = simExpanded, onDismissRequest = { simExpanded = false }) {
                        DropdownMenuItem(
                            text = { Text("System default") },
                            onClick = {
                                vm.setPreferredSubId(SettingsRepository.NO_SUB_ID)
                                simExpanded = false
                            }
                        )
                        subscriptions.forEach { sub ->
                            DropdownMenuItem(
                                text = { Text("${sub.displayName} (${sub.carrierName})") },
                                onClick = {
                                    vm.setPreferredSubId(sub.subscriptionId)
                                    simExpanded = false
                                }
                            )
                        }
                    }
                }
            }

            SectionHeader("Battery")
            Text(
                if (PermissionHelper.isIgnoringBatteryOptimizations(context))
                    "Battery optimization is off for SignalFind — best reliability."
                else
                    "Battery optimization is on. Exempt SignalFind so Android does " +
                        "not delay the SMS receiver on some devices.",
                style = MaterialTheme.typography.bodyMedium
            )
            if (Build.VERSION.SDK_INT >= 23) {
                WideButton("Battery optimization settings", onClick = {
                    context.startActivity(
                        Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    )
                })
            }

            SectionHeader("About")
            Text(
                "SignalFind 1.0.0 — offline SMS phone recovery.\n" +
                    "Core recovery works without internet, accounts, or cloud services. " +
                    "Location history never leaves this device.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            message?.let {
                Text(it, color = MaterialTheme.colorScheme.primary)
            }
        }
    }

    if (showPinDialog) {
        PinChangeDialog(
            onDismiss = { showPinDialog = false; vm.clearMessage() },
            onSave = { pin ->
                vm.changePin(pin)
                showPinDialog = false
            }
        )
    }
}

@Composable
private fun SettingSwitch(
    title: String,
    subtitle: String,
    checked: Boolean,
    onChecked: (Boolean) -> Unit
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
                Text(title, fontWeight = FontWeight.SemiBold)
                Text(subtitle, style = MaterialTheme.typography.bodySmall)
            }
            Switch(checked = checked, onCheckedChange = onChecked)
        }
    }
}

@Composable
private fun PinChangeDialog(onDismiss: () -> Unit, onSave: (String) -> Unit) {
    var pin by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Change recovery PIN") },
        text = {
            Column {
                OutlinedTextField(
                    value = pin,
                    onValueChange = { if (it.length <= 8 && it.all(Char::isDigit)) pin = it },
                    label = { Text("New PIN (4–8 digits)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = confirm,
                    onValueChange = { if (it.length <= 8 && it.all(Char::isDigit)) confirm = it },
                    label = { Text("Confirm PIN") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth()
                )
                error?.let {
                    Spacer(Modifier.height(8.dp))
                    Text(it, color = MaterialTheme.colorScheme.error)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                error = when {
                    pin.length < 4 -> "PIN must be 4–8 digits."
                    pin != confirm -> "PINs do not match."
                    else -> null
                }
                if (error == null) onSave(pin)
            }) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
