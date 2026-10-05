package com.startup.focuno.data.export

import com.startup.focuno.data.room.DailySummaryEntity
import com.startup.focuno.data.room.FocusSessionEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

class CsvExportTest {

    private val zone = ZoneId.of("Asia/Kolkata")

    @Test
    fun writesBothTablesInOrder() {
        val day = DailySummaryEntity("2026-10-05", 3 * 3_600_000L, 3_600_000L, 1_800_000L, 72, 0, 41, 0)
        val start = LocalDate.parse("2026-10-05").atTime(9, 0).atZone(zone).toInstant().toEpochMilli()
        val session = FocusSessionEntity(startTs = start, endTs = start + 25 * 60_000L, plannedMs = 25 * 60_000L, completed = true, interruptions = 0, subject = "Signals")
        val lines = CsvExport.build(listOf(day), listOf(session), zone).lines()
        assertEquals("date,screen_time_min,time_eater_min,helpful_min,focus_score,pickups", lines[0])
        assertEquals("2026-10-05,180,60,30,72,41", lines[1])
        assertEquals("", lines[2])
        assertEquals("2026-10-05 09:00,2026-10-05 09:25,25,Signals,yes", lines[4])
    }

    @Test
    fun quotesSubjectsThatWouldBreakTheColumns() {
        assertEquals("\"Maths, Calculus\"", CsvExport.escape("Maths, Calculus"))
        assertEquals("\"He said \"\"hi\"\"\"", CsvExport.escape("He said \"hi\""))
        assertTrue(CsvExport.escape("Networks") == "Networks")
    }
}
