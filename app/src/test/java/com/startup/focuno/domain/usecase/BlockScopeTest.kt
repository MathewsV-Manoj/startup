package com.startup.focuno.domain.usecase

import com.startup.focuno.domain.model.BlockSchedule
import com.startup.focuno.domain.model.BlockScope
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

class BlockScopeTest {

    private val noon = LocalDate.parse("2026-10-05").atTime(12, 0).atZone(ZoneId.of("UTC"))

    private fun schedule(scope: BlockScope) = BlockSchedule(
        id = 1,
        packageName = "com.instagram.android",
        scope = scope,
        startMinuteOfDay = 0,
        endMinuteOfDay = 0,
        daysOfWeekMask = BlockSchedule.ALL_DAYS,
    )

    @Test
    fun reelsOnlyScheduleDoesNotBlockTheWholeApp() {
        val schedules = listOf(schedule(BlockScope.SHORT_VIDEO))
        assertNull(ScheduleEvaluator.activeWindow(schedules, "com.instagram.android", noon, BlockScope.APP))
        assertNotNull(ScheduleEvaluator.activeWindow(schedules, "com.instagram.android", noon, BlockScope.SHORT_VIDEO))
    }

    @Test
    fun wholeAppScheduleDoesNotCountAsAReelsRule() {
        val schedules = listOf(schedule(BlockScope.APP))
        assertNotNull(ScheduleEvaluator.activeWindow(schedules, "com.instagram.android", noon, BlockScope.APP))
        assertNull(ScheduleEvaluator.activeWindow(schedules, "com.instagram.android", noon, BlockScope.SHORT_VIDEO))
    }

    @Test
    fun defaultScopeIsTheWholeApp() {
        val schedules = listOf(schedule(BlockScope.APP))
        assertNotNull(ScheduleEvaluator.activeWindow(schedules, "com.instagram.android", noon))
    }
}
