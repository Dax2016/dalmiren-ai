package com.darchums.dalmiren.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.darchums.dalmiren.data.repository.PreferencesRepository
import com.darchums.dalmiren.data.repository.ReflectionsRepository
import com.darchums.dalmiren.domain.model.AgeGroup
import com.darchums.dalmiren.domain.model.UserPreferences
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val preferencesRepository: PreferencesRepository,
    private val reflectionsRepository: ReflectionsRepository
) : ViewModel() {

    val userPreferences: StateFlow<UserPreferences> = preferencesRepository.userPreferencesFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = UserPreferences()
        )

    fun setDefaultAgeGroup(ageGroup: AgeGroup) {
        viewModelScope.launch {
            preferencesRepository.setDefaultAgeGroup(ageGroup)
        }
    }

    fun setDarkTheme(enabled: Boolean?) {
        viewModelScope.launch {
            preferencesRepository.setDarkTheme(enabled)
        }
    }

    fun clearAllUserData(onCleared: () -> Unit) {
        viewModelScope.launch {
            preferencesRepository.clearAllPreferences()
            reflectionsRepository.clearAllReflections()
            onCleared()
        }
    }
}
