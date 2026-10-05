package com.startup.focuno.service.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.SystemClock
import android.util.Log
import android.view.View
import android.widget.RemoteViews
import com.startup.focuno.R
import com.startup.focuno.data.local.SettingsStore
import com.startup.focuno.data.repository.UsageTrackingRepository
import com.startup.focuno.domain.model.FocusMode
import com.startup.focuno.domain.usecase.ExamCountdown
import com.startup.focuno.service.focus.FocusController
import com.startup.focuno.ui.MainActivity
import com.startup.focuno.ui.components.formatDuration
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.LocalDate

/**
 * Home-screen widget: today's screen time, the streak, and one button. The button starts a 25-minute
 * focus timer; while a timer runs it shows a live countdown and opens the app instead.
 */
class FocusWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        val pending = goAsync()
        FocusWidget.launch {
            try {
                FocusWidget.render(context)
            } finally {
                pending.finish()
            }
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action != FocusWidget.ACTION_START_FOCUS) return
        val pending = goAsync()
        FocusWidget.launch {
            try {
                val deps = FocusWidget.deps(context)
                val plan = deps.settingsStore().settings.first().focusPlan
                // A second tap while a timer runs must not start another one.
                if (plan?.isRunning(System.currentTimeMillis()) != true) {
                    deps.focusController().start(FocusMode.TIMER, FocusWidget.QUICK_MINUTES, strict = false, subject = "")
                }
                FocusWidget.render(context)
            } finally {
                pending.finish()
            }
        }
    }
}

object FocusWidget {
    const val ACTION_START_FOCUS = "com.startup.focuno.action.WIDGET_START_FOCUS"
    const val QUICK_MINUTES = 25
    private const val TAG = "FocusWidget"

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface Deps {
        fun settingsStore(): SettingsStore
        fun usageRepository(): UsageTrackingRepository
        fun focusController(): FocusController
    }

    internal fun deps(context: Context): Deps = EntryPointAccessors.fromApplication(context.applicationContext, Deps::class.java)

    internal fun launch(block: suspend () -> Unit) {
        scope.launch {
            try {
                block()
            } catch (e: Exception) {
                Log.w(TAG, "Widget update failed", e)
            }
        }
    }

    /** Redraws every Focuno widget. Cheap when there are none: it returns before reading any usage. */
    fun refresh(context: Context) {
        launch { render(context) }
    }

    internal suspend fun render(context: Context) {
        val manager = AppWidgetManager.getInstance(context)
        val ids = manager.getAppWidgetIds(ComponentName(context, FocusWidgetProvider::class.java))
        if (ids.isEmpty()) return
        val deps = deps(context)
        val settings = deps.settingsStore().settings.first()
        val usage = deps.usageRepository()
        val totalMs = if (usage.hasUsageAccess()) usage.getUsageForDay(LocalDate.now()).apps.sumOf { it.foregroundMs } else null
        val now = System.currentTimeMillis()
        val plan = settings.focusPlan?.takeIf { it.isRunning(now) }

        val views = RemoteViews(context.packageName, R.layout.widget_focus)
        views.setTextViewText(R.id.widget_time, totalMs?.let { formatDuration(context.resources, it) } ?: context.getString(R.string.gauge_dash))
        views.setTextViewText(R.id.widget_streak, context.resources.getQuantityString(R.plurals.widget_streak, settings.streakCount, settings.streakCount))
        views.setOnClickPendingIntent(R.id.widget_root, openApp(context))
        val examDays = ExamCountdown.daysLeft(ExamCountdown.parse(settings.examDate), LocalDate.now())
        if (examDays == null) {
            views.setViewVisibility(R.id.widget_exam, View.GONE)
        } else {
            val days = examDays.toInt()
            val text = when {
                days == 0 -> context.getString(R.string.exam_today_unnamed)
                settings.examName.isBlank() -> context.resources.getQuantityString(R.plurals.exam_days_left, days, days)
                else -> context.resources.getQuantityString(R.plurals.exam_countdown, days, settings.examName, days)
            }
            views.setTextViewText(R.id.widget_exam, text)
            views.setViewVisibility(R.id.widget_exam, View.VISIBLE)
        }

        if (plan == null) {
            views.setViewVisibility(R.id.widget_countdown, View.GONE)
            views.setViewVisibility(R.id.widget_action_label, View.VISIBLE)
            views.setTextViewText(R.id.widget_action_label, context.getString(R.string.widget_start, QUICK_MINUTES))
            views.setOnClickPendingIntent(R.id.widget_action, startFocus(context))
        } else {
            val phaseEnd = plan.phaseAt(now)?.endsAtMs ?: plan.endMs
            views.setViewVisibility(R.id.widget_action_label, View.GONE)
            views.setViewVisibility(R.id.widget_countdown, View.VISIBLE)
            if (plan.mode == FocusMode.STOPWATCH) {
                views.setChronometerCountDown(R.id.widget_countdown, false)
                views.setChronometer(R.id.widget_countdown, SystemClock.elapsedRealtime() - (now - plan.startMs), null, true)
            } else {
                views.setChronometerCountDown(R.id.widget_countdown, true)
                views.setChronometer(R.id.widget_countdown, SystemClock.elapsedRealtime() + (phaseEnd - now), null, true)
            }
            views.setOnClickPendingIntent(R.id.widget_action, openApp(context))
        }
        manager.updateAppWidget(ids, views)
    }

    private fun openApp(context: Context): PendingIntent = PendingIntent.getActivity(
        context,
        REQUEST_OPEN,
        Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )

    private fun startFocus(context: Context): PendingIntent = PendingIntent.getBroadcast(
        context,
        REQUEST_START,
        Intent(context, FocusWidgetProvider::class.java).setAction(ACTION_START_FOCUS),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )

    private const val REQUEST_OPEN = 20
    private const val REQUEST_START = 21
}
