package com.darchums.dalmiren.domain.repository

import com.darchums.dalmiren.domain.model.AgeGroup
import com.darchums.dalmiren.domain.model.ChatMessage
import com.darchums.dalmiren.domain.model.GuidanceResult
import com.darchums.dalmiren.domain.model.GuidanceModuleMetadata
import com.darchums.dalmiren.domain.model.ParentingCategory
import com.darchums.dalmiren.domain.model.ParentingSituation
import kotlinx.coroutines.flow.Flow

interface ParentingAssistantRepository {
    suspend fun analyzeBehaviour(situation: ParentingSituation): GuidanceResult
    suspend fun getEmotionalGuidance(ageGroup: AgeGroup, emotionalContext: String): GuidanceResult
    suspend fun getCommunicationScript(ageGroup: AgeGroup, scenario: String): GuidanceResult
    suspend fun getPositiveDisciplineStrategy(ageGroup: AgeGroup, conflictType: String): GuidanceResult
    suspend fun getLearningSupportStrategy(ageGroup: AgeGroup, goalArea: String): GuidanceResult
    suspend fun sendCompanionQuery(message: String): Flow<ChatMessage>
    suspend fun getGuidanceModules(): List<GuidanceModuleMetadata> = emptyList()
    fun getDailyReflectionTip(): String
}
