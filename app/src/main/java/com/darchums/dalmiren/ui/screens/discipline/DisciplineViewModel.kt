package com.darchums.dalmiren.ui.screens.discipline

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.darchums.dalmiren.domain.model.AgeGroup
import com.darchums.dalmiren.domain.model.GuidanceResult
import com.darchums.dalmiren.domain.repository.ParentingAssistantRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class DisciplineViewModel(
    private val assistantRepository: ParentingAssistantRepository
) : ViewModel() {

    var selectedAgeGroup = MutableStateFlow(AgeGroup.EARLY_CHILDHOOD)
    var conflictType = MutableStateFlow("")

    private val _guidanceResult = MutableStateFlow<GuidanceResult?>(null)
    val guidanceResult: StateFlow<GuidanceResult?> = _guidanceResult.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    fun fetchDisciplineStrategy() {
        _isLoading.value = true
        viewModelScope.launch {
            val result = assistantRepository.getPositiveDisciplineStrategy(
                ageGroup = selectedAgeGroup.value,
                conflictType = conflictType.value
            )
            _guidanceResult.value = result
            _isLoading.value = false
        }
    }
}
