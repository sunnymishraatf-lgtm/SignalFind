package com.signalfind.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.signalfind.SignalFindApp
import com.signalfind.data.db.TrustedNumber
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class TrustedNumbersViewModel(app: Application) : AndroidViewModel(app) {
    private val container get() = (getApplication<Application>() as SignalFindApp).container

    val numbers: StateFlow<List<TrustedNumber>> =
        container.trustedRepo.observeAll()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _trustedOnly = MutableStateFlow(container.settings.isTrustedOnly())
    val trustedOnly: StateFlow<Boolean> = _trustedOnly.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    fun add(number: String, label: String) {
        viewModelScope.launch {
            try {
                container.trustedRepo.add(number, label)
                _error.value = null
            } catch (e: IllegalArgumentException) {
                _error.value = e.message ?: "Invalid number"
            } catch (_: Exception) {
                _error.value = "Could not save the number"
            }
        }
    }

    fun remove(entry: TrustedNumber) {
        viewModelScope.launch {
            // Safety: with "trusted only" on, the last trusted number is the
            // owner's only way to command the phone — never remove it silently.
            val remaining = numbers.value.filter { it.id != entry.id }
            if (remaining.isEmpty() && container.settings.isTrustedOnly()) {
                _error.value = "Keep at least one trusted number, or turn off " +
                    "\"Only accept trusted numbers\" first — otherwise no SMS " +
                    "command would ever be accepted."
                return@launch
            }
            _error.value = null
            container.trustedRepo.remove(entry)
        }
    }

    fun setTrustedOnly(value: Boolean) {
        container.settings.setTrustedOnly(value)
        _trustedOnly.value = value
    }

    fun clearError() {
        _error.value = null
    }
}
