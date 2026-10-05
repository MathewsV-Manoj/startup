package com.startup.focuno.service.focus

import android.content.Context
import com.startup.focuno.R
import com.startup.focuno.data.local.SettingsStore
import com.startup.focuno.data.repository.FocusSessionRepository
import com.startup.focuno.domain.model.FocusMode
import com.startup.focuno.domain.model.FocusPlan
import com.startup.focuno.service.sound.FocusSoundPlayer
import com.startup.focuno.service.widget.FocusWidget
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Starts and stops focus plans with everything that goes with them: the session rows, the blocker's
 * plan, the background sound, the phase alarms and the home-screen widget. The app and the widget
 * both go through here, so they always behave the same.
 */
@Singleton
class FocusController @Inject constructor(
    @ApplicationContext private val context: Context,
    private val sessions: FocusSessionRepository,
    private val settingsStore: SettingsStore,
    private val soundPlayer: FocusSoundPlayer,
    private val alarms: FocusAlarms,
) {
    /** [amount] is minutes for a timer and rounds for a Pomodoro; a stopwatch ignores it. */
    suspend fun start(mode: FocusMode, amount: Int, strict: Boolean, subject: String) {
        // The pause screen shows this, so "Signals" says more than "Focus session".
        val label = subject.ifBlank { context.getString(R.string.quick_block_label_focus) }
        val now = System.currentTimeMillis()
        val plan = when (mode) {
            FocusMode.TIMER -> FocusPlan(FocusMode.TIMER, now, now + amount * 60_000L)
            FocusMode.POMODORO -> FocusPlan.pomodoro(now, FocusPlan.POMODORO_FOCUS_MIN, FocusPlan.POMODORO_BREAK_MIN, amount)
            FocusMode.STOPWATCH -> FocusPlan(FocusMode.STOPWATCH, now, now + FocusSessionRepository.STOPWATCH_MAX_MS)
        }
        // A stopwatch is meant to be stopped by hand, so it is never strict.
        sessions.start(plan, label, strict && mode != FocusMode.STOPWATCH, subject)
        soundPlayer.play(settingsStore.settings.first().focusSound, untilMs = plan.endMs)
        alarms.scheduleNext(plan, now)
        FocusWidget.refresh(context)
    }

    /** Ends the running plan. Returns false, and changes nothing, while a strict plan is running. */
    suspend fun stop(): Boolean {
        if (!sessions.stopEarly()) return false
        soundPlayer.stop()
        alarms.cancel()
        FocusWidget.refresh(context)
        return true
    }
}
