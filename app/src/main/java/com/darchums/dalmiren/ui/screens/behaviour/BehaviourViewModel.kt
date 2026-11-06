package com.darchums.dalmiren.ui.screens.behaviour

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.darchums.dalmiren.data.repository.ReflectionsRepository
import com.darchums.dalmiren.domain.model.AgeGroup
import com.darchums.dalmiren.domain.model.GuidanceResult
import com.darchums.dalmiren.domain.model.ParentingCategory
import com.darchums.dalmiren.domain.model.ParentingSituation
import com.darchums.dalmiren.domain.repository.ParentingAssistantRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class BehaviourViewModel(
    private val assistantRepository: ParentingAssistantRepository,
    private val reflectionsRepository: ReflectionsRepository
) : ViewModel() {

    var description = MutableStateFlow("")
    var selectedAgeGroup = MutableStateFlow(AgeGroup.EARLY_CHILDHOOD)
    var triggers = MutableStateFlow("")
    var contextDetails = MutableStateFlow("")

    private val _guidanceResult = MutableStateFlow<GuidanceResult?>(null)
    val guidanceResult: StateFlow<GuidanceResult?> = _guidanceResult.asStateFlow()

    private val _isAnalyzing = MutableStateFlow(false)
    val isAnalyzing: StateFlow<Boolean> = _isAnalyzing.asStateFlow()

    fun analyzeSituation() {
        if (description.value.isBlank()) return

        _isAnalyzing.value = true
        _guidanceResult.value = null

        viewModelScope.launch {
            val situation = ParentingSituation(
                description = description.value,
                ageGroup = selectedAgeGroup.value,
                triggers = triggers.value,
                context = contextDetails.value
            )
            val result = assistantRepository.analyzeBehaviour(situation)
            _guidanceResult.value = result
            _isAnalyzing.value = false

            // Automatically save guidance observation to history
            reflectionsRepository.addReflection(
                category = ParentingCategory.BEHAVIOUR,
                title = "Behaviour Insight (${situation.ageGroup.label})",
                notes = "Situation: ${description.value}\nResponse: ${result.calmResponse}",
                feelingRating = 4
            )
        }
    }
}
