package com.focuno.core.data.repository

import com.focuno.core.datastore.FocunoPreferences
import com.focuno.core.domain.repository.UserFocusProfileRepository
import com.focuno.core.model.UserFocusProfile
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DefaultUserFocusProfileRepository @Inject constructor(
    private val preferences: FocunoPreferences,
) : UserFocusProfileRepository {

    override val profile: Flow<UserFocusProfile> = preferences.profile

    override suspend fun current(): UserFocusProfile = preferences.current()

    override suspend fun update(profile: UserFocusProfile) = preferences.update(profile)
}
