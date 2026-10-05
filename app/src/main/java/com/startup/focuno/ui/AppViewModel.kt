package com.startup.focuno.ui

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.startup.focuno.data.local.SettingsStore
import com.startup.focuno.data.model.AppSettings
import com.startup.focuno.domain.usecase.RunDailyRollupUseCase
import com.startup.focuno.service.monitor.MonitorServiceLauncher
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface AppUiState {
    data object Loading : AppUiState
    data object Onboarding : AppUiState
    data object Main : AppUiState
}

@HiltViewModel
class AppViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val settingsStore: SettingsStore,
    private val rollup: RunDailyRollupUseCase,
) : ViewModel() {

    val state: StateFlow<AppUiState> = settingsStore.settings
        .map { if (it.onboardingCompleted) AppUiState.Main else AppUiState.Onboarding }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AppUiState.Loading)

    /** True once, after an update that brought something worth pointing out. */
    val showWhatsNew: StateFlow<Boolean> = settingsStore.settings
        .map { it.onboardingCompleted && it.whatsNewSeen < AppSettings.WHATS_NEW_VERSION }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    fun dismissWhatsNew() {
        viewModelScope.launch { settingsStore.setWhatsNewSeen(AppSettings.WHATS_NEW_VERSION) }
    }

    init {
        viewModelScope.launch {
            settingsStore.settings.first { it.onboardingCompleted }
            MonitorServiceLauncher.start(context)
            try {
                // Repairs any night the 00:05 job missed, so trends and the streak are never stale.
                rollup()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w("AppViewModel", "Rollup on launch failed", e)
            }
        }
    }
}
