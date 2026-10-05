package com.startup.focuno.domain.usecase

import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters

/** The Me tab's focus calendar: a grid of recent weeks, each day shaded by how long the person focused. */
object FocusCalendar {
    const val WEEKS = 12

    /** Focus time per local day. A session that runs past midnight counts on both days. */
    fun msByDay(spans: List<Pair<Long, Long>>, zone: ZoneId): Map<LocalDate, Long> {
        val result = HashMap<LocalDate, Long>()
        spans.forEach { (start, end) ->
            var cursor = start
            while (cursor < end) {
                val day = Instant.ofEpochMilli(cursor).atZone(zone).toLocalDate()
                val dayEnd = day.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
                val chunkEnd = minOf(end, dayEnd)
                result[day] = (result[day] ?: 0L) + (chunkEnd - cursor)
                cursor = chunkEnd
            }
        }
        return result
    }

    /** First day shown: the Monday [WEEKS] - 1 weeks before this week's Monday. */
    fun firstDay(today: LocalDate): LocalDate =
        today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).minusWeeks((WEEKS - 1).toLong())

    /** [WEEKS] columns of 7 days, Monday first. Days after [today] are null so they can be left blank. */
    fun grid(byDay: Map<LocalDate, Long>, today: LocalDate): List<List<Long?>> {
        val first = firstDay(today)
        return List(WEEKS) { week ->
            List(7) { weekday ->
                val date = first.plusDays((week * 7 + weekday).toLong())
                if (date.isAfter(today)) null else byDay[date] ?: 0L
            }
        }
    }

    /** 0 = none, then up to 4 for two hours or more. */
    fun level(ms: Long): Int = when {
        ms <= 0L -> 0
        ms < 25 * MINUTE -> 1
        ms < 60 * MINUTE -> 2
        ms < 120 * MINUTE -> 3
        else -> 4
    }

    private const val MINUTE = 60_000L
}
