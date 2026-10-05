package com.signalfind

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.signalfind.ui.nav.Routes
import com.signalfind.ui.screens.DashboardScreen
import com.signalfind.ui.screens.DiagnosticsScreen
import com.signalfind.ui.screens.HistoryScreen
import com.signalfind.ui.screens.OfflineMapScreen
import com.signalfind.ui.screens.SettingsScreen
import com.signalfind.ui.screens.SetupWizardScreen
import com.signalfind.ui.screens.TestRecoveryScreen
import com.signalfind.ui.screens.TrustedNumbersScreen
import com.signalfind.ui.theme.SignalFindTheme
import com.signalfind.ui.viewmodel.DashboardViewModel
import com.signalfind.ui.viewmodel.DiagnosticsViewModel
import com.signalfind.ui.viewmodel.HistoryViewModel
import com.signalfind.ui.viewmodel.SettingsViewModel
import com.signalfind.ui.viewmodel.SetupViewModel
import com.signalfind.ui.viewmodel.TestViewModel
import com.signalfind.ui.viewmodel.TrustedNumbersViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SignalFindTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    SignalFindNav()
                }
            }
        }
    }
}

@Composable
private fun SignalFindNav() {
    val app = androidx.compose.ui.platform.LocalContext.current.applicationContext as SignalFindApp
    val start = remember {
        if (app.container.settings.isSetupComplete()) Routes.DASHBOARD else Routes.SETUP
    }
    val nav = rememberNavController()
    val go: (String) -> Unit = { nav.navigate(it) }
    val back: () -> Unit = { nav.popBackStack() }

    NavHost(navController = nav, startDestination = start) {
        composable(Routes.DASHBOARD) {
            val vm: DashboardViewModel = viewModel()
            DashboardScreen(vm = vm, onNavigate = go)
        }
        composable(Routes.SETUP) {
            val vm: SetupViewModel = viewModel()
            SetupWizardScreen(vm = vm, onFinished = {
                nav.navigate(Routes.DASHBOARD) {
                    popUpTo(Routes.SETUP) { inclusive = true }
                }
            })
        }
        composable(Routes.TRUSTED) {
            val vm: TrustedNumbersViewModel = viewModel()
            TrustedNumbersScreen(vm = vm, onBack = back)
        }
        composable(Routes.HISTORY) {
            val vm: HistoryViewModel = viewModel()
            HistoryScreen(vm = vm, onBack = back)
        }
        composable(Routes.SETTINGS) {
            val vm: SettingsViewModel = viewModel()
            SettingsScreen(vm = vm, onBack = back)
        }
        composable(Routes.DIAGNOSTICS) {
            val vm: DiagnosticsViewModel = viewModel()
            DiagnosticsScreen(vm = vm, onBack = back, onNavigate = go)
        }
        composable(Routes.OFFLINE_MAP) {
            OfflineMapScreen(onBack = back)
        }
        composable(Routes.TEST) {
            val vm: TestViewModel = viewModel()
            TestRecoveryScreen(vm = vm, onBack = back)
        }
    }
}
