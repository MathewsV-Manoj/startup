package com.startup.focuno.data.repository

import com.startup.focuno.data.room.AppLimitDao
import com.startup.focuno.data.room.toDomain
import com.startup.focuno.data.room.toEntity
import com.startup.focuno.domain.model.AppLimit
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LimitRepository @Inject constructor(private val dao: AppLimitDao) {
    fun observeAll(): Flow<List<AppLimit>> = dao.observeAll().map { rows -> rows.map { it.toDomain() } }

    suspend fun save(limit: AppLimit) = dao.upsert(limit.toEntity())

    suspend fun delete(packageName: String) = dao.delete(packageName)
}
