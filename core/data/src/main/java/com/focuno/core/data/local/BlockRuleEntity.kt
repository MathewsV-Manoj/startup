package com.focuno.core.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "block_rule")
data class BlockRuleEntity(
    @PrimaryKey val packageName: String,
    val enabled: Boolean,
    val startMinute: Int,
    val endMinute: Int,
    val daysMask: Int,
)
