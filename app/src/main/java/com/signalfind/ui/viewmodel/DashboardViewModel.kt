package com.signalfind.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.signalfind.SignalFindApp
import com.signalfind.data.db.RecoveryEvent
import com.signalfind.system.BatteryInfo
import com.signalfind.system.NotificationHelper
import com.signalfind.system.PermissionHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class DashboardState(
    val recoveryEnabled: Boolean = false,
    val setupComplete: Boolean = false,
    val smsGranted: Boolean = false,
    val fineLocationGranted: Boolean = false,
    val backgroundLocationGranted: Boolean = false,
    val trustedCount: Int = 0,
    val batteryPct: Int = -1
)

class DashboardViewModel(app: Application) : AndroidViewModel(app) {
    private val container get() = (getApplication<Application>() as SignalFindApp).container

    private val _state = MutableStateFlow(DashboardState())
    val state: StateFlow<DashboardState> = _state.asStateFlow()

    /** Most recent recovery event, if any — shown on the dashboard. */
    val lastEvent: StateFlow<RecoveryEvent?> =
        container.historyRepo.observeAll()
            .map { it.firstOrNull() }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun refresh() {
        val ctx = getApplication<Application>()
        viewModelScope.launch {
            val count = try {
                container.trustedRepo.count()
            } catch (_: Exception) {
                0
            }
            _state.value = DashboardState(
                recoveryEnabled = container.settings.isRecoveryEnabled(),
                setupComplete = container.settings.isSetupComplete(),
                smsGranted = PermissionHelper.hasSms(ctx),
                fineLocationGranted = PermissionHelper.hasFineLocation(ctx),
                backgroundLocationGranted = PermissionHelper.hasBackgroundLocation(ctx),
                trustedCount = count,
                batteryPct = BatteryInfo.get(ctx).levelPct
            )
        }
    }

    fun setRecoveryEnabled(enabled: Boolean) {
        container.settings.setRecoveryEnabled(enabled)
        val ctx = getApplication<Application>()
        if (enabled) NotificationHelper.showRecoveryActive(ctx)
        else NotificationHelper.hideRecoveryActive(ctx)
        refresh()
    }
}
