package com.startup.focuno.data.repository

import com.startup.focuno.data.room.AppCategoryDao
import com.startup.focuno.data.room.AppCategoryEntity
import com.startup.focuno.domain.model.AppCategory
import com.startup.focuno.domain.model.SeedCategories
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppCategoryRepository @Inject constructor(private val dao: AppCategoryDao) {

    fun observeOverrides(): Flow<Map<String, AppCategory>> =
        dao.observeAll().map { rows -> rows.associate { it.packageName to it.category } }

    suspend fun overrides(): Map<String, AppCategory> = dao.getAll().associate { it.packageName to it.category }

    /** A user override always wins over the built-in seed list. */
    fun resolve(packageName: String, overrides: Map<String, AppCategory>): AppCategory =
        overrides[packageName] ?: SeedCategories.categoryOf(packageName)

    suspend fun setCategory(packageName: String, category: AppCategory) {
        dao.upsert(AppCategoryEntity(packageName, category, userOverridden = true))
    }

    suspend fun setCategories(categories: Map<String, AppCategory>) {
        dao.upsertAll(categories.map { (pkg, category) -> AppCategoryEntity(pkg, category, userOverridden = true) })
    }
}
