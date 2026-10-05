package com.startup.focuno.ui.screens.block

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.startup.focuno.data.local.SettingsStore
import com.startup.focuno.data.repository.AppCategoryRepository
import com.startup.focuno.data.repository.InstalledApp
import com.startup.focuno.data.repository.InstalledAppsRepository
import com.startup.focuno.data.repository.LimitRepository
import com.startup.focuno.data.repository.ProtectionRepository
import com.startup.focuno.data.repository.ScheduleRepository
import com.startup.focuno.domain.model.AppCategory
import com.startup.focuno.domain.model.AppLimit
import com.startup.focuno.domain.model.BlockSchedule
import com.startup.focuno.domain.model.BlockScope
import com.startup.focuno.domain.model.ShortVideoApps
import com.startup.focuno.domain.usecase.StrictLock
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class TimeField { START, END }

/**
 * The add flow is a few big choices in a row. Editing an existing schedule jumps straight to DETAILS,
 * and editing a daily limit jumps straight to LIMIT.
 */
enum class EditorStep { PICK_APP, WHAT, WHEN, DETAILS, LIMIT }

/** Daily budgets offered as one-tap pills, in minutes. 0 means no time limit (opens only). */
val LIMIT_CHOICES = listOf(0, 15, 30, 60, 90, 120)

/** Daily open caps offered as pills. 0 means no cap. */
val OPEN_CHOICES = listOf(0, 3, 5, 10, 20)

/** One-tap times for the add flow. start == end means all day. */
enum class WhenPreset(val startMinute: Int, val endMinute: Int) {
    ALL_DAY(0, 0),
    NIGHT(22 * 60, 6 * 60),
    SCHOOL(9 * 60, 17 * 60),
    EVENING(18 * 60, 23 * 60),
}

data class ScheduleEditorUiState(
    val isOpen: Boolean = false,
    val step: EditorStep = EditorStep.PICK_APP,
    val isLoadingApps: Boolean = false,
    val apps: List<InstalledApp> = emptyList(),
    val query: String = "",
    val id: Long = 0,
    val packageName: String? = null,
    val appLabel: String = "",
    val scope: BlockScope = BlockScope.APP,
    val startMinute: Int = 22 * 60,
    val endMinute: Int = 6 * 60,
    val daysMask: Int = BlockSchedule.ALL_DAYS,
    val strict: Boolean = false,
    val enabled: Boolean = true,
    val timePickerFor: TimeField? = null,
    /** When non-null the schedule is a strict window that is open right now and cannot be changed. */
    val lockedUntilMs: Long? = null,
    val limitMinutes: Int = 30,
    val limitOpens: Int = 0,
    /** True while editing a daily limit that already exists. */
    val isEditingLimit: Boolean = false,
    /** Reels/Shorts-only blocking needs Accessibility; basic mode cannot see inside an app. */
    val accessibilityOn: Boolean = true,
) {
    val isEditing: Boolean get() = id != 0L
    val isOvernight: Boolean get() = endMinute <= startMinute
    val strictAllowed: Boolean get() = StrictLock.allowsStrict(startMinute, endMinute, daysMask)
    val canSaveLimit: Boolean get() = lockedUntilMs == null && (limitMinutes > 0 || limitOpens > 0)
    val canSave: Boolean get() = packageName != null && daysMask != 0 && lockedUntilMs == null && (!strict || strictAllowed)
    val supportsShortVideo: Boolean get() = ShortVideoApps.supports(packageName)
}

