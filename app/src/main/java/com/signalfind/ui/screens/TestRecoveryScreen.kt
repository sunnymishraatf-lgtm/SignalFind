package com.signalfind.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.signalfind.ui.AppScaffold
import com.signalfind.ui.SectionHeader
import com.signalfind.ui.WideButton
import com.signalfind.ui.viewmodel.TestViewModel
import com.signalfind.util.PhoneNumberUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TestRecoveryScreen(
    vm: TestViewModel,
    onBack: () -> Unit
) {
    AppScaffold(title = "Test recovery", onBack = onBack) { pad ->
        Column(
            modifier = Modifier
                .padding(pad)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            TestRecoveryContent(vm = vm)
        }
    }
}

/** Shared by the standalone screen and the setup wizard. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TestRecoveryContent(vm: TestViewModel) {
    val preview by vm.preview.collectAsState()
    val busy by vm.busy.collectAsState()
    val result by vm.result.collectAsState()
    val trusted by vm.trustedNumbers.collectAsState()

    var command by remember { mutableStateOf("SIGNALFIND  LOCATION") }
    var targetExpanded by remember { mutableStateOf(false) }
    var target by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(trusted) {
        if (target == null && trusted.isNotEmpty()) target = trusted.first().number
    }

    SectionHeader("1 · Preview a command")
    Text(
        "Type the command exactly as it would arrive by SMS. Preview runs the " +
            "real parser and reply builder — nothing is sent.",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    OutlinedTextField(
        value = command,
        onValueChange = { command = it },
        label = { Text("Command, e.g. SIGNALFIND 4829 LOCATION") },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true
    )
    WideButton("Preview reply", onClick = { vm.preview(command) }, enabled = !busy)

    preview?.let {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Text(
                it,
                modifier = Modifier.padding(12.dp),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Normal
            )
        }
    }

    SectionHeader("2 · Send a real test SMS")
    Text(
        "Sends a genuine LOCATION reply to one of your trusted numbers. " +
            "Use this to verify the end-to-end path, including with mobile " +
            "data turned off.",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    ExposedDropdownMenuBox(
        expanded = targetExpanded,
        onExpandedChange = { targetExpanded = !targetExpanded }
    ) {
        OutlinedTextField(
            value = target?.let { "$it" } ?: "No trusted numbers",
            onValueChange = {},
            readOnly = true,
            label = { Text("Send to") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(targetExpanded) },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor()
        )
        ExposedDropdownMenu(
            expanded = targetExpanded,
            onDismissRequest = { targetExpanded = false }
        ) {
            trusted.forEach { entry ->
                DropdownMenuItem(
                    text = { Text("${entry.label}: ${entry.number}") },
                    onClick = {
                        target = entry.number
                        targetExpanded = false
                    }
                )
            }
        }
    }
    WideButton(
        "Send test SMS",
        onClick = { target?.let(vm::sendRealTest) },
        enabled = !busy && target != null
    )

    SectionHeader("3 · Test the alarm")
    WideButton("Ring now (test)", onClick = vm::ringTest, enabled = !busy)

    result?.let {
        Spacer(Modifier.height(4.dp))
        Text(it, color = MaterialTheme.colorScheme.primary)
    }
    if (busy) {
        Text("Working…", style = MaterialTheme.typography.bodySmall)
    }
    Text(
        "Tip: type your real PIN in place of the blank above — only on this " +
            "device. Never share your PIN with anyone.",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}
