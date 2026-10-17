package com.focuno.core.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "focus_score")
data class FocusScoreEntity(
    @PrimaryKey val epochDay: Long,
    val score: Int,
    val deepWorkRatio: Float,
    val switchStability: Float,
    val distractionResistance: Float,
    val consistency: Float,
)
