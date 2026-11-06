package com.darchums.dalmiren.ui.screens.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.darchums.dalmiren.data.repository.ReflectionsRepository
import com.darchums.dalmiren.domain.model.GuidanceModuleMetadata
import com.darchums.dalmiren.domain.model.ParentReflection
import com.darchums.dalmiren.domain.repository.ParentingAssistantRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class DashboardViewModel(
    private val assistantRepository: ParentingAssistantRepository,
    private val reflectionsRepository: ReflectionsRepository
) : ViewModel() {

    val dailyTip: String = assistantRepository.getDailyReflectionTip()

    private val _modules = MutableStateFlow<List<GuidanceModuleMetadata>>(emptyList())
    val modules: StateFlow<List<GuidanceModuleMetadata>> = _modules.asStateFlow()

    val recentReflections: StateFlow<List<ParentReflection>> =
        reflectionsRepository.reflectionsFlow.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    init {
        viewModelScope.launch {
            _modules.value = assistantRepository.getGuidanceModules()
        }
    }
}
