package com.signalfind.ui.viewmodel

import android.app.Application
import android.content.Intent
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.signalfind.SignalFindApp
import com.signalfind.alarm.RingService
import com.signalfind.data.db.TrustedNumber
import com.signalfind.sms.CommandParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Powers the built-in test screen. "Preview" runs the real parser and reply
 * builder without sending any SMS; "Send real test" sends a genuine LOCATION
 * reply to a trusted number.
 */
class TestViewModel(app: Application) : AndroidViewModel(app) {
    private val container get() = (getApplication<Application>() as SignalFindApp).container

    val trustedNumbers: StateFlow<List<TrustedNumber>> =
        container.trustedRepo.observeAll()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _preview = MutableStateFlow<String?>(null)
    val preview: StateFlow<String?> = _preview.asStateFlow()

    private val _busy = MutableStateFlow(false)
    val busy: StateFlow<Boolean> = _busy.asStateFlow()

    private val _result = MutableStateFlow<String?>(null)
    val result: StateFlow<String?> = _result.asStateFlow()

    fun preview(commandText: String) {
        viewModelScope.launch {
            _busy.value = true
            _result.value = null
            try {
                val keyword = container.settings.getEmergencyKeyword()
                val parsed = CommandParser.parse(commandText, keyword)
                if (parsed == null) {
                    _preview.value = "Not a valid SignalFind command.\n\n" +
                        "Expected:\nSIGNALFIND <PIN> LOCATION|BATTERY|RING|STATUS\n" +
                        "or: $keyword <PIN>"
                    return@launch
                }
                // PBKDF2 verification is deliberately slow — off the main thread.
                val pinOk = withContext(Dispatchers.IO) {
                    container.settings.verifyPin(parsed.pin)
                }
                if (!pinOk) {
                    _preview.value = "PIN does not match the configured PIN.\n" +
                        "The reply below would NOT be sent."
                    return@launch
                }
                if (parsed.command is CommandParser.Command.Ring) {
                    _preview.value = "RING is valid and authenticated.\n\n" +
                        "In preview mode the alarm is not started.\n" +
                        "Use “Ring now (test)” below to hear it."
                    return@launch
                }
                val draft = container.responder.buildReply(parsed.command)
                _preview.value = "This exact text would be sent by SMS:\n\n${draft.text}"
            } finally {
                _busy.value = false
            }
        }
    }

    /** Sends a real LOCATION reply to [destination] (must be a trusted number). */
    fun sendRealTest(destination: String) {
        viewModelScope.launch {
            _busy.value = true
            _result.value = null
            try {
                val sent = container.responder.handle(destination, CommandParser.Command.Location)
                _result.value = if (sent) {
                    "Test SMS sent to ${com.signalfind.util.PhoneNumberUtils.mask(destination)}."
                } else {
                    "Failed to send the test SMS. Check SMS permission and signal."
                }
            } finally {
                _busy.value = false
            }
        }
    }

    fun ringTest() {
        val ctx = getApplication<Application>()
        try {
            ContextCompat.startForegroundService(
                ctx,
                Intent(ctx, RingService::class.java).setAction(RingService.ACTION_START)
            )
            _result.value = "Alarm started — stop it from the notification."
        } catch (e: Exception) {
            _result.value = "Could not start the alarm: ${e.javaClass.simpleName}."
        }
    }

    fun clearMessages() {
        _preview.value = null
        _result.value = null
    }
}
