package com.signalfind.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.signalfind.SignalFindApp
import com.signalfind.system.NotificationHelper
import com.signalfind.system.PermissionHelper
import com.signalfind.system.SimInfo
import com.signalfind.system.SimSubscription
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class SettingsState(
    val trustedOnly: Boolean = true,
    val recoveryEnabled: Boolean = false,
    val keyword: String = "FINDFONE",
    val preferredSubId: Int = -1,
    val hasPin: Boolean = false,
    val simPermission: Boolean = false
)

class SettingsViewModel(app: Application) : AndroidViewModel(app) {
    private val container get() = (getApplication<Application>() as SignalFindApp).container

    private val _state = MutableStateFlow(SettingsState())
    val state: StateFlow<SettingsState> = _state.asStateFlow()

    private val _subscriptions = MutableStateFlow<List<SimSubscription>>(emptyList())
    val subscriptions: StateFlow<List<SimSubscription>> = _subscriptions.asStateFlow()

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    fun refresh() {
        val ctx = getApplication<Application>()
        viewModelScope.launch {
            _state.value = SettingsState(
                trustedOnly = container.settings.isTrustedOnly(),
                recoveryEnabled = container.settings.isRecoveryEnabled(),
                keyword = container.settings.getEmergencyKeyword(),
                preferredSubId = container.settings.getPreferredSubId(),
                hasPin = container.settings.hasPin(),
                simPermission = PermissionHelper.hasPhoneState(ctx)
            )
            _subscriptions.value = SimInfo.getSubscriptions(ctx)
        }
    }

    fun setTrustedOnly(value: Boolean) {
        container.settings.setTrustedOnly(value)
        refresh()
    }

    fun setRecoveryEnabled(enabled: Boolean) {
        container.settings.setRecoveryEnabled(enabled)
        val ctx = getApplication<Application>()
        if (enabled) NotificationHelper.showRecoveryActive(ctx)
        else NotificationHelper.hideRecoveryActive(ctx)
        refresh()
    }

    fun setKeyword(keyword: String) {
        try {
            container.settings.setEmergencyKeyword(keyword)
            _message.value = "Emergency keyword updated."
        } catch (e: IllegalArgumentException) {
            _message.value = e.message
        }
        refresh()
    }

    fun setPreferredSubId(subId: Int) {
        container.settings.setPreferredSubId(subId)
        refresh()
    }

    fun changePin(newPin: String) {
        // PBKDF2 hashing is deliberately slow — never on the main thread.
        viewModelScope.launch {
            _message.value = "Updating PIN…"
            val error: String? = withContext(Dispatchers.IO) {
                try {
                    container.settings.setPin(newPin)
                    null
                } catch (e: IllegalArgumentException) {
                    e.message
                }
            }
            _message.value = error ?: "PIN updated."
            refresh()
        }
    }

    fun clearMessage() {
        _message.value = null
    }
}
