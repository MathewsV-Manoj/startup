package com.startup.focuno.ui.screens.insights

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.startup.focuno.data.repository.BypassRepository
import com.startup.focuno.data.repository.FocusSessionRepository
import com.startup.focuno.data.repository.SummaryRepository
import com.startup.focuno.data.repository.UsageTrackingRepository
import com.startup.focuno.data.room.SubjectTotal
import com.startup.focuno.domain.model.BypassOutcome
import com.startup.focuno.domain.model.DayStats
import com.startup.focuno.domain.usecase.ComputeDayStatsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject

enum class InsightsRange(val days: Int) { WEEK(7), MONTH(30) }

data class TrendPoint(val date: LocalDate, val score: Int?, val distractingMs: Long)

data class BypassStats(
    val started: Int = 0,
    val abandoned: Int = 0,
    val granted: Int = 0,
    val completedFriction: Int = 0,
    val blockHits: Int = 0,
)

data class InsightsUiState(
    val isLoading: Boolean = true,
    val range: InsightsRange = InsightsRange.WEEK,
    val points: List<TrendPoint> = emptyList(),
    val averageScore: Int? = null,
    val hourlyDistractingMs: List<Long> = List(24) { 0L },
    val worstHour: Int? = null,
    val bypass: BypassStats = BypassStats(),
    val focusSessionsCompleted: Int = 0,
    /** Focus time per subject in the range, largest first. An empty subject means "no subject". */
    val subjectTotals: List<SubjectTotal> = emptyList(),
) {
    val hasAnyData: Boolean
        get() = points.any { it.score != null || it.distractingMs > 0 } || bypass.blockHits > 0 || bypass.started > 0 ||
            subjectTotals.isNotEmpty()
}

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class InsightsViewModel @Inject constructor(
    private val savedState: SavedStateHandle,
    private val summaryRepository: SummaryRepository,
    private val bypassRepository: BypassRepository,
    private val focusSessions: FocusSessionRepository,
    private val computeDayStats: ComputeDayStatsUseCase,
    private val usageRepository: UsageTrackingRepository,
) : ViewModel() {

    private val range = MutableStateFlow(
        InsightsRange.entries.firstOrNull { it.name == savedState.get<String>(KEY_RANGE) } ?: InsightsRange.WEEK,
    )
    private val todayLive = MutableStateFlow<DayStats?>(null)

    val state: StateFlow<InsightsUiState> = range.flatMapLatest { selected ->
        val today = LocalDate.now()
        val from = today.minusDays((selected.days - 1).toLong())
        val sinceMs = from.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()

        val bypassFlow = combine(
            bypassRepository.observeCount(BypassOutcome.ABANDONED, sinceMs),
            bypassRepository.observeCount(BypassOutcome.GRANTED, sinceMs),
            bypassRepository.observeCount(BypassOutcome.COMPLETED_FRICTION, sinceMs),
            bypassRepository.observeBlockHitsSince(sinceMs),
        ) { abandoned, granted, completed, hits ->
            // Every attempt ends in exactly one of abandoned or granted, so together they are the attempts started.
            BypassStats(started = abandoned + granted, abandoned = abandoned, granted = granted, completedFriction = completed, blockHits = hits)
        }

        val focusFlow = combine(
            focusSessions.observeCompletedCount(sinceMs),
            focusSessions.observeSubjectTotals(sinceMs, System.currentTimeMillis()),
        ) { count, subjects -> count to subjects.filter { it.totalMs > 0 } }

        combine(
            summaryRepository.observeSummaries(from, today),
            summaryRepository.observeHourlyTotals(from, today),
            bypassFlow,
            focusFlow,
            todayLive,
        ) { summaries, hourlyRows, bypass, (sessions, subjects), live ->
            val byDate = summaries.associateBy { it.date }
            val points = (0 until selected.days).map { i ->
                val date = from.plusDays(i.toLong())
                if (date == today) {
                    TrendPoint(date, live?.focusScore, live?.distractingMs ?: 0L)
                } else {
                    val row = byDate[date.toString()]
                    TrendPoint(date, row?.focusScore, row?.distractingMs ?: 0L)
                }
            }
            val hourly = LongArray(24)
            hourlyRows.forEach { hourly[it.hour.coerceIn(0, 23)] += it.totalMs }
            live?.hourlyDistractingMs?.forEachIndexed { hour, ms -> hourly[hour] += ms }
            val peak = hourly.indices.maxByOrNull { hourly[it] }
            val scores = points.mapNotNull { it.score }

            InsightsUiState(
                isLoading = false,
                range = selected,
                points = points,
                averageScore = if (scores.isEmpty()) null else scores.average().toInt(),
                hourlyDistractingMs = hourly.toList(),
                worstHour = peak?.takeIf { hourly[it] > 0 },
                bypass = bypass,
                focusSessionsCompleted = sessions,
                subjectTotals = subjects,
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), InsightsUiState())

    fun selectRange(selected: InsightsRange) {
        savedState[KEY_RANGE] = selected.name
        range.value = selected
    }

    /** Today is not rolled up yet, so it is computed live and merged into the trends. */
    fun refresh() {
        viewModelScope.launch {
            try {
                if (usageRepository.hasUsageAccess()) todayLive.value = computeDayStats(LocalDate.now())
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                // Keep showing the last good value; the stored history is unaffected.
            }
        }
    }

    private companion object {
        const val KEY_RANGE = "range"
    }
}
