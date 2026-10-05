package com.startup.focuno.domain.usecase

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZoneId

class FocusCalendarTest {

    private val zone = ZoneId.of("Asia/Kolkata")
    private val min = 60_000L

    private fun at(date: String, hour: Int, minute: Int = 0) =
        LocalDate.parse(date).atTime(hour, minute).atZone(zone).toInstant().toEpochMilli()

    @Test
    fun sessionOverMidnightCountsOnBothDays() {
        val byDay = FocusCalendar.msByDay(listOf(at("2026-10-05", 23, 30) to at("2026-10-06", 0, 20)), zone)
        assertEquals(30 * min, byDay[LocalDate.parse("2026-10-05")])
        assertEquals(20 * min, byDay[LocalDate.parse("2026-10-06")])
    }

    @Test
    fun sessionsOnTheSameDayAddUp() {
        val spans = listOf(at("2026-10-05", 9) to at("2026-10-05", 9, 25), at("2026-10-05", 10) to at("2026-10-05", 10, 50))
        assertEquals(75 * min, FocusCalendar.msByDay(spans, zone)[LocalDate.parse("2026-10-05")])
    }

    @Test
    fun gridIsTwelveWeeksMondayFirstWithFutureDaysBlank() {
        val today = LocalDate.parse("2026-10-07") // a Wednesday
        val grid = FocusCalendar.grid(mapOf(today to 30 * min), today)
        assertEquals(FocusCalendar.WEEKS, grid.size)
        assertEquals(7, grid.last().size)
        assertEquals(DayOfWeek.MONDAY, FocusCalendar.firstDay(today).dayOfWeek)
        assertEquals(30 * min, grid.last()[2])
        assertNull(grid.last()[3])
        assertEquals(0L, grid.first()[0])
    }

    @Test
    fun levelsGrowWithTime() {
        assertEquals(0, FocusCalendar.level(0))
        assertEquals(1, FocusCalendar.level(10 * min))
        assertEquals(2, FocusCalendar.level(30 * min))
        assertEquals(3, FocusCalendar.level(90 * min))
        assertEquals(4, FocusCalendar.level(150 * min))
    }
}
