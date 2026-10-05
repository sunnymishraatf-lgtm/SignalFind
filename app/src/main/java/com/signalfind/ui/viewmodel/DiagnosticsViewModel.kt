package com.signalfind.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.signalfind.SignalFindApp
import com.signalfind.system.PermissionHelper
import com.signalfind.system.SimInfo
import com.signalfind.ui.nav.Routes
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class DiagnosticCheck(
    val id: String,
    val title: String,
    val passed: Boolean,
    val detail: String,
    val fix: FixAction?
)

sealed interface FixAction {
    data object AppSettings : FixAction
    data object LocationSettings : FixAction
    data object BatterySettings : FixAction
    data class Navigate(val route: String) : FixAction
}

class DiagnosticsViewModel(app: Application) : AndroidViewModel(app) {
    private val container get() = (getApplication<Application>() as SignalFindApp).container

    private val _checks = MutableStateFlow<List<DiagnosticCheck>>(emptyList())
    val checks: StateFlow<List<DiagnosticCheck>> = _checks.asStateFlow()

    fun refresh() {
        val ctx = getApplication<Application>()
        viewModelScope.launch {
            val trustedCount = try {
                container.trustedRepo.count()
            } catch (_: Exception) {
                0
            }
            _checks.value = listOf(
                DiagnosticCheck(
                    "sms", "SMS permissions",
                    PermissionHelper.hasSms(ctx),
                    if (PermissionHelper.hasSms(ctx)) "RECEIVE_SMS + SEND_SMS granted"
                    else "SignalFind cannot receive commands or send replies.",
                    FixAction.AppSettings
                ),
                DiagnosticCheck(
                    "loc", "Location permission",
                    PermissionHelper.hasFineLocation(ctx),
                    if (PermissionHelper.hasFineLocation(ctx)) "Precise location granted"
                    else "Needed to obtain GPS coordinates.",
                    FixAction.AppSettings
                ),
                DiagnosticCheck(
                    "bgloc", "Background location",
                    PermissionHelper.hasBackgroundLocation(ctx),
                    if (PermissionHelper.hasBackgroundLocation(ctx))
                        "Granted — SMS commands work while the app is closed"
                    else "Required: commands arrive while the app is in the background.",
                    FixAction.AppSettings
                ),
                DiagnosticCheck(
                    "gps", "Location services enabled",
                    PermissionHelper.isLocationEnabled(ctx),
                    if (PermissionHelper.isLocationEnabled(ctx)) "GPS/location is ON"
                    else "Turn on location services to allow fixes.",
                    FixAction.LocationSettings
                ),
                DiagnosticCheck(
                    "sim", "SIM available",
                    SimInfo.hasUsableSim(ctx),
                    if (SimInfo.hasUsableSim(ctx)) "A usable SIM was detected"
                    else "No usable SIM — SMS recovery cannot work.",
                    null
                ),
                DiagnosticCheck(
                    "pin", "Recovery PIN configured",
                    container.settings.hasPin(),
                    if (container.settings.hasPin()) "PIN is set (stored as a salted hash)"
                    else "Set a PIN so commands can be authenticated.",
                    FixAction.Navigate(Routes.SETTINGS)
                ),
                DiagnosticCheck(
                    "trusted", "Trusted number configured",
                    trustedCount > 0,
                    if (trustedCount > 0) "$trustedCount trusted number(s)"
                    else "Add at least one trusted recovery number.",
                    FixAction.Navigate(Routes.TRUSTED)
                ),
                DiagnosticCheck(
                    "mode", "Recovery mode enabled",
                    container.settings.isRecoveryEnabled(),
                    if (container.settings.isRecoveryEnabled()) "SMS recovery is active"
                    else "Enable recovery mode to answer commands.",
                    FixAction.Navigate(Routes.DASHBOARD)
                ),
                DiagnosticCheck(
                    "battery", "Battery optimization",
                    PermissionHelper.isIgnoringBatteryOptimizations(ctx),
                    if (PermissionHelper.isIgnoringBatteryOptimizations(ctx))
                        "Unrestricted — best reliability"
                    else
                        "Restricted — Android may delay background work. " +
                            "Exempt SignalFind for best reliability.",
                    FixAction.BatterySettings
                ),
            )
        }
    }
}
