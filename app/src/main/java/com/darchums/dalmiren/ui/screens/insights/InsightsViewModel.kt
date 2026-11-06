package com.darchums.dalmiren.ui.screens.insights

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.darchums.dalmiren.data.repository.ReflectionsRepository
import com.darchums.dalmiren.domain.model.ParentReflection
import com.darchums.dalmiren.domain.model.ParentingCategory
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

class InsightsViewModel(
    private val reflectionsRepository: ReflectionsRepository
) : ViewModel() {

    val reflections: StateFlow<List<ParentReflection>> = reflectionsRepository.reflectionsFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun addReflection(category: ParentingCategory, title: String, notes: String, feelingRating: Int) {
        if (title.isBlank() || notes.isBlank()) return
        reflectionsRepository.addReflection(category, title, notes, feelingRating)
    }

    fun deleteReflection(id: String) {
        reflectionsRepository.deleteReflection(id)
    }

    fun clearHistory() {
        reflectionsRepository.clearAllReflections()
    }
}
