package com.startup.focuno.ui.screens.block

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.startup.focuno.data.local.SettingsStore
import com.startup.focuno.data.repository.InstalledAppsRepository
import com.startup.focuno.data.repository.LimitRepository
import com.startup.focuno.data.repository.ProtectionRepository
import com.startup.focuno.data.repository.ProtectionStatus
import com.startup.focuno.data.repository.ScheduleRepository
import com.startup.focuno.data.repository.UsageTrackingRepository
import com.startup.focuno.domain.model.AppLimit
import com.startup.focuno.domain.model.BlockSchedule
import com.startup.focuno.domain.usecase.DailyLimitPolicy
import com.startup.focuno.domain.usecase.StrictLock
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.ZonedDateTime
import javax.inject.Inject

data class ScheduleRowUi(val schedule: BlockSchedule, val lockedUntilMs: Long?)

data class ScheduleItem(val label: String, val row: ScheduleRowUi)

/** A daily limit with today's use so far. [lockedUntilMs] is set while a used-up strict limit cannot change. */
data class LimitItem(val label: String, val limit: AppLimit, val usedMs: Long, val lockedUntilMs: Long?)

data class BlockUiState(
    val isLoading: Boolean = true,
    val items: List<ScheduleItem> = emptyList(),
    val limits: List<LimitItem> = emptyList(),
    /** Epoch ms until which a focus session is pausing distracting apps, or 0. */
    val focusUntilMs: Long = 0L,
    val protection: ProtectionStatus? = null,
)

@HiltViewModel
class BlockViewModel @Inject constructor(
    private val scheduleRepository: ScheduleRepository,
    private val limitRepository: LimitRepository,
    private val usageRepository: UsageTrackingRepository,
    private val settingsStore: SettingsStore,
    private val installedApps: InstalledAppsRepository,
    private val protectionRepository: ProtectionRepository,
) : ViewModel() {

    private val protection = MutableStateFlow<ProtectionStatus?>(null)
    private val usedToday = MutableStateFlow<Map<String, Long>>(emptyMap())

    /** Today's use of each limited app, for the "12m of 30m" line. Cheap enough to run every half minute. */
    fun refreshUsage() {
        viewModelScope.launch {
            val packages = limitRepository.observeAll().first().map { it.packageName }.toSet()
            usedToday.value = try {
                usageRepository.foregroundMsToday(packages)
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                usedToday.value
            }
        }
    }

    fun refreshProtection() {
        viewModelScope.launch { protection.value = withContext(Dispatchers.IO) { protectionRepository.status() } }
    }

    val state: StateFlow<BlockUiState> = combine(
        scheduleRepository.observeAll(),
        limitRepository.observeAll(),
        usedToday,
        settingsStore.settings,
        protection,
    ) { schedules, limits, used, settings, status ->
        val labels = (schedules.map { it.packageName } + limits.map { it.packageName }).distinct()
            .associateWith { installedApps.labelBlocking(it) }
        val now = ZonedDateTime.now()
        val limitItems = limits
            .map { limit ->
                val usedMs = used[limit.packageName] ?: 0L
                LimitItem(labels.getValue(limit.packageName), limit, usedMs, DailyLimitPolicy.lockedUntilMs(limit, usedMs, now))
            }
            .sortedBy { it.label.lowercase() }
        val items = schedules
            .map { ScheduleItem(labels.getValue(it.packageName), ScheduleRowUi(it, StrictLock.lockedUntilMs(it))) }
            .sortedWith(compareBy({ it.label.lowercase() }, { it.row.schedule.startMinuteOfDay }))
        BlockUiState(
            isLoading = false,
            items = items,
            limits = limitItems,
            focusUntilMs = settings.quickBlockUntilMs,
            protection = status,
        )
    }
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), BlockUiState())

    fun setLimitEnabled(item: LimitItem, enabled: Boolean) {
        if (item.lockedUntilMs != null) return
        viewModelScope.launch { limitRepository.save(item.limit.copy(enabled = enabled)) }
    }

    fun setEnabled(schedule: BlockSchedule, enabled: Boolean) {
        if (StrictLock.lockedUntilMs(schedule) != null) return
        viewModelScope.launch { scheduleRepository.save(schedule.copy(enabled = enabled)) }
    }
}
