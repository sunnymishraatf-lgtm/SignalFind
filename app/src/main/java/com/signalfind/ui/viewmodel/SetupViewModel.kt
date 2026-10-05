package com.signalfind.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.signalfind.SignalFindApp
import com.signalfind.system.NotificationHelper
import com.signalfind.system.PermissionHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class SetupPermState(
    val fineLocation: Boolean = false,
    val backgroundLocation: Boolean = false,
    val sms: Boolean = false,
    val notifications: Boolean = false
)

class SetupViewModel(app: Application) : AndroidViewModel(app) {
    private val container get() = (getApplication<Application>() as SignalFindApp).container

    private val _perms = MutableStateFlow(SetupPermState())
    val perms: StateFlow<SetupPermState> = _perms.asStateFlow()

    fun refresh() {
        val ctx = getApplication<Application>()
        _perms.value = SetupPermState(
            fineLocation = PermissionHelper.hasFineLocation(ctx),
            backgroundLocation = PermissionHelper.hasBackgroundLocation(ctx),
            sms = PermissionHelper.hasSms(ctx),
            notifications = PermissionHelper.hasNotifications(ctx)
        )
    }

    /** Called on the final wizard step: enables recovery and shows its status. */
    fun finishSetup() {
        container.settings.setSetupComplete(true)
        container.settings.setRecoveryEnabled(true)
        NotificationHelper.showRecoveryActive(getApplication())
    }
}
