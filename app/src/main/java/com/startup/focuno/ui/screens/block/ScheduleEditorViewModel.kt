package com.startup.focuno.ui.screens.block

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.startup.focuno.data.repository.AppCategoryRepository
import com.startup.focuno.data.repository.InstalledApp
import com.startup.focuno.data.repository.InstalledAppsRepository
import com.startup.focuno.data.repository.ScheduleRepository
import com.startup.focuno.domain.model.AppCategory
import com.startup.focuno.domain.model.BlockSchedule
import com.startup.focuno.domain.usecase.StrictLock
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class TimeField { START, END }

data class ScheduleEditorUiState(
    val isOpen: Boolean = false,
    val isLoadingApps: Boolean = false,
    val apps: List<InstalledApp> = emptyList(),
    val query: String = "",
    val id: Long = 0,
    val packageName: String? = null,
    val appLabel: String = "",
    val startMinute: Int = 22 * 60,
    val endMinute: Int = 6 * 60,
    val daysMask: Int = BlockSchedule.ALL_DAYS,
    val strict: Boolean = false,
    val enabled: Boolean = true,
    val timePickerFor: TimeField? = null,
    /** When non-null the schedule is a strict window that is open right now and cannot be changed. */
    val lockedUntilMs: Long? = null,
) {
    val isEditing: Boolean get() = id != 0L
    val isOvernight: Boolean get() = endMinute <= startMinute
    val canSave: Boolean get() = packageName != null && daysMask != 0 && lockedUntilMs == null
}

@HiltViewModel
class ScheduleEditorViewModel @Inject constructor(
    private val scheduleRepository: ScheduleRepository,
    private val installedAppsRepository: InstalledAppsRepository,
    private val categoryRepository: AppCategoryRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(ScheduleEditorUiState())
    val state: StateFlow<ScheduleEditorUiState> = _state.asStateFlow()
    private var allApps: List<InstalledApp> = emptyList()

    /** Add flow from the Block tab: first pick an app. */
    fun openNew() {
        _state.value = ScheduleEditorUiState(isOpen = true, isLoadingApps = true)
        loadApps()
    }

    /** Add flow from the Apps tab lock icon: the app is already chosen. */
    fun openForApp(packageName: String) {
        _state.value = ScheduleEditorUiState(isOpen = true)
        selectApp(packageName)
    }

    fun openEdit(schedule: BlockSchedule) {
        viewModelScope.launch {
            val label = installedAppsRepository.label(schedule.packageName)
            _state.value = ScheduleEditorUiState(
                isOpen = true,
                id = schedule.id,
                packageName = schedule.packageName,
                appLabel = label,
                startMinute = schedule.startMinuteOfDay,
                endMinute = schedule.endMinuteOfDay,
                daysMask = schedule.daysOfWeekMask,
                strict = schedule.strictMode,
                enabled = schedule.enabled,
                lockedUntilMs = StrictLock.lockedUntilMs(schedule),
            )
        }
    }

    fun dismiss() {
        _state.value = ScheduleEditorUiState()
    }

    private fun loadApps() {
        viewModelScope.launch {
            val overrides = categoryRepository.overrides()
            allApps = installedAppsRepository.installedApps().sortedWith(
                compareBy<InstalledApp> { categoryRepository.resolve(it.packageName, overrides) != AppCategory.DISTRACTING }
                    .thenBy { it.label.lowercase() },
            )
            _state.update { it.copy(isLoadingApps = false, apps = filter(it.query)) }
        }
    }

    fun setQuery(query: String) {
        _state.update { it.copy(query = query, apps = filter(query)) }
    }

    private fun filter(query: String): List<InstalledApp> =
        if (query.isBlank()) allApps else allApps.filter { it.label.contains(query.trim(), ignoreCase = true) }

    fun selectApp(packageName: String) {
        viewModelScope.launch {
            val label = installedAppsRepository.label(packageName)
            _state.update { it.copy(packageName = packageName, appLabel = label) }
        }
    }

    fun changeApp() {
        _state.update { it.copy(packageName = null, appLabel = "", isLoadingApps = allApps.isEmpty(), apps = filter(it.query)) }
        if (allApps.isEmpty()) loadApps()
    }

    fun showTimePicker(field: TimeField) {
        if (_state.value.lockedUntilMs == null) _state.update { it.copy(timePickerFor = field) }
    }

    fun hideTimePicker() = _state.update { it.copy(timePickerFor = null) }

    fun setTime(field: TimeField, minuteOfDay: Int) {
        _state.update {
            when (field) {
                TimeField.START -> it.copy(startMinute = minuteOfDay, timePickerFor = null)
                TimeField.END -> it.copy(endMinute = minuteOfDay, timePickerFor = null)
            }
        }
    }

    fun toggleDay(dayIndexFromMonday: Int) {
        if (_state.value.lockedUntilMs != null) return
        _state.update { it.copy(daysMask = it.daysMask xor (1 shl dayIndexFromMonday)) }
    }

    fun setStrict(strict: Boolean) {
        if (_state.value.lockedUntilMs == null) _state.update { it.copy(strict = strict) }
    }

    fun save() {
        val current = _state.value
        val pkg = current.packageName ?: return
        if (!current.canSave) return
        viewModelScope.launch {
            scheduleRepository.save(
                BlockSchedule(
                    id = current.id,
                    packageName = pkg,
                    startMinuteOfDay = current.startMinute,
                    endMinuteOfDay = current.endMinute,
                    daysOfWeekMask = current.daysMask,
                    enabled = current.enabled,
                    strictMode = current.strict,
                ),
            )
            dismiss()
        }
    }

    fun delete() {
        val current = _state.value
        if (!current.isEditing || current.lockedUntilMs != null) return
        viewModelScope.launch {
            scheduleRepository.delete(current.id)
            dismiss()
        }
    }
}
