package com.focuno.core.domain.repository

import com.focuno.core.model.UserFocusProfile
import kotlinx.coroutines.flow.Flow

interface UserFocusProfileRepository {
    val profile: Flow<UserFocusProfile>
    suspend fun current(): UserFocusProfile
    suspend fun update(profile: UserFocusProfile)
}
