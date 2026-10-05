package com.startup.focuno.service.focus

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.startup.focuno.data.local.SettingsStore
import com.startup.focuno.service.monitor.NotificationHelper
import com.startup.focuno.service.widget.FocusWidget
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Fires at the end of a focus phase: tells the person what comes next and sets the following alarm.
 * With [EXTRA_RESCHEDULE_ONLY] (after a reboot or update) it only sets the alarm again.
 */
class FocusAlarmReceiver : BroadcastReceiver() {

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface Deps {
        fun settingsStore(): SettingsStore
        fun focusAlarms(): FocusAlarms
        fun notificationHelper(): NotificationHelper
    }

    override fun onReceive(context: Context, intent: Intent) {
        val deps = EntryPointAccessors.fromApplication(context.applicationContext, Deps::class.java)
        val rescheduleOnly = intent.getBooleanExtra(EXTRA_RESCHEDULE_ONLY, false)
        val pending = goAsync()
        CoroutineScope(Dispatchers.Default).launch {
            try {
                val plan = deps.settingsStore().settings.first().focusPlan ?: return@launch
                val now = System.currentTimeMillis()
                val phase = plan.phaseAt(now)
                if (!rescheduleOnly) {
                    when {
                        phase == null && now >= plan.endMs && now - plan.endMs < STALE_MS -> deps.notificationHelper().notifyFocusDone()
                        phase == null -> Unit
                        phase.focusing -> deps.notificationHelper().notifyBackToFocus(phase.round, phase.rounds)
                        else -> deps.notificationHelper().notifyBreak(phase.round, phase.rounds, phase.endsAtMs - phase.startsAtMs)
                    }
                }
                deps.focusAlarms().scheduleNext(plan, now)
                // The widget's countdown runs to the end of a phase, so it needs the next phase now.
                FocusWidget.refresh(context)
            } catch (e: Exception) {
                Log.w(TAG, "Focus alarm failed", e)
            } finally {
                pending.finish()
            }
        }
    }

    companion object {
        private const val TAG = "FocusAlarm"
        const val EXTRA_RESCHEDULE_ONLY = "reschedule_only"

        /** Do not announce a plan that ended long ago (for example after the phone was off). */
        private const val STALE_MS = 10 * 60_000L

        fun reschedule(context: Context) {
            context.sendBroadcast(Intent(context, FocusAlarmReceiver::class.java).putExtra(EXTRA_RESCHEDULE_ONLY, true))
        }
    }
}