@HiltViewModel
class ScheduleEditorViewModel @Inject constructor(
    private val scheduleRepository: ScheduleRepository,
    private val limitRepository: LimitRepository,
    private val installedAppsRepository: InstalledAppsRepository,
    private val categoryRepository: AppCategoryRepository,
    private val protectionRepository: ProtectionRepository,
    private val settingsStore: SettingsStore,
) : ViewModel() {

    private val _state = MutableStateFlow(ScheduleEditorUiState())
    val state: StateFlow<ScheduleEditorUiState> = _state.asStateFlow()
    private var allApps: List<InstalledApp> = emptyList()

    /** Add flow from the Block tab: first pick an app. */
    fun openNew() {
        _state.value = ScheduleEditorUiState(isOpen = true, step = EditorStep.PICK_APP, isLoadingApps = true)
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
                step = EditorStep.DETAILS,
                id = schedule.id,
                packageName = schedule.packageName,
                appLabel = label,
                scope = schedule.scope,
                startMinute = schedule.startMinuteOfDay,
                endMinute = schedule.endMinuteOfDay,
                daysMask = schedule.daysOfWeekMask,
                strict = schedule.strictMode,
                enabled = schedule.enabled,
                lockedUntilMs = StrictLock.lockedUntilMs(schedule),
            )
        }
    }

    /** Opens an existing daily limit. [lockedUntilMs] is set while a used-up strict limit cannot change. */
    fun openEditLimit(limit: AppLimit, lockedUntilMs: Long?) {
        viewModelScope.launch {
            val label = installedAppsRepository.label(limit.packageName)
            _state.value = ScheduleEditorUiState(
                isOpen = true,
                step = EditorStep.LIMIT,
                packageName = limit.packageName,
                appLabel = label,
                limitMinutes = limit.dailyMinutes,
                limitOpens = limit.maxOpens,
                strict = limit.strict,
                enabled = limit.enabled,
                lockedUntilMs = lockedUntilMs,
                isEditingLimit = true,
            )
        }
    }

    /** "Pause before opening": a breathing pause each time the app opens, instead of a block. */
    fun chooseMindful() {
        val pkg = _state.value.packageName ?: return
        viewModelScope.launch {
            settingsStore.setMindful(pkg, true)
            dismiss()
        }
    }

    fun chooseLimit() {
        _state.update { it.copy(step = EditorStep.LIMIT, scope = BlockScope.APP) }
    }

    fun setLimitMinutes(minutes: Int) {
        if (_state.value.lockedUntilMs == null) _state.update { it.copy(limitMinutes = minutes) }
    }

    fun setLimitOpens(opens: Int) {
        if (_state.value.lockedUntilMs == null) _state.update { it.copy(limitOpens = opens) }
    }

    fun saveLimit() {
        val current = _state.value
        val pkg = current.packageName ?: return
        if (!current.canSaveLimit) return
        viewModelScope.launch {
            limitRepository.save(AppLimit(pkg, current.limitMinutes, current.strict, current.enabled, current.limitOpens))
            dismiss()
        }
    }

    fun deleteLimit() {
        val current = _state.value
        val pkg = current.packageName ?: return
        if (!current.isEditingLimit || current.lockedUntilMs != null) return
        viewModelScope.launch {
            limitRepository.delete(pkg)
            dismiss()
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
            val accessibilityOn = protectionRepository.isAccessibilityServiceEnabled()
            _state.update {
                it.copy(
                    accessibilityOn = accessibilityOn,
                    packageName = packageName,
                    appLabel = label,
                    scope = BlockScope.APP,
                    step = if (ShortVideoApps.supports(packageName)) EditorStep.WHAT else EditorStep.WHEN,
                )
            }
        }
    }

    fun chooseScope(scope: BlockScope) {
        _state.update { it.copy(scope = scope, step = EditorStep.WHEN) }
    }

    /** Saves straight away for every day, strict if the Strict switch above the presets is on. */
    fun choosePreset(preset: WhenPreset) {
        val current = _state.value
        val pkg = current.packageName ?: return
        if (current.strict && !StrictLock.allowsStrict(preset.startMinute, preset.endMinute, BlockSchedule.ALL_DAYS)) return
        viewModelScope.launch {
            scheduleRepository.save(
                BlockSchedule(
                    packageName = pkg,
                    scope = current.scope,
                    startMinuteOfDay = preset.startMinute,
                    endMinuteOfDay = preset.endMinute,
                    daysOfWeekMask = BlockSchedule.ALL_DAYS,
                    strictMode = current.strict,
                ),
            )
            dismiss()
        }
    }

    fun chooseCustomTime() {
        _state.update { it.copy(step = EditorStep.DETAILS) }
    }

    fun stepBack() {
        val current = _state.value
        when (current.step) {
            EditorStep.PICK_APP -> dismiss()
            EditorStep.WHAT -> changeApp()
            EditorStep.WHEN -> if (current.supportsShortVideo) {
                _state.update { it.copy(step = EditorStep.WHAT) }
            } else {
                changeApp()
            }
            EditorStep.DETAILS -> if (current.isEditing) dismiss() else _state.update { it.copy(step = EditorStep.WHEN) }
            EditorStep.LIMIT -> if (current.isEditingLimit) dismiss() else _state.update { it.copy(step = EditorStep.WHEN) }
        }
    }

    fun changeApp() {
        _state.update { it.copy(packageName = null, appLabel = "", step = EditorStep.PICK_APP, isLoadingApps = allApps.isEmpty(), apps = filter(it.query)) }
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
                    scope = current.scope,
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
