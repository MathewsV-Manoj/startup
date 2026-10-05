package com.startup.focuno.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.startup.focuno.data.model.AppSettings
import com.startup.focuno.domain.model.FocusMode
import com.startup.focuno.domain.model.FocusPlan
import com.startup.focuno.domain.model.FocusSound
import com.startup.focuno.domain.model.NudgeSensitivity
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

private val Context.focunoDataStore: DataStore<Preferences> by preferencesDataStore(name = "focuno_settings")

@Singleton
class SettingsStore @Inject constructor(@ApplicationContext private val context: Context) {

    private object Keys {
        val onboardingCompleted = booleanPreferencesKey("onboarding_completed")
        val onboardingStep = intPreferencesKey("onboarding_step")
        val nudgesEnabled = booleanPreferencesKey("nudges_enabled")
        val nudgeSensitivity = stringPreferencesKey("nudge_sensitivity")
        val theme = stringPreferencesKey("theme")
        val dailyGoalMinutes = intPreferencesKey("daily_goal_minutes")
        val studyGoalMinutes = intPreferencesKey("study_goal_minutes")
        val examName = stringPreferencesKey("exam_name")
        val examDate = stringPreferencesKey("exam_date")
        val streakCount = intPreferencesKey("streak_count")
        val lastActiveDate = stringPreferencesKey("last_active_date")
        val quickBlockUntil = longPreferencesKey("quick_block_until")
        val quickBlockLabel = stringPreferencesKey("quick_block_label")
        val quickBlockStrict = booleanPreferencesKey("quick_block_strict")
        val quickBlockStart = longPreferencesKey("quick_block_start")
        val focusMode = stringPreferencesKey("focus_mode")
        val focusRoundMs = longPreferencesKey("focus_round_ms")
        val focusBreakMs = longPreferencesKey("focus_break_ms")
        val lastNudgeAt = longPreferencesKey("last_nudge_at")
        val nudgeDay = stringPreferencesKey("nudge_day")
        val nudgeCountToday = intPreferencesKey("nudge_count_today")
        val goalThresholdsHit = stringSetPreferencesKey("goal_thresholds_hit")
        val xpBanked = intPreferencesKey("xp_banked")
        val xpBankedThrough = stringPreferencesKey("xp_banked_through")
        val badgesUnlocked = stringSetPreferencesKey("badges_unlocked")
        val celebratedLevel = intPreferencesKey("celebrated_level")
        val subjects = stringPreferencesKey("subjects")
        val focusSound = stringPreferencesKey("focus_sound")
        val weeklyReportWeek = stringPreferencesKey("weekly_report_week")
        val budgetMinutes = intPreferencesKey("budget_minutes")
        val budgetStrict = booleanPreferencesKey("budget_strict")
        val focusLockAll = booleanPreferencesKey("focus_lock_all")
        val focusAllowed = stringSetPreferencesKey("focus_allowed")
    }

