package com.startup.focuno.service.monitor

import com.startup.focuno.data.local.SettingsStore
import com.startup.focuno.data.repository.FocusSessionRepository
import com.startup.focuno.data.repository.SummaryRepository
import com.startup.focuno.domain.usecase.WeeklyReportRules
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

/** Sends last week's report once, from Monday morning on. Checked by the background service; cheap when not due. */
@Singleton
class WeeklyReportSender @Inject constructor(
    private val settingsStore: SettingsStore,
    private val summaryRepository: SummaryRepository,
    private val focusSessions: FocusSessionRepository,
    private val notificationHelper: NotificationHelper,
) {
    suspend fun maybeSend(now: LocalDateTime = LocalDateTime.now(), zone: ZoneId = ZoneId.systemDefault()) {
        val settings = settingsStore.settings.first()
        val lastReported = settings.weeklyReportWeek?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
        val weekStart = WeeklyReportRules.weekDue(now, lastReported) ?: return

        val week = summaryRepository.summaries(weekStart, weekStart.plusDays(6))
        val weekBefore = summaryRepository.summaries(weekStart.minusWeeks(1), weekStart.minusDays(1))
        val fromMs = weekStart.atStartOfDay(zone).toInstant().toEpochMilli()
        val toMs = weekStart.plusWeeks(1).atStartOfDay(zone).toInstant().toEpochMilli()
        val focusMs = focusSessions.focusSpans(fromMs, toMs).sumOf { (start, end) -> (end - start).coerceAtLeast(0L) }

        // Remember the week even when it had no data, so this check stays a single settings read.
        settingsStore.setWeeklyReportWeek(weekStart.toString())
        WeeklyReportRules.build(weekStart, week, weekBefore, focusMs, settings.dailyGoalMinutes)
            ?.let(notificationHelper::notifyWeeklyReport)
    }
}
