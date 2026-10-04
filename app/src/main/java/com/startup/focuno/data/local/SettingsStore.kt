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
        val streakCount = intPreferencesKey("streak_count")
        val lastActiveDate = stringPreferencesKey("last_active_date")
        val quickBlockUntil = longPreferencesKey("quick_block_until")
        val quickBlockLabel = stringPreferencesKey("quick_block_label")
        val quickBlockStrict = booleanPreferencesKey("quick_block_strict")
        val lastNudgeAt = longPreferencesKey("last_nudge_at")
        val nudgeDay = stringPreferencesKey("nudge_day")
        val nudgeCountToday = intPreferencesKey("nudge_count_today")
        val goalThresholdsHit = stringSetPreferencesKey("goal_thresholds_hit")
        val xpBanked = intPreferencesKey("xp_banked")
        val xpBankedThrough = stringPreferencesKey("xp_banked_through")
        val badgesUnlocked = stringSetPreferencesKey("badges_unlocked")
        val celebratedLevel = intPreferencesKey("celebrated_level")
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
                streakCount = prefs[Keys.streakCount] ?: defaults.streakCount,
                lastActiveDate = prefs[Keys.lastActiveDate],
                quickBlockUntilMs = prefs[Keys.quickBlockUntil] ?: defaults.quickBlockUntilMs,
                quickBlockLabel = prefs[Keys.quickBlockLabel] ?: defaults.quickBlockLabel,
                quickBlockStrict = prefs[Keys.quickBlockStrict] ?: defaults.quickBlockStrict,
                lastNudgeAtMs = prefs[Keys.lastNudgeAt] ?: defaults.lastNudgeAtMs,
                nudgeDay = prefs[Keys.nudgeDay] ?: defaults.nudgeDay,
                nudgeCountToday = prefs[Keys.nudgeCountToday] ?: defaults.nudgeCountToday,
                goalThresholdsHit = prefs[Keys.goalThresholdsHit] ?: defaults.goalThresholdsHit,
                xpBanked = prefs[Keys.xpBanked] ?: defaults.xpBanked,
                xpBankedThrough = prefs[Keys.xpBankedThrough],
                badgesUnlocked = prefs[Keys.badgesUnlocked] ?: defaults.badgesUnlocked,
                celebratedLevel = prefs[Keys.celebratedLevel] ?: defaults.celebratedLevel,
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

    suspend fun setStreak(count: Int, lastActiveDate: String?) {
        context.focunoDataStore.edit {
            it[Keys.streakCount] = count
            if (lastActiveDate != null) it[Keys.lastActiveDate] = lastActiveDate else it.remove(Keys.lastActiveDate)
        }
    }

    suspend fun setQuickBlock(untilMs: Long, label: String, strict: Boolean = false) {
        context.focunoDataStore.edit {
            it[Keys.quickBlockUntil] = untilMs
            it[Keys.quickBlockLabel] = label
            it[Keys.quickBlockStrict] = strict
        }
    }

    suspend fun clearQuickBlock() = setQuickBlock(0L, "", strict = false)

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

    suspend fun setCelebrated(level: Int, badges: Set<String>) {
        context.focunoDataStore.edit {
            it[Keys.celebratedLevel] = level
            it[Keys.badgesUnlocked] = badges
        }
    }
}
