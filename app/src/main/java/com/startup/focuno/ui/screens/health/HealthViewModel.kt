package com.startup.focuno.ui.screens.health

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.startup.focuno.data.local.OemAutostart
import com.startup.focuno.data.local.OemFamily
import com.startup.focuno.data.repository.ProtectionRepository
import com.startup.focuno.data.repository.ProtectionStatus
import com.startup.focuno.service.accessibility.FocunoAccessibilityService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

data class HealthUiState(
    val status: ProtectionStatus? = null,
    /** Enabled in settings, but Android has not actually started the service (or it was just killed). */
    val accessibilityEnabledButIdle: Boolean = false,
    val showRestrictedHelp: Boolean = false,
    val oem: OemFamily = OemAutostart.detect(),
)

@HiltViewModel
class HealthViewModel @Inject constructor(
    private val protectionRepository: ProtectionRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(HealthUiState())
    val state: StateFlow<HealthUiState> = _state.asStateFlow()

    fun refresh() {
        viewModelScope.launch {
            val status = withContext(Dispatchers.IO) { protectionRepository.status() }
            _state.value = _state.value.copy(
                status = status,
                accessibilityEnabledButIdle = status.accessibilityEnabled && !FocunoAccessibilityService.isConnected,
                showRestrictedHelp = !status.accessibilityEnabled && protectionRepository.likelyNeedsRestrictedSettingUnlock(),
            )
        }
    }
}
