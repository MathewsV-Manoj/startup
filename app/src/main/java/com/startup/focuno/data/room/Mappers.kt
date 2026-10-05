package com.startup.focuno.data.room

import com.startup.focuno.domain.model.AppLimit
import com.startup.focuno.domain.model.BlockSchedule

fun BlockScheduleEntity.toDomain() = BlockSchedule(
    id = id,
    packageName = packageName,
    scope = scope,
    startMinuteOfDay = startMinuteOfDay,
    endMinuteOfDay = endMinuteOfDay,
    daysOfWeekMask = daysOfWeekMask,
    enabled = enabled,
    strictMode = strictMode,
)

fun BlockSchedule.toEntity() = BlockScheduleEntity(
    id = id,
    packageName = packageName,
    scope = scope,
    startMinuteOfDay = startMinuteOfDay,
    endMinuteOfDay = endMinuteOfDay,
    daysOfWeekMask = daysOfWeekMask,
    enabled = enabled,
    strictMode = strictMode,
)

fun AppLimitEntity.toDomain() =
    AppLimit(packageName = packageName, dailyMinutes = dailyMinutes, strict = strict, enabled = enabled, maxOpens = maxOpens)

fun AppLimit.toEntity() =
    AppLimitEntity(packageName = packageName, dailyMinutes = dailyMinutes, strict = strict, enabled = enabled, maxOpens = maxOpens)
