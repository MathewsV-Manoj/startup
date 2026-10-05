package com.startup.focuno.service.focus

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.startup.focuno.domain.model.FocusPlan
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Wakes the app at the end of each phase of a focus plan, so it can say "break time", "back to focus" or
 * "done" even when the screen is off. Uses an inexact alarm that is allowed while idle, which needs no
 * special permission; Android may deliver it a little late when the phone has been still for a long time.
 */
@Singleton
class FocusAlarms @Inject constructor(@ApplicationContext private val context: Context) {

    private val alarmManager = context.getSystemService(AlarmManager::class.java)

    fun scheduleNext(plan: FocusPlan, nowMs: Long = System.currentTimeMillis()) {
        val phase = plan.phaseAt(nowMs)
        if (phase == null) {
            cancel()
            return
        }
        alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, phase.endsAtMs, pendingIntent())
    }

    fun cancel() {
        alarmManager.cancel(pendingIntent())
    }

    private fun pendingIntent(): PendingIntent = PendingIntent.getBroadcast(
        context,
        REQUEST_CODE,
        Intent(context, FocusAlarmReceiver::class.java),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )

    private companion object {
        const val REQUEST_CODE = 7
    }
}
