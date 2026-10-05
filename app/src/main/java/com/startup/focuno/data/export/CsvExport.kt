package com.startup.focuno.data.export

import com.startup.focuno.data.room.DailySummaryEntity
import com.startup.focuno.data.room.FocusSessionEntity
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * Turns the person's own history into one CSV file with two tables, separated by a blank line:
 * daily screen time, then focus sessions. Opens in any spreadsheet app.
 */
object CsvExport {
    private val timeFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")

    fun build(days: List<DailySummaryEntity>, sessions: List<FocusSessionEntity>, zone: ZoneId): String = buildString {
        appendLine("date,screen_time_min,time_eater_min,helpful_min,focus_score,pickups")
        days.sortedBy { it.date }.forEach { day ->
            appendLine(
                listOf(
                    day.date,
                    minutes(day.totalMs),
                    minutes(day.distractingMs),
                    minutes(day.productiveMs),
                    day.focusScore?.toString().orEmpty(),
                    day.pickupCount.toString(),
                ).joinToString(","),
            )
        }
        appendLine()
        appendLine("focus_start,focus_end,minutes,subject,finished")
        sessions.sortedBy { it.startTs }.forEach { session ->
            appendLine(
                listOf(
                    format(session.startTs, zone),
                    format(session.endTs, zone),
                    minutes(session.endTs - session.startTs),
                    escape(session.subject),
                    if (session.completed) "yes" else "no",
                ).joinToString(","),
            )
        }
    }

    private fun minutes(ms: Long): String = (ms.coerceAtLeast(0L) / 60_000L).toString()

    private fun format(ms: Long, zone: ZoneId): String = Instant.ofEpochMilli(ms).atZone(zone).format(timeFormat)

    /** Quotes a field that contains a comma, a quote or a line break, as CSV requires. */
    internal fun escape(value: String): String =
        if (value.any { it == ',' || it == '"' || it == '\n' || it == '\r' }) "\"" + value.replace("\"", "\"\"") + "\"" else value
}
