package com.startup.focuno.domain.usecase

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

class ExamCountdownTest {

    private val today = LocalDate.parse("2026-10-05")

    @Test
    fun countsWholeDaysAndZeroOnTheDay() {
        assertEquals(123L, ExamCountdown.daysLeft(today.plusDays(123), today))
        assertEquals(0L, ExamCountdown.daysLeft(today, today))
    }

    @Test
    fun nothingOnceTheExamHasPassedOrWhenUnset() {
        assertNull(ExamCountdown.daysLeft(today.minusDays(1), today))
        assertNull(ExamCountdown.daysLeft(null, today))
    }

    @Test
    fun parsesOnlyValidDates() {
        assertEquals(LocalDate.parse("2027-02-06"), ExamCountdown.parse("2027-02-06"))
        assertNull(ExamCountdown.parse("not a date"))
        assertNull(ExamCountdown.parse(null))
    }
}
