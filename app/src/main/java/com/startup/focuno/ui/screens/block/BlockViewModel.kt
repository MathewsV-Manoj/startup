package com.startup.focuno.ui.screens.block

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.startup.focuno.data.local.SettingsStore
import com.startup.focuno.data.repository.AppCategoryRepository
import com.startup.focuno.data.repository.InstalledAppsRepository
import com.startup.focuno.data.repository.LimitRepository
import com.startup.focuno.data.repository.ProtectionRepository
import com.startup.focuno.data.repository.ProtectionStatus
import com.startup.focuno.data.repository.ScheduleRepository
import com.startup.focuno.data.repository.UsageTrackingRepository
import com.startup.focuno.domain.model.AppCategory
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
data class LimitItem(val label: String, val limit: AppLimit, val usedMs: Long, val lockedUntilMs: Long?, val opensToday: Int = 0)

/** The time-eater budget: [minutes] 0 means off. [lockedUntilMs] is set while a used-up strict budget cannot change. */
data class BudgetUi(val minutes: Int = 0, val strict: Boolean = false, val usedMs: Long = 0L, val lockedUntilMs: Long? = null)

private data class UsageSnapshot(
    val perApp: Map<String, Long> = emptyMap(),
    val opens: Map<String, Int> = emptyMap(),
    val timeEaterMs: Long = 0L,
)

/** An app with "pause before opening". */
data class MindfulItem(val packageName: String, val label: String)

data class BlockUiState(
    val isLoading: Boolean = true,
    val items: List<ScheduleItem> = emptyList(),
    val limits: List<LimitItem> = emptyList(),
    val budget: BudgetUi = BudgetUi(),
    val mindful: List<MindfulItem> = emptyList(),
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
    private val categoryRepository: AppCategoryRepository,
) : ViewModel() {

    private val protection = MutableStateFlow<ProtectionStatus?>(null)
    private val usedToday = MutableStateFlow(UsageSnapshot())

    /** Today's use of each app and of all time-eaters together, for the "12m of 30m" lines. */
    fun refreshUsage() {
        viewModelScope.launch {
            usedToday.value = try {
                val usage = usageRepository.usageToday()
                val overrides = categoryRepository.overrides()
                val timeEaterMs = usage.values
                    .filter { categoryRepository.resolve(it.packageName, overrides) == AppCategory.DISTRACTING }
                    .sumOf { it.foregroundMs }
                UsageSnapshot(
                    perApp = usage.mapValues { it.value.foregroundMs },
                    opens = usage.mapValues { it.value.openCount },
                    timeEaterMs = timeEaterMs,
                )
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
        val labels = (schedules.map { it.packageName } + limits.map { it.packageName } + settings.mindfulApps).distinct()
            .associateWith { installedApps.labelBlocking(it) }
        val now = ZonedDateTime.now()
        val limitItems = limits
            .map { limit ->
                val usedMs = used.perApp[limit.packageName] ?: 0L
                val opens = used.opens[limit.packageName] ?: 0
                LimitItem(labels.getValue(limit.packageName), limit, usedMs, DailyLimitPolicy.lockedUntilMs(limit, usedMs, now, opens), opens)
            }
            .sortedBy { it.label.lowercase() }
        val items = schedules
            .map { ScheduleItem(labels.getValue(it.packageName), ScheduleRowUi(it, StrictLock.lockedUntilMs(it))) }
            .sortedWith(compareBy({ it.label.lowercase() }, { it.row.schedule.startMinuteOfDay }))
        val budget = settings.timeEaterBudget
        BlockUiState(
            isLoading = false,
            items = items,
            limits = limitItems,
            mindful = settings.mindfulApps.map { MindfulItem(it, labels.getValue(it)) }.sortedBy { it.label.lowercase() },
            budget = BudgetUi(
                minutes = settings.budgetMinutes,
                strict = settings.budgetStrict,
                usedMs = used.timeEaterMs,
                lockedUntilMs = budget?.let { DailyLimitPolicy.lockedUntilMs(it, used.timeEaterMs, now) },
            ),
            focusUntilMs = settings.quickBlockUntilMs,
            protection = status,
        )
    }
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), BlockUiState())

    fun removeMindful(packageName: String) {
        viewModelScope.launch { settingsStore.setMindful(packageName, false) }
    }

    /** 0 minutes turns the budget off. Refused while a used-up strict budget is locked. */
    fun setBudget(minutes: Int, strict: Boolean) {
        if (state.value.budget.lockedUntilMs != null) return
        viewModelScope.launch { settingsStore.setTimeEaterBudget(minutes, strict && minutes > 0) }
    }

    fun setLimitEnabled(item: LimitItem, enabled: Boolean) {
        if (item.lockedUntilMs != null) return
        viewModelScope.launch { limitRepository.save(item.limit.copy(enabled = enabled)) }
    }

    fun setEnabled(schedule: BlockSchedule, enabled: Boolean) {
        if (StrictLock.lockedUntilMs(schedule) != null) return
        viewModelScope.launch { scheduleRepository.save(schedule.copy(enabled = enabled)) }
    }
}
