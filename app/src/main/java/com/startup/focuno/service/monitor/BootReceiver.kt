package com.startup.focuno.service.monitor

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.startup.focuno.service.focus.FocusAlarmReceiver
import com.startup.focuno.service.worker.WorkScheduler

/** Restarts monitoring after a reboot or an app update. The accessibility service is restarted by Android itself. */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED, Intent.ACTION_MY_PACKAGE_REPLACED -> {
                MonitorServiceLauncher.start(context)
                WorkScheduler.schedule(context)
                // Alarms do not survive a reboot; a running focus plan needs its next one back.
                FocusAlarmReceiver.reschedule(context)
            }
        }
    }
}
