package com.signalfind.ui.screens

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.signalfind.SignalFindApp
import com.signalfind.ui.WideButton
import com.signalfind.ui.viewmodel.SetupViewModel
import com.signalfind.ui.viewmodel.TestViewModel
import com.signalfind.util.PhoneNumberUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val TOTAL_STEPS = 8

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SetupWizardScreen(
    vm: SetupViewModel,
    onFinished: () -> Unit
) {
    val perms by vm.perms.collectAsState()
    var step by remember { mutableIntStateOf(0) }
    var consent by remember { mutableStateOf(false) }
    var pinSaved by remember { mutableStateOf(false) }
    var trustedCount by remember { mutableIntStateOf(0) }

    val context = LocalContext.current
    val container = remember {
        (context.applicationContext as SignalFindApp).container
    }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        vm.refresh()
        pinSaved = container.settings.hasPin()
        trustedCount = try {
            container.trustedRepo.count()
        } catch (_: Exception) {
            0
        }
    }

    val locationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { vm.refresh() }
    val backgroundLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { vm.refresh() }
    val smsLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { vm.refresh() }
    val notifLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { vm.refresh() }

    val nextEnabled = when (step) {
        1 -> consent
        2 -> perms.fineLocation
        3 -> perms.sms
        4 -> pinSaved
        5 -> trustedCount > 0
        else -> true
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Setup  ${step + 1} / $TOTAL_STEPS") })
        }
    ) { pad ->
        Column(
            modifier = Modifier
                .padding(pad)
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            when (step) {
                0 -> WelcomeStep()
                1 -> HowItWorksStep(consent, onConsent = { consent = it })
                2 -> LocationStep(
                    perms = perms,
                    onGrantForeground = {
                        locationLauncher.launch(
                            arrayOf(
                                Manifest.permission.ACCESS_FINE_LOCATION,
                                Manifest.permission.ACCESS_COARSE_LOCATION
                            )
                        )
                    },
                    onGrantBackground = {
                        if (Build.VERSION.SDK_INT >= 29) {
                            backgroundLauncher.launch(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
                        }
                    }
                )
                3 -> SmsStep(
                    perms = perms,
                    onGrantSms = {
                        smsLauncher.launch(
                            arrayOf(
                                Manifest.permission.RECEIVE_SMS,
                                Manifest.permission.SEND_SMS
                            )
                        )
                    },
                    onGrantNotifications = {
                        if (Build.VERSION.SDK_INT >= 33) {
                            notifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        }
                    }
                )
                4 -> PinStep(
                    alreadySet = pinSaved,
                    onSaved = { pinSaved = true }
                )
                5 -> TrustedNumberStep(
                    onCountChanged = { trustedCount = it }
                )
                6 -> {
                    Text(
                        "Send a test recovery command",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Try a command exactly as you would send it by SMS. " +
                            "Preview shows the reply text; nothing is sent unless you choose the real test.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(Modifier.height(12.dp))
                    val testVm: TestViewModel = viewModel()
                    TestRecoveryContent(vm = testVm)
                }
                7 -> DoneStep()
            }

            Spacer(Modifier.weight(1f))
            Spacer(Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (step > 0) {
                    OutlinedButton(
                        onClick = { step-- },
                        modifier = Modifier.weight(1f)
                    ) { Text("Back") }
                }
                if (step < TOTAL_STEPS - 1) {
                    Button(
                        onClick = { step++ },
                        enabled = nextEnabled,
                        modifier = Modifier.weight(1f)
                    ) { Text("Next") }
                } else {
                    Button(
                        onClick = {
                            scope.launch {
                                vm.finishSetup()
                                onFinished()
                            }
                        },
                        modifier = Modifier.weight(1f)
                    ) { Text("Finish") }
                }
            }
        }
    }
}

@Composable
private fun WelcomeStep() {
    Text("Welcome to SignalFind", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
    Spacer(Modifier.height(12.dp))
    Text(
        "SignalFind helps you locate this phone by SMS when mobile data and " +
            "Wi-Fi are unavailable. A trusted phone sends an authenticated text " +
            "command, and this phone replies with its GPS coordinates — no " +
            "internet required.",
        style = MaterialTheme.typography.bodyLarge
    )
}

@Composable
private fun HowItWorksStep(consent: Boolean, onConsent: (Boolean) -> Unit) {
    Text("How offline recovery works", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
    Spacer(Modifier.height(12.dp))
    val bullets = listOf(
        "You create a secret PIN and add trusted recovery numbers.",
        "From another phone you text: SIGNALFIND <PIN> LOCATION",
        "This phone checks the PIN and the sender, gets a GPS fix, and texts the coordinates back.",
        "Everything happens over SMS. No account, no cloud, no internet needed."
    )
    bullets.forEach { b ->
        Text("•  $b", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(vertical = 4.dp))
    }
    Spacer(Modifier.height(16.dp))
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(checked = consent, onCheckedChange = onConsent)
            Text(
                "SignalFind can respond to recovery SMS commands with this " +
                    "device's location. I enable this feature on a device I own " +
                    "or have permission to manage.",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(start = 8.dp)
            )
        }
    }
}

@Composable
private fun LocationStep(
    perms: com.signalfind.ui.viewmodel.SetupPermState,
    onGrantForeground: () -> Unit,
    onGrantBackground: () -> Unit
) {
    Text("Location permission", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
    Spacer(Modifier.height(8.dp))
    Text(
        "SignalFind needs precise location to answer LOCATION commands. " +
            "Background access is required because commands arrive by SMS " +
            "while the app is closed.",
        style = MaterialTheme.typography.bodyMedium
    )
    Spacer(Modifier.height(12.dp))
    WideButton(
        if (perms.fineLocation) "✓ Precise location granted" else "Grant location access",
        onClick = onGrantForeground,
        enabled = !perms.fineLocation
    )
    if (perms.fineLocation && Build.VERSION.SDK_INT >= 29) {
        Spacer(Modifier.height(8.dp))
        WideButton(
            if (perms.backgroundLocation) "✓ Background location granted"
            else "Grant background location (recommended)",
            onClick = onGrantBackground,
            enabled = !perms.backgroundLocation
        )
        if (!perms.backgroundLocation) {
            Spacer(Modifier.height(8.dp))
            Text(
                "Without this, Android will not deliver a location to the SMS " +
                    "receiver while the app is closed.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error
            )
        }
    }
}

@Composable
private fun SmsStep(
    perms: com.signalfind.ui.viewmodel.SetupPermState,
    onGrantSms: () -> Unit,
    onGrantNotifications: () -> Unit
) {
    Text("SMS permissions", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
    Spacer(Modifier.height(8.dp))
    Text(
        "RECEIVE_SMS lets SignalFind detect recovery commands. SEND_SMS lets " +
            "it text the coordinates back. Messages that are not recovery " +
            "commands are ignored.",
        style = MaterialTheme.typography.bodyMedium
    )
    Spacer(Modifier.height(12.dp))
    WideButton(
        if (perms.sms) "✓ SMS permissions granted" else "Grant SMS permissions",
        onClick = onGrantSms,
        enabled = !perms.sms
    )
    if (Build.VERSION.SDK_INT >= 33) {
        Spacer(Modifier.height(8.dp))
        WideButton(
            if (perms.notifications) "✓ Notifications granted"
            else "Allow notifications (recommended)",
            onClick = onGrantNotifications,
            enabled = !perms.notifications
        )
    }
}

@Composable
private fun PinStep(alreadySet: Boolean, onSaved: () -> Unit) {
    val context = LocalContext.current
    val container = remember { (context.applicationContext as SignalFindApp).container }
    val scope = rememberCoroutineScope()
    var pin by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var saved by remember(alreadySet) { mutableStateOf(alreadySet) }
    var saving by remember { mutableStateOf(false) }

    Text("Create recovery PIN", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
    Spacer(Modifier.height(8.dp))
    Text(
        "4–8 digits (6 or more recommended — a longer PIN resists guessing). " +
            "You will include it in every recovery SMS. It is stored " +
            "as a salted hash in Keystore-encrypted storage — never in plain text.",
        style = MaterialTheme.typography.bodyMedium
    )
    Spacer(Modifier.height(12.dp))
    OutlinedTextField(
        value = pin,
        onValueChange = { if (it.length <= 8 && it.all(Char::isDigit)) pin = it },
        label = { Text("PIN") },
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
    if (saved) {
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Text("  PIN saved securely.", color = MaterialTheme.colorScheme.primary)
        }
    }
    Spacer(Modifier.height(12.dp))
    WideButton(
        if (saving) "Saving…" else "Save PIN",
        enabled = !saving,
        onClick = {
            error = when {
                pin.length < 4 -> "PIN must be 4–8 digits."
                pin != confirm -> "PINs do not match."
                else -> null
            }
            if (error == null) {
                // PBKDF2 hashing is deliberately slow — off the main thread.
                saving = true
                scope.launch {
                    val failure: String? = withContext(Dispatchers.IO) {
                        try {
                            container.settings.setPin(pin)
                            null
                        } catch (e: IllegalArgumentException) {
                            e.message
                        }
                    }
                    if (failure == null) {
                        saved = true
                        onSaved()
                        pin = ""
                        confirm = ""
                    } else {
                        error = failure
                    }
                    saving = false
                }
            }
        }
    )
}

@Composable
private fun TrustedNumberStep(onCountChanged: (Int) -> Unit) {
    val context = LocalContext.current
    val container = remember { (context.applicationContext as SignalFindApp).container }
    val scope = rememberCoroutineScope()
    var number by remember { mutableStateOf("") }
    var label by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var added by remember { mutableStateOf(0) }

    LaunchedEffect(added) {
        onCountChanged(
            try {
                container.trustedRepo.count()
            } catch (_: Exception) {
                0
            }
        )
    }

    Text("Add trusted recovery number", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
    Spacer(Modifier.height(8.dp))
    Text(
        "Commands are accepted only from these numbers by default. " +
            "Use the phone you would borrow if this one is lost.",
        style = MaterialTheme.typography.bodyMedium
    )
    Spacer(Modifier.height(12.dp))
    OutlinedTextField(
        value = number,
        onValueChange = { number = it },
        label = { Text("Phone number (e.g. +911234567890)") },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
        modifier = Modifier.fillMaxWidth()
    )
    Spacer(Modifier.height(8.dp))
    OutlinedTextField(
        value = label,
        onValueChange = { label = it },
        label = { Text("Label (e.g. My second phone)") },
        modifier = Modifier.fillMaxWidth()
    )
    error?.let {
        Spacer(Modifier.height(8.dp))
        Text(it, color = MaterialTheme.colorScheme.error)
    }
    Spacer(Modifier.height(12.dp))
    WideButton("Add number", onClick = {
        scope.launch {
            try {
                container.trustedRepo.add(number, label.ifBlank { "Recovery phone" })
                number = ""
                label = ""
                error = null
                added++
            } catch (e: IllegalArgumentException) {
                error = e.message ?: "Invalid number"
            } catch (_: Exception) {
                error = "Could not save the number"
            }
        }
    })
    Spacer(Modifier.height(4.dp))
    if (!PhoneNumberUtils.isPlausible(number) && number.isNotBlank()) {
        Text("Enter a valid phone number.", style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun DoneStep() {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
        Spacer(Modifier.height(32.dp))
        Icon(
            Icons.Filled.CheckCircle,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(16.dp)
        )
        Text("SignalFind is ready.", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Text(
            "Lost Mode is ON. From a trusted phone, text:\n\nSIGNALFIND <PIN> LOCATION",
            style = MaterialTheme.typography.bodyLarge
        )
    }
}
