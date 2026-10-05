package com.startup.focuno.ui.screens.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.startup.focuno.data.local.SettingsStore
import com.startup.focuno.data.model.AppSettings
import com.startup.focuno.data.repository.AppCategoryRepository
import com.startup.focuno.data.repository.InstalledApp
import com.startup.focuno.data.repository.InstalledAppsRepository
import com.startup.focuno.data.repository.ProtectionRepository
import com.startup.focuno.data.repository.ProtectionStatus
import com.startup.focuno.data.repository.ScheduleRepository
import com.startup.focuno.domain.model.AppCategory
import com.startup.focuno.domain.model.BlockSchedule
import com.startup.focuno.service.accessibility.FocunoAccessibilityService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

/** The two permissions blocking needs come first; Accessibility is optional, so it follows them. */
enum class OnboardingStep { PURPOSE, USAGE_ACCESS, OVERLAY, ACCESSIBILITY, STAY_ALIVE, AUTOSTART, PICK_APPS, FIRST_SCHEDULE }

enum class SchedulePreset(val startMinute: Int, val endMinute: Int) {
    NIGHT(22 * 60, 6 * 60),
    STUDY_DAY(9 * 60, 17 * 60),
    EVENING(18 * 60, 23 * 60),
}

data class OnboardingUiState(
    val isLoading: Boolean = true,
    val step: OnboardingStep = OnboardingStep.PURPOSE,
    val status: ProtectionStatus? = null,
    val accessibilityEnabledButIdle: Boolean = false,
    val showRestrictedHelp: Boolean = false,
    val appsLoading: Boolean = false,
    val apps: List<InstalledApp> = emptyList(),
    val query: String = "",
    val selected: Set<String> = emptySet(),
    val preset: SchedulePreset = SchedulePreset.NIGHT,
    val isFinishing: Boolean = false,
) {
    val stepNumber: Int get() = step.ordinal + 1
    val stepCount: Int get() = OnboardingStep.entries.size
    val visibleApps: List<InstalledApp>
        get() = if (query.isBlank()) apps else apps.filter { it.label.contains(query.trim(), ignoreCase = true) }
}

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val settingsStore: SettingsStore,
    private val protectionRepository: ProtectionRepository,
    private val installedAppsRepository: InstalledAppsRepository,
    private val categoryRepository: AppCategoryRepository,
    private val scheduleRepository: ScheduleRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(OnboardingUiState())
    val state: StateFlow<OnboardingUiState> = _state.asStateFlow()

    private var overrides: Map<String, AppCategory> = emptyMap()
    private var originallyDistracting: Set<String> = emptySet()

    init {
        viewModelScope.launch {
            // Resume where the person left off if the app was closed or killed mid-setup.
            val saved = settingsStore.settings.first().onboardingStep
            val step = OnboardingStep.entries.getOrElse(saved) { OnboardingStep.PURPOSE }
            _state.update { it.copy(isLoading = false, step = step) }
            if (step == OnboardingStep.PICK_APPS || step == OnboardingStep.FIRST_SCHEDULE) loadApps()
            refreshStatus()
        }
    }

    fun refreshStatus() {
        viewModelScope.launch {
            val status = withContext(Dispatchers.IO) { protectionRepository.status() }
            _state.update {
                it.copy(
                    status = status,
                    accessibilityEnabledButIdle = status.accessibilityEnabled && !FocunoAccessibilityService.isConnected,
                    showRestrictedHelp = !status.accessibilityEnabled && protectionRepository.likelyNeedsRestrictedSettingUnlock(),
                )
            }
        }
    }

    fun next() {
        val current = _state.value.step
        val target = OnboardingStep.entries.getOrNull(current.ordinal + 1) ?: return
        goTo(target)
    }

    fun back() {
        val target = OnboardingStep.entries.getOrNull(_state.value.step.ordinal - 1) ?: return
        goTo(target)
    }

    private fun goTo(target: OnboardingStep) {
        _state.update { it.copy(step = target) }
        viewModelScope.launch { settingsStore.setOnboardingStep(target.ordinal) }
        if ((target == OnboardingStep.PICK_APPS || target == OnboardingStep.FIRST_SCHEDULE) && _state.value.apps.isEmpty()) loadApps()
        refreshStatus()
    }

    private fun loadApps() {
        _state.update { it.copy(appsLoading = true) }
        viewModelScope.launch {
            overrides = categoryRepository.overrides()
            val apps = installedAppsRepository.installedApps()
            originallyDistracting = apps.filter { categoryRepository.resolve(it.packageName, overrides) == AppCategory.DISTRACTING }
                .mapTo(HashSet()) { it.packageName }
            _state.update {
                it.copy(
                    appsLoading = false,
                    apps = apps.sortedWith(
                        compareBy<InstalledApp>({ app -> app.packageName !in originallyDistracting }, { app -> app.label.lowercase() }),
                    ),
                    selected = if (it.selected.isEmpty()) originallyDistracting else it.selected,
                )
            }
        }
    }

    fun setQuery(query: String) = _state.update { it.copy(query = query) }

    fun toggleApp(packageName: String) {
        _state.update {
            it.copy(selected = if (packageName in it.selected) it.selected - packageName else it.selected + packageName)
        }
    }

    fun setPreset(preset: SchedulePreset) = _state.update { it.copy(preset = preset) }

    /** Saves the app picks as category overrides (only where they differ from the starting guess), then moves on. */
    fun confirmApps() {
        val selected = _state.value.selected
        viewModelScope.launch {
            val changes = HashMap<String, AppCategory>()
            _state.value.apps.forEach { app ->
                val wantsDistracting = app.packageName in selected
                val wasDistracting = app.packageName in originallyDistracting
                if (wantsDistracting != wasDistracting) {
                    changes[app.packageName] = if (wantsDistracting) AppCategory.DISTRACTING else AppCategory.NEUTRAL
                }
            }
            if (changes.isNotEmpty()) categoryRepository.setCategories(changes)
            next()
        }
    }

    fun finish(createSchedule: Boolean) {
        val current = _state.value
        if (current.isFinishing) return
        _state.update { it.copy(isFinishing = true) }
        viewModelScope.launch {
            if (createSchedule && current.selected.isNotEmpty()) {
                scheduleRepository.saveAll(
                    current.selected.map { pkg ->
                        BlockSchedule(
                            packageName = pkg,
                            startMinuteOfDay = current.preset.startMinute,
                            endMinuteOfDay = current.preset.endMinute,
                            daysOfWeekMask = BlockSchedule.ALL_DAYS,
                        )
                    },
                )
            }
            // Everything is new to someone who just set up, so there is no "What's new" to show them.
            settingsStore.setWhatsNewSeen(AppSettings.WHATS_NEW_VERSION)
            settingsStore.setOnboardingCompleted(true)
        }
    }
}
