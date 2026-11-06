package com.darchums.dalmiren.data.repository

import com.darchums.dalmiren.domain.model.ParentReflection
import com.darchums.dalmiren.domain.model.ParentingCategory
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID

class ReflectionsRepository {

    private val _reflections = MutableStateFlow<List<ParentReflection>>(
        listOf(
            ParentReflection(
                id = UUID.randomUUID().toString(),
                category = ParentingCategory.BEHAVIOUR,
                title = "Evening Transition Reflection",
                notes = "Used advance warnings 5 minutes before bedtime. Resistance was much lower.",
                timestamp = System.currentTimeMillis() - (1000 * 60 * 60 * 24),
                feelingRating = 4
            ),
            ParentReflection(
                id = UUID.randomUUID().toString(),
                category = ParentingCategory.EMOTIONAL,
                title = "Co-regulation at grocery store",
                notes = "Crouched down and validated feeling overwhelmed instead of getting anxious. Child calmed down faster.",
                timestamp = System.currentTimeMillis() - (1000 * 60 * 60 * 48),
                feelingRating = 5
            )
        )
    )

    val reflectionsFlow: Flow<List<ParentReflection>> = _reflections.asStateFlow()

    fun addReflection(category: ParentingCategory, title: String, notes: String, feelingRating: Int) {
        val newReflection = ParentReflection(
            id = UUID.randomUUID().toString(),
            category = category,
            title = title,
            notes = notes,
            timestamp = System.currentTimeMillis(),
            feelingRating = feelingRating
        )
        _reflections.value = listOf(newReflection) + _reflections.value
    }

    fun deleteReflection(id: String) {
        _reflections.value = _reflections.value.filterNot { it.id == id }
    }

    fun clearAllReflections() {
        _reflections.value = emptyList()
    }
}
