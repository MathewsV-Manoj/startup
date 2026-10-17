package com.focuno.core.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.focuno.core.model.AppCategory
import com.focuno.core.model.UserFocusProfile
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Typed access to Focuno's local preferences. Owns the `UserFocusProfile` and
 * user category overrides. No network, ever.
 */
@Singleton
class FocunoPreferences @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) {
    val profile: Flow<UserFocusProfile> = dataStore.data.map { prefs ->
        UserFocusProfile(
            onboardingCompleted = prefs[ONBOARDED] ?: false,
            dailyFocusGoalMinutes = prefs[GOAL_MINUTES] ?: 120,
            nudgesEnabled = prefs[NUDGES] ?: true,
            deepWorkThresholdMinutes = prefs[DEEP_WORK_THRESHOLD] ?: 25,
            consistencyWindowDays = prefs[CONSISTENCY_WINDOW] ?: 7,
        )
    }

    suspend fun current(): UserFocusProfile = profile.first()

    suspend fun update(profile: UserFocusProfile) {
        dataStore.edit { prefs ->
            prefs[ONBOARDED] = profile.onboardingCompleted
            prefs[GOAL_MINUTES] = profile.dailyFocusGoalMinutes
            prefs[NUDGES] = profile.nudgesEnabled
            prefs[DEEP_WORK_THRESHOLD] = profile.deepWorkThresholdMinutes
            prefs[CONSISTENCY_WINDOW] = profile.consistencyWindowDays
        }
    }

    val categoryOverrides: Flow<Map<String, AppCategory>> = dataStore.data.map { prefs ->
        decodeOverrides(prefs[CATEGORY_OVERRIDES].orEmpty())
    }

    suspend fun setCategoryOverride(packageName: String, category: AppCategory) {
        dataStore.edit { prefs ->
            val current = decodeOverrides(prefs[CATEGORY_OVERRIDES].orEmpty()).toMutableMap()
            current[packageName] = category
            prefs[CATEGORY_OVERRIDES] = encodeOverrides(current)
        }
    }

    private fun decodeOverrides(raw: String): Map<String, AppCategory> =
        raw.split(';')
            .mapNotNull { entry ->
                val parts = entry.split('=')
                if (parts.size != 2) return@mapNotNull null
                val category = runCatching { AppCategory.valueOf(parts[1]) }.getOrNull()
                    ?: return@mapNotNull null
                parts[0] to category
            }
            .toMap()

    private fun encodeOverrides(map: Map<String, AppCategory>): String =
        map.entries.joinToString(";") { "${it.key}=${it.value.name}" }

    private companion object {
        val ONBOARDED = booleanPreferencesKey("onboarded")
        val GOAL_MINUTES = intPreferencesKey("daily_goal_minutes")
        val NUDGES = booleanPreferencesKey("nudges_enabled")
        val DEEP_WORK_THRESHOLD = intPreferencesKey("deep_work_threshold_minutes")
        val CONSISTENCY_WINDOW = intPreferencesKey("consistency_window_days")
        val CATEGORY_OVERRIDES = stringPreferencesKey("category_overrides")
    }
}