    val settings: Flow<AppSettings> = context.focunoDataStore.data
        .catch { error -> if (error is IOException) emit(emptyPreferences()) else throw error }
        .map { prefs ->
            val defaults = AppSettings()
            AppSettings(
                onboardingCompleted = prefs[Keys.onboardingCompleted] ?: defaults.onboardingCompleted,
                onboardingStep = prefs[Keys.onboardingStep] ?: defaults.onboardingStep,
                nudgesEnabled = prefs[Keys.nudgesEnabled] ?: defaults.nudgesEnabled,
                nudgeSensitivity = prefs[Keys.nudgeSensitivity]
                    ?.let { name -> NudgeSensitivity.entries.firstOrNull { it.name == name } }
                    ?: defaults.nudgeSensitivity,
                theme = prefs[Keys.theme] ?: defaults.theme,
                dailyGoalMinutes = prefs[Keys.dailyGoalMinutes] ?: defaults.dailyGoalMinutes,
                studyGoalMinutes = prefs[Keys.studyGoalMinutes] ?: defaults.studyGoalMinutes,
                examName = prefs[Keys.examName] ?: defaults.examName,
                examDate = prefs[Keys.examDate],
                streakCount = prefs[Keys.streakCount] ?: defaults.streakCount,
                lastActiveDate = prefs[Keys.lastActiveDate],
                quickBlockUntilMs = prefs[Keys.quickBlockUntil] ?: defaults.quickBlockUntilMs,
                quickBlockLabel = prefs[Keys.quickBlockLabel] ?: defaults.quickBlockLabel,
                quickBlockStrict = prefs[Keys.quickBlockStrict] ?: defaults.quickBlockStrict,
                quickBlockStartMs = prefs[Keys.quickBlockStart] ?: defaults.quickBlockStartMs,
                focusMode = prefs[Keys.focusMode]
                    ?.let { name -> FocusMode.entries.firstOrNull { it.name == name } }
                    ?: defaults.focusMode,
                focusRoundMs = prefs[Keys.focusRoundMs] ?: defaults.focusRoundMs,
                focusBreakMs = prefs[Keys.focusBreakMs] ?: defaults.focusBreakMs,
                lastNudgeAtMs = prefs[Keys.lastNudgeAt] ?: defaults.lastNudgeAtMs,
                nudgeDay = prefs[Keys.nudgeDay] ?: defaults.nudgeDay,
                nudgeCountToday = prefs[Keys.nudgeCountToday] ?: defaults.nudgeCountToday,
                goalThresholdsHit = prefs[Keys.goalThresholdsHit] ?: defaults.goalThresholdsHit,
                xpBanked = prefs[Keys.xpBanked] ?: defaults.xpBanked,
                xpBankedThrough = prefs[Keys.xpBankedThrough],
                badgesUnlocked = prefs[Keys.badgesUnlocked] ?: defaults.badgesUnlocked,
                celebratedLevel = prefs[Keys.celebratedLevel] ?: defaults.celebratedLevel,
                focusSound = prefs[Keys.focusSound]
                    ?.let { name -> FocusSound.entries.firstOrNull { it.name == name } }
                    ?: defaults.focusSound,
                weeklyReportWeek = prefs[Keys.weeklyReportWeek],
                budgetMinutes = prefs[Keys.budgetMinutes] ?: defaults.budgetMinutes,
                budgetStrict = prefs[Keys.budgetStrict] ?: defaults.budgetStrict,
                focusLockAll = prefs[Keys.focusLockAll] ?: defaults.focusLockAll,
                focusAllowed = prefs[Keys.focusAllowed] ?: defaults.focusAllowed,
                subjects = prefs[Keys.subjects]?.split(SUBJECT_SEPARATOR)?.filter { it.isNotBlank() } ?: defaults.subjects,
            )
        }

    suspend fun setOnboardingStep(step: Int) {
        context.focunoDataStore.edit { it[Keys.onboardingStep] = step }
    }

    suspend fun setOnboardingCompleted(completed: Boolean) {
        context.focunoDataStore.edit { it[Keys.onboardingCompleted] = completed }
    }

    suspend fun setNudgesEnabled(enabled: Boolean) {
        context.focunoDataStore.edit { it[Keys.nudgesEnabled] = enabled }
    }

    suspend fun setNudgeSensitivity(sensitivity: NudgeSensitivity) {
        context.focunoDataStore.edit { it[Keys.nudgeSensitivity] = sensitivity.name }
    }

    suspend fun setDailyGoalMinutes(minutes: Int) {
        context.focunoDataStore.edit { it[Keys.dailyGoalMinutes] = minutes }
    }

    /** A null [date] clears the countdown. */
    suspend fun setExam(name: String, date: String?) {
        context.focunoDataStore.edit {
            it[Keys.examName] = name
            if (date != null) it[Keys.examDate] = date else it.remove(Keys.examDate)
        }
    }

