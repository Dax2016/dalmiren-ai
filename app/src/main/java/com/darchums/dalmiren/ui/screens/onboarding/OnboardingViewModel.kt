package com.darchums.dalmiren.ui.screens.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.darchums.dalmiren.data.repository.PreferencesRepository
import kotlinx.coroutines.launch

class OnboardingViewModel(private val preferencesRepository: PreferencesRepository) : ViewModel() {

    fun completeOnboarding(onSuccess: () -> Unit) {
        viewModelScope.launch {
            preferencesRepository.setOnboardingCompleted(true)
            onSuccess()
        }
    }
}
