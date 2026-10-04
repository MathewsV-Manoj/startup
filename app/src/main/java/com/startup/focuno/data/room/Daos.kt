package com.startup.focuno.data.room

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import androidx.room.Upsert
import com.startup.focuno.domain.model.BypassOutcome
import kotlinx.coroutines.flow.Flow

data class HourTotal(val hour: Int, val totalMs: Long)

data class LastGranted(val packageName: String, val lastGrantedAt: Long)

@Dao
interface DailyUsageDao {
    @Upsert
    suspend fun upsertAll(rows: List<DailyUsageEntity>)

    @Query("DELETE FROM daily_usage WHERE date = :date")
    suspend fun deleteForDate(date: String)

    @Query("SELECT * FROM daily_usage WHERE date = :date ORDER BY foregroundMs DESC")
    suspend fun forDate(date: String): List<DailyUsageEntity>

    @Query("DELETE FROM daily_usage WHERE date < :date")
    suspend fun deleteOlderThan(date: String)
}

@Dao
interface DailySummaryDao {
    @Upsert
    suspend fun upsert(row: DailySummaryEntity)

    @Query("SELECT * FROM daily_summary WHERE date BETWEEN :from AND :to ORDER BY date")
    fun observeRange(from: String, to: String): Flow<List<DailySummaryEntity>>

    @Query("SELECT * FROM daily_summary WHERE date BETWEEN :from AND :to ORDER BY date")
    suspend fun range(from: String, to: String): List<DailySummaryEntity>

    @Query("SELECT date FROM daily_summary WHERE date BETWEEN :from AND :to")
    suspend fun existingDates(from: String, to: String): List<String>

    @Query("DELETE FROM daily_summary WHERE date < :date")
    suspend fun deleteOlderThan(date: String)
}

@Dao
interface HourlyDistractingDao {
    @Upsert
    suspend fun upsertAll(rows: List<HourlyDistractingEntity>)

    @Query("DELETE FROM hourly_distracting WHERE date = :date")
    suspend fun deleteForDate(date: String)

    @Query(
        "SELECT hour AS hour, SUM(distractingMs) AS totalMs FROM hourly_distracting " +
            "WHERE date BETWEEN :from AND :to GROUP BY hour",
    )
    fun observeTotalsByHour(from: String, to: String): Flow<List<HourTotal>>

    @Query("DELETE FROM hourly_distracting WHERE date < :date")
    suspend fun deleteOlderThan(date: String)
}

@Dao
interface BlockScheduleDao {
    @Query("SELECT * FROM block_schedule ORDER BY packageName, startMinuteOfDay")
    fun observeAll(): Flow<List<BlockScheduleEntity>>

    @Insert
    suspend fun insert(row: BlockScheduleEntity): Long

    @Update
    suspend fun update(row: BlockScheduleEntity)

    @Query("DELETE FROM block_schedule WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM block_schedule WHERE packageName = :packageName")
    suspend fun deleteForPackage(packageName: String)
}

@Dao
interface BypassEventDao {
    @Insert
    suspend fun insert(row: BypassEventEntity)

    @Query(
        "SELECT packageName AS packageName, MAX(timestamp) AS lastGrantedAt FROM bypass_event " +
            "WHERE outcome = 'GRANTED' GROUP BY packageName",
    )
    fun observeLastGranted(): Flow<List<LastGranted>>

    @Query("SELECT COUNT(*) FROM bypass_event WHERE outcome = :outcome AND timestamp >= :since")
    fun observeCount(outcome: BypassOutcome, since: Long): Flow<Int>

    @Query("SELECT COUNT(*) FROM bypass_event WHERE outcome = 'GRANTED' AND timestamp >= :from AND timestamp < :to")
    suspend fun grantedBetween(from: Long, to: Long): Int

    @Query("SELECT COUNT(*) FROM bypass_event WHERE outcome = :outcome AND timestamp >= :from AND timestamp < :to")
    suspend fun countBetween(outcome: BypassOutcome, from: Long, to: Long): Int
}

@Dao
interface BlockHitDao {
    @Insert
    suspend fun insert(row: BlockHitEntity)

    @Query("SELECT COUNT(*) FROM block_hit WHERE timestamp >= :since")
    fun observeCountSince(since: Long): Flow<Int>

    @Query("SELECT COUNT(*) FROM block_hit")
    suspend fun total(): Int
}

@Dao
interface FocusSessionDao {
    @Insert
    suspend fun insert(row: FocusSessionEntity): Long

    @Update
    suspend fun update(row: FocusSessionEntity)

    @Query("SELECT * FROM focus_session WHERE completed = 0 AND endTs > :nowMs ORDER BY startTs DESC LIMIT 1")
    suspend fun active(nowMs: Long): FocusSessionEntity?

    @Query("UPDATE focus_session SET completed = 1 WHERE completed = 0 AND endTs <= :nowMs AND endTs - startTs >= plannedMs")
    suspend fun finalizeDue(nowMs: Long)

    @Query("SELECT COUNT(*) FROM focus_session WHERE completed = 1 AND startTs >= :from AND startTs < :to")
    suspend fun completedBetween(from: Long, to: Long): Int

    @Query("SELECT MAX(endTs) FROM focus_session WHERE completed = 1")
    suspend fun lastCompletedEnd(): Long?

    @Query("SELECT COUNT(*) FROM focus_session WHERE completed = 1 AND startTs >= :since")
    fun observeCompletedCount(since: Long): Flow<Int>
}

@Dao
interface AppCategoryDao {
    @Query("SELECT * FROM app_category")
    fun observeAll(): Flow<List<AppCategoryEntity>>

    @Query("SELECT * FROM app_category")
    suspend fun getAll(): List<AppCategoryEntity>

    @Upsert
    suspend fun upsert(row: AppCategoryEntity)

    @Upsert
    suspend fun upsertAll(rows: List<AppCategoryEntity>)

    @Query("DELETE FROM app_category WHERE packageName = :packageName")
    suspend fun delete(packageName: String)
}

@Dao
interface AppLimitDao {
    @Query("SELECT * FROM app_limit ORDER BY packageName")
    fun observeAll(): Flow<List<AppLimitEntity>>

    @Upsert
    suspend fun upsert(row: AppLimitEntity)

    @Query("DELETE FROM app_limit WHERE packageName = :packageName")
    suspend fun delete(packageName: String)
}
