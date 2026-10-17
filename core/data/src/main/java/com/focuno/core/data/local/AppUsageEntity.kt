package com.focuno.core.data.local

import androidx.room.Entity

@Entity(tableName = "app_usage", primaryKeys = ["packageName", "epochDay"])
data class AppUsageEntity(
    val packageName: String,
    val epochDay: Long,
    val appLabel: String?,
    val totalMillis: Long,
    val launchCount: Int,
    val category: String,
)