    suspend fun setStudyGoalMinutes(minutes: Int) {
        context.focunoDataStore.edit { it[Keys.studyGoalMinutes] = minutes }
    }

    suspend fun setStreak(count: Int, lastActiveDate: String?) {
        context.focunoDataStore.edit {
            it[Keys.streakCount] = count
            if (lastActiveDate != null) it[Keys.lastActiveDate] = lastActiveDate else it.remove(Keys.lastActiveDate)
        }
    }

    /** Saves the focus plan that is starting. The blocker pauses time-eater apps during its focus rounds. */
    suspend fun setFocusPlan(plan: FocusPlan, label: String, strict: Boolean) {
        context.focunoDataStore.edit {
            it[Keys.quickBlockStart] = plan.startMs
            it[Keys.quickBlockUntil] = plan.endMs
            it[Keys.focusMode] = plan.mode.name
            it[Keys.focusRoundMs] = plan.roundMs
            it[Keys.focusBreakMs] = plan.breakMs
            it[Keys.quickBlockLabel] = label
            it[Keys.quickBlockStrict] = strict
        }
    }

    suspend fun clearQuickBlock() {
        context.focunoDataStore.edit {
            it[Keys.quickBlockStart] = 0L
            it[Keys.quickBlockUntil] = 0L
            it[Keys.focusMode] = FocusMode.TIMER.name
            it[Keys.focusRoundMs] = 0L
            it[Keys.focusBreakMs] = 0L
            it[Keys.quickBlockLabel] = ""
            it[Keys.quickBlockStrict] = false
        }
    }

    suspend fun recordNudge(nowMs: Long, day: String, countToday: Int) {
        context.focunoDataStore.edit {
            it[Keys.lastNudgeAt] = nowMs
            it[Keys.nudgeDay] = day
            it[Keys.nudgeCountToday] = countToday
        }
    }

    suspend fun addGoalThresholdHit(entry: String, keepDay: String) {
        context.focunoDataStore.edit { prefs ->
            val current = prefs[Keys.goalThresholdsHit].orEmpty().filter { it.startsWith("$keepDay:") }.toSet()
            prefs[Keys.goalThresholdsHit] = current + entry
        }
    }

    /** Adds a finished day's points once, and remembers which day was the last one counted. */
    suspend fun bankXp(amount: Int, throughDay: String) {
        context.focunoDataStore.edit {
            it[Keys.xpBanked] = (it[Keys.xpBanked] ?: 0) + amount
            it[Keys.xpBankedThrough] = throughDay
        }
    }

    suspend fun setFocusLockAll(enabled: Boolean) {
        context.focunoDataStore.edit { it[Keys.focusLockAll] = enabled }
    }

    suspend fun setFocusAllowed(packages: Set<String>) {
        context.focunoDataStore.edit { it[Keys.focusAllowed] = packages }
    }

    suspend fun setTimeEaterBudget(minutes: Int, strict: Boolean) {
        context.focunoDataStore.edit {
            it[Keys.budgetMinutes] = minutes
            it[Keys.budgetStrict] = strict
        }
    }

    suspend fun setWeeklyReportWeek(weekStart: String) {
        context.focunoDataStore.edit { it[Keys.weeklyReportWeek] = weekStart }
    }

    suspend fun setFocusSound(sound: FocusSound) {
        context.focunoDataStore.edit { it[Keys.focusSound] = sound.name }
    }

    /** Stored as one string so the order the person chose is kept. */
    suspend fun setSubjects(subjects: List<String>) {
        context.focunoDataStore.edit { it[Keys.subjects] = subjects.joinToString(SUBJECT_SEPARATOR) }
    }

    suspend fun setCelebrated(level: Int, badges: Set<String>) {
        context.focunoDataStore.edit {
            it[Keys.celebratedLevel] = level
            it[Keys.badgesUnlocked] = badges
        }
    }

    private companion object {
        const val SUBJECT_SEPARATOR = "\n"
    }
}
