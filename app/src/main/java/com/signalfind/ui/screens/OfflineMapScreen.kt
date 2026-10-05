package com.signalfind.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.signalfind.ui.AppScaffold
import com.signalfind.ui.WideButton
import java.util.Locale

/**
 * Offline coordinate viewer. Enter coordinates received by SMS and see them
 * plotted on a schematic grid — no internet, no map tiles, no tracking.
 * The coordinates themselves are the source of truth; this is only a visual aid.
 */
@Composable
fun OfflineMapScreen(onBack: () -> Unit) {
    var latText by remember { mutableStateOf("") }
    var lngText by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var point by remember { mutableStateOf<Pair<Double, Double>?>(null) }

    AppScaffold(title = "Offline map", onBack = onBack) { pad ->
        Column(
            modifier = Modifier
                .padding(pad)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                "Paste coordinates from a SignalFind SMS reply to plot them. " +
                    "This is a schematic grid, not real map tiles — it works " +
                    "fully offline.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = latText,
                    onValueChange = { latText = it },
                    label = { Text("Latitude") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
                OutlinedTextField(
                    value = lngText,
                    onValueChange = { lngText = it },
                    label = { Text("Longitude") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
            }
            error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            WideButton("Plot coordinates", onClick = {
                // Accept both '.' and ',' as decimal separators (some locales
                // type a comma); toDoubleOrNull only understands '.'.
                val lat = latText.replace(',', '.').toDoubleOrNull()
                val lng = lngText.replace(',', '.').toDoubleOrNull()
                error = when {
                    lat == null || lng == null -> "Enter valid decimal coordinates."
                    lat !in -90.0..90.0 || lng !in -180.0..180.0 -> "Coordinates out of range."
                    else -> null
                }
                point = if (error == null) lat!! to lng!! else null
            })

            point?.let { (lat, lng) ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(12.dp)) {
                        SchematicMap()
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "Plotted: %.6f, %.6f".format(Locale.US, lat, lng),
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            "Grid: 0.1° ≈ 11.1 km. North is up.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SchematicMap() {
    // 1.0° window centered on the plotted point; gridlines every 0.1°.
    val span = 1.0
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF0E1512))
    ) {
        val w = size.width
        val h = size.height
        val grid = Color(0xFF2A3B33)
        val label = Color(0xFF8AA399)

        var d = -0.5
        while (d <= 0.5001) {
            val x = ((d + 0.5) / span * w).toFloat()
            val y = ((d + 0.5) / span * h).toFloat()
            drawLine(grid, Offset(x, 0f), Offset(x, h), strokeWidth = 1f)
            drawLine(grid, Offset(0f, y), Offset(w, y), strokeWidth = 1f)
            d += 0.1
        }
        // Border + north arrow.
        drawRect(label, style = Stroke(width = 3f))
        val cx = w / 2f
        val cy = h / 2f
        drawLine(label, Offset(cx, 36f), Offset(cx, 12f), strokeWidth = 4f)
        drawLine(label, Offset(cx, 12f), Offset(cx - 10f, 30f), strokeWidth = 4f)
        drawLine(label, Offset(cx, 12f), Offset(cx + 10f, 30f), strokeWidth = 4f)

        // Crosshair + pin at the exact coordinate (window is centered on it).
        val accent = Color(0xFF22C07A)
        drawLine(accent, Offset(cx - 40f, cy), Offset(cx + 40f, cy), strokeWidth = 2f)
        drawLine(accent, Offset(cx, cy - 40f), Offset(cx, cy + 40f), strokeWidth = 2f)
        drawCircle(accent, radius = 16f, center = Offset(cx, cy))
        drawCircle(Color.White, radius = 7f, center = Offset(cx, cy))
    }
}
