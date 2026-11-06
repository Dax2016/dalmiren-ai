package com.darchums.dalmiren.domain.model

enum class AgeGroup(val label: String, val description: String) {
    TODDLER("1 - 3 years", "Early autonomy, emotional discovery, and motor play"),
    EARLY_CHILDHOOD("4 - 6 years", "Curiosity, boundary testing, and social learning"),
    MIDDLE_CHILDHOOD("7 - 11 years", "Peer relationships, self-esteem, and complex reasoning"),
    ADOLESCENT("12 - 17 years", "Identity formation, independence, and emotional processing")
}

enum class ParentingCategory(val title: String) {
    BEHAVIOUR("Behaviour Intelligence"),
    EMOTIONAL("Emotional Intelligence"),
    COMMUNICATION("Communication Guidance"),
    DISCIPLINE("Positive Discipline"),
    LEARNING("Learning Support")
}

data class GuidanceModuleMetadata(
    val id: String,
    val name: String,
    val role: String,
    val mission: String,
    val promptVersion: String,
    val suggestedQuestions: List<String> = emptyList()
)

data class ParentingSituation(
    val description: String,
    val ageGroup: AgeGroup = AgeGroup.EARLY_CHILDHOOD,
    val triggers: String = "",
    val context: String = "",
    val observations: String = ""
)

data class GuidanceResult(
    val title: String,
    val category: ParentingCategory,
    val hypotheses: List<String>,
    val emotionalNeeds: List<String>,
    val calmResponse: String,
    val nextObservations: List<String>,
    val practicalTip: String,
    val disclaimer: String = "Hypotheses provided for guidance. Not a medical or psychological diagnosis."
)

enum class MessageSender {
    USER,
    ASSISTANT
}

data class ChatMessage(
    val id: String,
    val sender: MessageSender,
    val text: String,
    val timestamp: Long = System.currentTimeMillis(),
    val suggestedPrompts: List<String> = emptyList(),
    val responseSummary: String = "",
    val empathyStatement: String = "",
    val understanding: String = "",
    val possibleContributingFactors: List<String> = emptyList(),
    val developmentalContext: String = "",
    val recommendations: List<String> = emptyList(),
    val practicalSteps: List<String> = emptyList(),
    val communicationScripts: List<String> = emptyList(),
    val positiveDisciplineGuidance: String = "",
    val whatToAvoid: List<String> = emptyList(),
    val actionPlan: List<String> = emptyList(),
    val reflectionQuestions: List<String> = emptyList(),
    val followUpQuestion: String = "",
    val professionalSupportGuidance: String? = null,
    val safetyFlags: List<String> = emptyList(),
    val provider: String? = null,
    val model: String? = null,
    val routingReason: String? = null
)

data class ParentReflection(
    val id: String,
    val category: ParentingCategory,
    val title: String,
    val notes: String,
    val timestamp: Long = System.currentTimeMillis(),
    val feelingRating: Int = 3 // 1 (Challenging) to 5 (Connected & Peaceful)
)

data class UserPreferences(
    val onboardingCompleted: Boolean = false,
    val defaultAgeGroup: AgeGroup = AgeGroup.EARLY_CHILDHOOD,
    val darkTheme: Boolean? = null
)
