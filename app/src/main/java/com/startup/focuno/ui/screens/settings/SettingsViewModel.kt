package com.startup.focuno.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.startup.focuno.data.local.SettingsStore
import com.startup.focuno.data.model.AppSettings
import com.startup.focuno.domain.model.NudgeSensitivity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(private val store: SettingsStore) : ViewModel() {

    val state: StateFlow<AppSettings> = store.settings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AppSettings())

    fun setDailyGoalMinutes(minutes: Int) {
        viewModelScope.launch { store.setDailyGoalMinutes(minutes) }
    }

    fun setNudgesEnabled(enabled: Boolean) {
        viewModelScope.launch { store.setNudgesEnabled(enabled) }
    }

    fun setNudgeSensitivity(sensitivity: NudgeSensitivity) {
        viewModelScope.launch { store.setNudgeSensitivity(sensitivity) }
    }
}
