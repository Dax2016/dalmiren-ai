package com.darchums.dalmiren

import com.darchums.dalmiren.data.repository.ParentingAssistantRepositoryImpl
import com.darchums.dalmiren.domain.model.AgeGroup
import com.darchums.dalmiren.domain.model.ParentingCategory
import com.darchums.dalmiren.domain.model.ParentingSituation
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ParentingAssistantRepositoryTest {

    private lateinit var repository: ParentingAssistantRepositoryImpl

    @Before
    fun setUp() {
        repository = ParentingAssistantRepositoryImpl()
    }

    @Test
    fun analyzeBehaviour_returnsValidHypothesesAndCalmResponse() = runTest {
        val situation = ParentingSituation(
            description = "My child refuses to get dressed in the morning and throws a tantrum.",
            ageGroup = AgeGroup.EARLY_CHILDHOOD
        )

        val result = repository.analyzeBehaviour(situation)

        assertEquals(ParentingCategory.BEHAVIOUR, result.category)
        assertTrue(result.hypotheses.isNotEmpty())
        assertTrue(result.emotionalNeeds.isNotEmpty())
        assertNotNull(result.calmResponse)
        assertTrue(result.nextObservations.isNotEmpty())
    }

    @Test
    fun getEmotionalGuidance_returnsNonDiagnosticAdvice() = runTest {
        val result = repository.getEmotionalGuidance(AgeGroup.TODDLER, "Meltdown when leaving playground")

        assertEquals(ParentingCategory.EMOTIONAL, result.category)
        assertTrue(result.disclaimer.contains("Not a medical or psychological diagnosis"))
    }

    @Test
    fun sendCompanionQuery_emitsAssistantResponseWithSuggestedPrompts() = runTest {
        val flow = repository.sendCompanionQuery("How can I set a calm boundary?")
        val response = flow.first()

        assertNotNull(response)
        assertTrue(response.text.isNotBlank())
        assertTrue(response.suggestedPrompts.isNotEmpty())
    }

    @Test
    fun getDailyReflectionTip_returnsNonEmptyTip() {
        val tip = repository.getDailyReflectionTip()
        assertTrue(tip.isNotBlank())
    }
}
