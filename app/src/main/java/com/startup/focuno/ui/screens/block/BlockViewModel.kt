package com.startup.focuno.ui.screens.block

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.startup.focuno.data.local.SettingsStore
import com.startup.focuno.data.repository.InstalledAppsRepository
import com.startup.focuno.data.repository.ProtectionRepository
import com.startup.focuno.data.repository.ProtectionStatus
import com.startup.focuno.data.repository.ScheduleRepository
import com.startup.focuno.domain.model.BlockSchedule
import com.startup.focuno.domain.usecase.StrictLock
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

data class ScheduleRowUi(val schedule: BlockSchedule, val lockedUntilMs: Long?)

data class ScheduleItem(val label: String, val row: ScheduleRowUi)

data class BlockUiState(
    val isLoading: Boolean = true,
    val items: List<ScheduleItem> = emptyList(),
    /** Epoch ms until which a focus session is pausing distracting apps, or 0. */
    val focusUntilMs: Long = 0L,
    val protection: ProtectionStatus? = null,
)

@HiltViewModel
class BlockViewModel @Inject constructor(
    private val scheduleRepository: ScheduleRepository,
    private val settingsStore: SettingsStore,
    private val installedApps: InstalledAppsRepository,
    private val protectionRepository: ProtectionRepository,
) : ViewModel() {

    private val protection = MutableStateFlow<ProtectionStatus?>(null)

    fun refreshProtection() {
        viewModelScope.launch { protection.value = withContext(Dispatchers.IO) { protectionRepository.status() } }
    }

    val state: StateFlow<BlockUiState> = combine(scheduleRepository.observeAll(), settingsStore.settings, protection) { schedules, settings, status ->
        val labels = schedules.map { it.packageName }.distinct().associateWith { installedApps.labelBlocking(it) }
        val items = schedules
            .map { ScheduleItem(labels.getValue(it.packageName), ScheduleRowUi(it, StrictLock.lockedUntilMs(it))) }
            .sortedWith(compareBy({ it.label.lowercase() }, { it.row.schedule.startMinuteOfDay }))
        BlockUiState(
            isLoading = false,
            items = items,
            focusUntilMs = settings.quickBlockUntilMs,
            protection = status,
        )
    }
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), BlockUiState())

    fun setEnabled(schedule: BlockSchedule, enabled: Boolean) {
        if (StrictLock.lockedUntilMs(schedule) != null) return
        viewModelScope.launch { scheduleRepository.save(schedule.copy(enabled = enabled)) }
    }
}
