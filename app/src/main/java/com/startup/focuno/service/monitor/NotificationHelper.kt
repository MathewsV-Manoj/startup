package com.startup.focuno.service.monitor

import android.annotation.SuppressLint
import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.startup.focuno.R
import com.startup.focuno.data.repository.ProtectionIssue
import com.startup.focuno.data.repository.ProtectionStatus
import com.startup.focuno.ui.MainActivity
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationHelper @Inject constructor(@ApplicationContext private val context: Context) {

    private val manager = NotificationManagerCompat.from(context)
    private var lastProtectionIssues: List<ProtectionIssue>? = null

    fun ensureChannels() {
        manager.createNotificationChannel(
            NotificationChannelCompat.Builder(CHANNEL_MONITOR, NotificationManagerCompat.IMPORTANCE_LOW)
                .setName(context.getString(R.string.channel_monitor_name))
                .setDescription(context.getString(R.string.channel_monitor_description))
                .setShowBadge(false)
                .build(),
        )
        manager.createNotificationChannel(
            NotificationChannelCompat.Builder(CHANNEL_FOCUS, NotificationManagerCompat.IMPORTANCE_HIGH)
                .setName(context.getString(R.string.channel_focus_name))
                .setDescription(context.getString(R.string.channel_focus_description))
                .build(),
        )
        manager.createNotificationChannel(
            NotificationChannelCompat.Builder(CHANNEL_PROTECTION, NotificationManagerCompat.IMPORTANCE_HIGH)
                .setName(context.getString(R.string.channel_protection_name))
                .setDescription(context.getString(R.string.channel_protection_description))
                .build(),
        )
    }

    fun monitorNotification(scoreText: String): Notification =
        NotificationCompat.Builder(context, CHANNEL_MONITOR)
            .setSmallIcon(R.drawable.ic_stat_focuno)
            .setContentTitle(context.getString(R.string.monitor_notification_title))
            .setContentText(context.getString(R.string.monitor_notification_text, scoreText))
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .setContentIntent(openAppIntent(openHealth = false))
            .build()

    @SuppressLint("MissingPermission")
    fun updateMonitor(scoreText: String) {
        if (manager.areNotificationsEnabled()) manager.notify(MONITOR_ID, monitorNotification(scoreText))
    }

    /** Shows "protection is OFF" the moment blocking stops working, and clears it once it works again. */
    @SuppressLint("MissingPermission")
    fun updateProtection(status: ProtectionStatus) {
        val blockingIssues = status.blockingIssues
        if (blockingIssues == lastProtectionIssues) return
        lastProtectionIssues = blockingIssues

        if (blockingIssues.isEmpty()) {
            manager.cancel(PROTECTION_ID)
            return
        }
        if (!manager.areNotificationsEnabled()) return
        val missing = blockingIssues.joinToString(", ") { issue ->
            context.getString(
                when (issue) {
                    ProtectionIssue.ACCESSIBILITY -> R.string.issue_accessibility
                    ProtectionIssue.OVERLAY -> R.string.issue_overlay
                    ProtectionIssue.USAGE_ACCESS -> R.string.issue_usage_access
                    ProtectionIssue.NOTIFICATIONS -> R.string.issue_notifications
                    ProtectionIssue.BATTERY -> R.string.issue_battery
                    ProtectionIssue.BACKGROUND_SERVICE -> R.string.issue_background
                },
            )
        }
        val notification = NotificationCompat.Builder(context, CHANNEL_PROTECTION)
            .setSmallIcon(R.drawable.ic_stat_focuno)
            .setContentTitle(context.getString(R.string.protection_off_title))
            .setContentText(context.getString(R.string.protection_off_text, missing))
            .setStyle(NotificationCompat.BigTextStyle().bigText(context.getString(R.string.protection_off_text, missing)))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ERROR)
            .setAutoCancel(true)
            .setContentIntent(openAppIntent(openHealth = true))
            .build()
        manager.notify(PROTECTION_ID, notification)
    }

    fun notifyBreak(round: Int, rounds: Int, breakMs: Long) {
        val minutes = ((breakMs + 59_999L) / 60_000L).toInt()
        notifyFocus(
            context.resources.getQuantityString(R.plurals.focus_break_title, minutes, minutes),
            context.getString(R.string.focus_break_text, round, rounds),
        )
    }

    fun notifyBackToFocus(round: Int, rounds: Int) {
        notifyFocus(context.getString(R.string.focus_back_title), context.getString(R.string.focus_back_text, round, rounds))
    }

    fun notifyFocusDone() {
        notifyFocus(context.getString(R.string.focus_done_title), context.getString(R.string.focus_done_text))
    }

    @SuppressLint("MissingPermission")
    private fun notifyFocus(title: String, text: String) {
        ensureChannels()
        if (!manager.areNotificationsEnabled()) return
        val notification = NotificationCompat.Builder(context, CHANNEL_FOCUS)
            .setSmallIcon(R.drawable.ic_stat_focuno)
            .setContentTitle(title)
            .setContentText(text)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setAutoCancel(true)
            .setContentIntent(openAppIntent(openHealth = false))
            .build()
        manager.notify(FOCUS_ID, notification)
    }

    private fun openAppIntent(openHealth: Boolean): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra(MainActivity.EXTRA_OPEN_HEALTH, openHealth)
        }
        return PendingIntent.getActivity(
            context,
            if (openHealth) 1 else 0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
    }

    companion object {
        const val CHANNEL_MONITOR = "focuno_monitor"
        const val CHANNEL_PROTECTION = "focuno_protection"
        const val CHANNEL_FOCUS = "focuno_focus"
        const val MONITOR_ID = 1001
        const val PROTECTION_ID = 1002
        const val FOCUS_ID = 1003
    }
}
