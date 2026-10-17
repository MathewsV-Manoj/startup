package com.focuno.feature.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.focuno.core.domain.usecase.CompleteOnboardingUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val completeOnboarding: CompleteOnboardingUseCase,
) : ViewModel() {

    fun finish(goalMinutes: Int) {
        viewModelScope.launch { completeOnboarding(goalMinutes) }
    }
}
