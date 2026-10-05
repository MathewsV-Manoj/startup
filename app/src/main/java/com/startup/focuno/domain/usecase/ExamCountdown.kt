package com.startup.focuno.domain.usecase

import java.time.LocalDate
import java.time.temporal.ChronoUnit

/** Days until the person's exam, shown on the Focus tab and the pause screen. */
object ExamCountdown {
    /** Whole days from [today] to [examDate]: 0 on the day itself, null once it has passed or when unset. */
    fun daysLeft(examDate: LocalDate?, today: LocalDate): Long? {
        if (examDate == null || examDate.isBefore(today)) return null
        return ChronoUnit.DAYS.between(today, examDate)
    }

    fun parse(stored: String?): LocalDate? = stored?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
}
