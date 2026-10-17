package com.focuno.core.data.mapper

import com.focuno.core.data.local.AppUsageEntity
import com.focuno.core.data.local.BlockRuleEntity
import com.focuno.core.data.local.FocusScoreEntity
import com.focuno.core.model.AppCategory
import com.focuno.core.model.AppUsage
import com.focuno.core.model.BlockRule
import com.focuno.core.model.FocusScore
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime

fun AppUsageEntity.toDomain(): AppUsage = AppUsage(
    packageName = packageName,
    appLabel = appLabel,
    date = LocalDate.ofEpochDay(epochDay),
    totalTimeInForeground = java.time.Duration.ofMillis(totalMillis),
    launchCount = launchCount,
    category = runCatching { AppCategory.valueOf(category) }.getOrDefault(AppCategory.NEUTRAL),
)

fun AppUsage.toEntity(): AppUsageEntity = AppUsageEntity(
    packageName = packageName,
    epochDay = date.toEpochDay(),
    appLabel = appLabel,
    totalMillis = totalTimeInForeground.toMillis(),
    launchCount = launchCount,
    category = category.name,
)

fun FocusScoreEntity.toDomain(): FocusScore = FocusScore(
    date = LocalDate.ofEpochDay(epochDay),
    score = score,
    deepWorkRatio = deepWorkRatio,
    switchStability = switchStability,
    distractionResistance = distractionResistance,
    consistency = consistency,
)

fun FocusScore.toEntity(): FocusScoreEntity = FocusScoreEntity(
    epochDay = date.toEpochDay(),
    score = score,
    deepWorkRatio = deepWorkRatio,
    switchStability = switchStability,
    distractionResistance = distractionResistance,
    consistency = consistency,
)

fun BlockRuleEntity.toDomain(): BlockRule = BlockRule(
    packageName = packageName,
    enabled = enabled,
    startTime = LocalTime.of(startMinute / 60, startMinute % 60),
    endTime = LocalTime.of(endMinute / 60, endMinute % 60),
    days = decodeDays(daysMask),
)

fun BlockRule.toEntity(): BlockRuleEntity = BlockRuleEntity(
    packageName = packageName,
    enabled = enabled,
    startMinute = startTime.hour * 60 + startTime.minute,
    endMinute = endTime.hour * 60 + endTime.minute,
    daysMask = encodeDays(days),
)

internal fun encodeDays(days: Set<DayOfWeek>): Int =
    days.fold(0) { mask, day -> mask or (1 shl (day.value - 1)) }

internal fun decodeDays(mask: Int): Set<DayOfWeek> =
    DayOfWeek.entries.filter { mask and (1 shl (it.value - 1)) != 0 }.toSet()
