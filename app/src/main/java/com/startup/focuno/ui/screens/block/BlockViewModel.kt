package com.startup.focuno.ui.screens.block

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.startup.focuno.R
import com.startup.focuno.data.local.SettingsStore
import com.startup.focuno.data.repository.InstalledAppsRepository
import com.startup.focuno.data.repository.ProtectionRepository
import com.startup.focuno.data.repository.ProtectionStatus
import com.startup.focuno.data.repository.ScheduleRepository
import com.startup.focuno.domain.model.BlockSchedule
import com.startup.focuno.domain.usecase.StrictLock
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.ExperimentalCoroutinesApi
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
import kotlinx.coroutines.flow.map

data class ScheduleRowUi(val schedule: BlockSchedule, val lockedUntilMs: Long?)

data class AppScheduleGroup(val packageName: String, val label: String, val schedules: List<ScheduleRowUi>)

data class BlockUiState(
    val isLoading: Boolean = true,
    val groups: List<AppScheduleGroup> = emptyList(),
    val quickBlockUntilMs: Long = 0L,
    val quickBlockLabel: String = "",
    val protection: ProtectionStatus? = null,
)

@HiltViewModel
class BlockViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
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
        val groups = schedules
            .groupBy { it.packageName }
            .map { (pkg, items) ->
                AppScheduleGroup(
                    packageName = pkg,
                    label = installedApps.labelBlocking(pkg),
                    schedules = items.sortedBy { it.startMinuteOfDay }.map { ScheduleRowUi(it, StrictLock.lockedUntilMs(it)) },
                )
            }
            .sortedBy { it.label.lowercase() }
        BlockUiState(
            isLoading = false,
            groups = groups,
            quickBlockUntilMs = settings.quickBlockUntilMs,
            quickBlockLabel = settings.quickBlockLabel,
            protection = status,
        )
    }
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), BlockUiState())

    fun setEnabled(schedule: BlockSchedule, enabled: Boolean) {
        if (StrictLock.lockedUntilMs(schedule) != null) return
        viewModelScope.launch { scheduleRepository.save(schedule.copy(enabled = enabled)) }
    }

    /** Blocks every distracting app for [minutes]. The normal "I need this" friction still applies. */
    fun startQuickBlock(minutes: Int) {
        viewModelScope.launch {
            scheduleRepository.startQuickBlock(
                untilMs = System.currentTimeMillis() + minutes * 60_000L,
                label = context.getString(R.string.quick_block_label_block_now),
            )
        }
    }
}
