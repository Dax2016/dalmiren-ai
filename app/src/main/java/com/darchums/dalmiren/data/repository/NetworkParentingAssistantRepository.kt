package com.darchums.dalmiren.data.repository

import com.darchums.dalmiren.data.remote.BehaviourApiRequest
import com.darchums.dalmiren.data.remote.ChatApiRequest
import com.darchums.dalmiren.data.remote.NetworkClient
import com.darchums.dalmiren.domain.model.AgeGroup
import com.darchums.dalmiren.domain.model.ChatMessage
import com.darchums.dalmiren.domain.model.GuidanceResult
import com.darchums.dalmiren.domain.model.GuidanceModuleMetadata
import com.darchums.dalmiren.domain.model.MessageSender
import com.darchums.dalmiren.domain.model.ParentingCategory
import com.darchums.dalmiren.domain.model.ParentingSituation
import com.darchums.dalmiren.domain.repository.ParentingAssistantRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.util.UUID

class NetworkParentingAssistantRepository(
    private val offlineFallback: ParentingAssistantRepositoryImpl = ParentingAssistantRepositoryImpl()
) : ParentingAssistantRepository {

    override suspend fun analyzeBehaviour(situation: ParentingSituation): GuidanceResult {
        return runCatching {
            val response = NetworkClient.apiService.analyzeBehaviour(
                BehaviourApiRequest(
                    age_group = situation.ageGroup.name,
                    description = situation.description,
                    triggers = situation.triggers,
                    context = situation.context,
                    observations = situation.observations
                )
            )
            if (response.isSuccessful && response.body() != null) {
                val b = response.body()!!
                GuidanceResult(
                    title = b.title,
                    category = ParentingCategory.BEHAVIOUR,
                    hypotheses = b.hypotheses,
                    emotionalNeeds = b.emotional_needs,
                    calmResponse = b.calm_response,
                    nextObservations = b.next_observations,
                    practicalTip = b.practical_tip,
                    disclaimer = b.disclaimer
                )
            } else {
                offlineFallback.analyzeBehaviour(situation)
            }
        }.getOrElse {
            offlineFallback.analyzeBehaviour(situation)
        }
    }

    override suspend fun getEmotionalGuidance(ageGroup: AgeGroup, emotionalContext: String): GuidanceResult {
        return runCatching {
            val response = NetworkClient.apiService.getEmotionalGuidance(
                BehaviourApiRequest(
                    age_group = ageGroup.name,
                    description = emotionalContext
                )
            )
            if (response.isSuccessful && response.body() != null) {
                val b = response.body()!!
                GuidanceResult(
                    title = b.title,
                    category = ParentingCategory.EMOTIONAL,
                    hypotheses = b.hypotheses,
                    emotionalNeeds = b.emotional_needs,
                    calmResponse = b.calm_response,
                    nextObservations = b.next_observations,
                    practicalTip = b.practical_tip,
                    disclaimer = b.disclaimer
                )
            } else {
                offlineFallback.getEmotionalGuidance(ageGroup, emotionalContext)
            }
        }.getOrElse {
            offlineFallback.getEmotionalGuidance(ageGroup, emotionalContext)
        }
    }

    override suspend fun getCommunicationScript(ageGroup: AgeGroup, scenario: String): GuidanceResult {
        return runCatching {
            val response = NetworkClient.apiService.getCommunicationScript(
                BehaviourApiRequest(
                    age_group = ageGroup.name,
                    description = scenario
                )
            )
            if (response.isSuccessful && response.body() != null) {
                val b = response.body()!!
                GuidanceResult(
                    title = b.title,
                    category = ParentingCategory.COMMUNICATION,
                    hypotheses = b.hypotheses,
                    emotionalNeeds = b.emotional_needs,
                    calmResponse = b.calm_response,
                    nextObservations = b.next_observations,
                    practicalTip = b.practical_tip,
                    disclaimer = b.disclaimer
                )
            } else {
                offlineFallback.getCommunicationScript(ageGroup, scenario)
            }
        }.getOrElse {
            offlineFallback.getCommunicationScript(ageGroup, scenario)
        }
    }

    override suspend fun getPositiveDisciplineStrategy(ageGroup: AgeGroup, conflictType: String): GuidanceResult {
        return runCatching {
            val response = NetworkClient.apiService.getPositiveDisciplineStrategy(
                BehaviourApiRequest(
                    age_group = ageGroup.name,
                    description = conflictType
                )
            )
            if (response.isSuccessful && response.body() != null) {
                val b = response.body()!!
                GuidanceResult(
                    title = b.title,
                    category = ParentingCategory.DISCIPLINE,
                    hypotheses = b.hypotheses,
                    emotionalNeeds = b.emotional_needs,
                    calmResponse = b.calm_response,
                    nextObservations = b.next_observations,
                    practicalTip = b.practical_tip,
                    disclaimer = b.disclaimer
                )
            } else {
                offlineFallback.getPositiveDisciplineStrategy(ageGroup, conflictType)
            }
        }.getOrElse {
            offlineFallback.getPositiveDisciplineStrategy(ageGroup, conflictType)
        }
    }

    override suspend fun getLearningSupportStrategy(ageGroup: AgeGroup, goalArea: String): GuidanceResult {
        return runCatching {
            val response = NetworkClient.apiService.getLearningSupportStrategy(
                BehaviourApiRequest(
                    age_group = ageGroup.name,
                    description = goalArea
                )
            )
            if (response.isSuccessful && response.body() != null) {
                val b = response.body()!!
                GuidanceResult(
                    title = b.title,
                    category = ParentingCategory.LEARNING,
                    hypotheses = b.hypotheses,
                    emotionalNeeds = b.emotional_needs,
                    calmResponse = b.calm_response,
                    nextObservations = b.next_observations,
                    practicalTip = b.practical_tip,
                    disclaimer = b.disclaimer
                )
            } else {
                offlineFallback.getLearningSupportStrategy(ageGroup, goalArea)
            }
        }.getOrElse {
            offlineFallback.getLearningSupportStrategy(ageGroup, goalArea)
        }
    }

    override suspend fun sendCompanionQuery(message: String): Flow<ChatMessage> = flow {
        try {
            val response = NetworkClient.apiService.chat(
                ChatApiRequest(message = message, module = "COMPANION")
            )
            if (response.isSuccessful && response.body() != null) {
                val b = response.body()!!
                emit(
                    ChatMessage(
                        id = UUID.randomUUID().toString(),
                        sender = MessageSender.ASSISTANT,
                        text = b.message,
                        responseSummary = b.response_summary ?: "",
                        empathyStatement = b.empathy_statement ?: "",
                        understanding = b.understanding ?: "",
                        possibleContributingFactors = b.possible_contributing_factors ?: emptyList(),
                        developmentalContext = b.developmental_context ?: "",
                        recommendations = b.recommendations ?: emptyList(),
                        practicalSteps = b.practical_steps ?: emptyList(),
                        communicationScripts = b.communication_scripts ?: emptyList(),
                        positiveDisciplineGuidance = b.positive_discipline_guidance ?: "",
                        whatToAvoid = b.what_to_avoid ?: emptyList(),
                        actionPlan = b.action_plan ?: emptyList(),
                        reflectionQuestions = b.reflection_questions ?: emptyList(),
                        followUpQuestion = b.follow_up_question ?: "",
                        professionalSupportGuidance = b.professional_support_guidance,
                        safetyFlags = b.safety_flags ?: emptyList(),
                        provider = b.provider,
                        model = b.model,
                        routingReason = b.routing_reason,
                        suggestedPrompts = getGuidanceModules()
                            .firstOrNull { it.id.equals("COMPANION", ignoreCase = true) }
                            ?.suggestedQuestions
                            .orEmpty()
                    )
                )
            } else {
                offlineFallback.sendCompanionQuery(message).collect { emit(it) }
            }
        } catch (ex: Exception) {
            offlineFallback.sendCompanionQuery(message).collect { emit(it) }
        }
    }


    override suspend fun getGuidanceModules(): List<GuidanceModuleMetadata> {
        return runCatching {
            val response = NetworkClient.apiService.getGuidanceModules()
            if (response.isSuccessful) {
                response.body()?.modules.orEmpty().map { module ->
                    GuidanceModuleMetadata(
                        id = module.id,
                        name = module.name,
                        role = module.role,
                        mission = module.mission,
                        promptVersion = module.prompt_version,
                        suggestedQuestions = module.suggested_questions
                    )
                }
            } else {
                emptyList()
            }
        }.getOrElse {
            emptyList()
        }
    }
    override fun getDailyReflectionTip(): String {
        return offlineFallback.getDailyReflectionTip()
    }
}
