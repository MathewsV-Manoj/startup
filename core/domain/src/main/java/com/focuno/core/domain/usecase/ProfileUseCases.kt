package com.focuno.core.domain.usecase

import com.focuno.core.domain.repository.UserFocusProfileRepository
import com.focuno.core.model.UserFocusProfile
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ObserveProfileUseCase @Inject constructor(
    private val repository: UserFocusProfileRepository,
) {
    operator fun invoke(): Flow<UserFocusProfile> = repository.profile
}

class UpdateProfileUseCase @Inject constructor(
    private val repository: UserFocusProfileRepository,
) {
    suspend operator fun invoke(profile: UserFocusProfile) = repository.update(profile)
}

class CompleteOnboardingUseCase @Inject constructor(
    private val repository: UserFocusProfileRepository,
) {
    suspend operator fun invoke(dailyFocusGoalMinutes: Int) {
        val current = repository.current()
        repository.update(
            current.copy(
                onboardingCompleted = true,
                dailyFocusGoalMinutes = dailyFocusGoalMinutes,
            ),
        )
    }
}
