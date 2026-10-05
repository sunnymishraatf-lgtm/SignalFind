package com.signalfind.ui.screens

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.signalfind.ui.AppScaffold
import com.signalfind.ui.viewmodel.DiagnosticCheck
import com.signalfind.ui.viewmodel.DiagnosticsViewModel
import com.signalfind.ui.viewmodel.FixAction

@Composable
fun DiagnosticsScreen(
    vm: DiagnosticsViewModel,
    onBack: () -> Unit,
    onNavigate: (String) -> Unit
) {
    val checks by vm.checks.collectAsState()
    val context = LocalContext.current
    LaunchedEffect(Unit) { vm.refresh() }

    AppScaffold(title = "Diagnostics", onBack = onBack) { pad ->
        LazyColumn(
            modifier = Modifier
                .padding(pad)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                Text(
                    "Each check must pass for reliable offline recovery. " +
                        "Tap a fix to resolve a failing check.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            items(checks, key = { it.id }) { check ->
                DiagnosticRow(check = check, onFix = { fix ->
                    when (fix) {
                        FixAction.AppSettings -> context.startActivity(
                            Intent(
                                Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                Uri.fromParts("package", context.packageName, null)
                            ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        )
                        FixAction.LocationSettings -> context.startActivity(
                            Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        )
                        FixAction.BatterySettings -> {
                            if (Build.VERSION.SDK_INT >= 23) {
                                context.startActivity(
                                    Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
                                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                )
                            }
                        }
                        is FixAction.Navigate -> onNavigate(fix.route)
                    }
                })
            }
            item { Spacer(Modifier.height(8.dp)) }
        }
    }
}

@Composable
private fun DiagnosticRow(check: DiagnosticCheck, onFix: (FixAction) -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                if (check.passed) Icons.Filled.CheckCircle else Icons.Filled.Warning,
                contentDescription = null,
                tint = if (check.passed) Color(0xFF1B8A5A) else MaterialTheme.colorScheme.error
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 12.dp)
            ) {
                Text(check.title, fontWeight = FontWeight.SemiBold)
                Text(
                    check.detail,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (!check.passed && check.fix != null) {
                TextButton(onClick = { onFix(check.fix) }) { Text("Fix") }
            }
        }
    }
}
