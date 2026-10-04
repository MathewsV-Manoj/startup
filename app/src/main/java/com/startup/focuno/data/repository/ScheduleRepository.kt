package com.startup.focuno.data.repository

import com.startup.focuno.data.local.SettingsStore
import com.startup.focuno.data.room.BlockScheduleDao
import com.startup.focuno.data.room.toDomain
import com.startup.focuno.data.room.toEntity
import com.startup.focuno.domain.model.BlockSchedule
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ScheduleRepository @Inject constructor(
    private val dao: BlockScheduleDao,
    private val settings: SettingsStore,
) {
    fun observeAll(): Flow<List<BlockSchedule>> = dao.observeAll().map { rows -> rows.map { it.toDomain() } }

    suspend fun save(schedule: BlockSchedule) {
        if (schedule.id == 0L) dao.insert(schedule.toEntity()) else dao.update(schedule.toEntity())
    }

    suspend fun saveAll(schedules: List<BlockSchedule>) {
        schedules.forEach { save(it) }
    }

    suspend fun delete(id: Long) = dao.deleteById(id)

    suspend fun deleteForPackage(packageName: String) = dao.deleteForPackage(packageName)

    /** Blocks every distracting app until [untilMs]. The normal friction flow still applies. */
    suspend fun startQuickBlock(untilMs: Long, label: String) = settings.setQuickBlock(untilMs, label)

    suspend fun stopQuickBlock() = settings.clearQuickBlock()
}
