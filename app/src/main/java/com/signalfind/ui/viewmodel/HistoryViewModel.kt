package com.signalfind.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.signalfind.SignalFindApp
import com.signalfind.data.db.RecoveryEvent
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class HistoryViewModel(app: Application) : AndroidViewModel(app) {
    private val container get() = (getApplication<Application>() as SignalFindApp).container

    val events: StateFlow<List<RecoveryEvent>> =
        container.historyRepo.observeAll()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun delete(event: RecoveryEvent) {
        viewModelScope.launch { container.historyRepo.delete(event.id) }
    }

    fun clearAll() {
        viewModelScope.launch { container.historyRepo.clear() }
    }
}
