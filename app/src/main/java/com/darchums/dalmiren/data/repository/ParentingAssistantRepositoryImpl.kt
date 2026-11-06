package com.darchums.dalmiren.data.repository

import com.darchums.dalmiren.domain.model.AgeGroup
import com.darchums.dalmiren.domain.model.ChatMessage
import com.darchums.dalmiren.domain.model.GuidanceResult
import com.darchums.dalmiren.domain.model.MessageSender
import com.darchums.dalmiren.domain.model.ParentingCategory
import com.darchums.dalmiren.domain.model.ParentingSituation
import com.darchums.dalmiren.domain.repository.ParentingAssistantRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.util.UUID

class ParentingAssistantRepositoryImpl : ParentingAssistantRepository {

    override suspend fun analyzeBehaviour(situation: ParentingSituation): GuidanceResult {
        delay(600) // Simulate thoughtful AI analysis
        val descLower = situation.description.lowercase()

        val hypotheses = mutableListOf<String>()
        val emotionalNeeds = mutableListOf<String>()
        val nextObservations = mutableListOf<String>()

        if (descLower.contains("refus") || descLower.contains("no") || descLower.contains("listen")) {
            hypotheses.add("Desire for autonomy: Testing personal control and decision-making boundaries.")
            hypotheses.add("Transition overload: Difficulty switching from a high-engagement task to a routine activity.")
            hypotheses.add("Unmet fatigue or physical need: Energy depletion making compliance mentally taxing.")
            emotionalNeeds.add("A need to feel heard and respected before accepting direction.")
            emotionalNeeds.add("Predictability and advance warning before activity transitions.")
            nextObservations.add("Note if refusals happen at specific times of day (e.g., bedtime or mealtime).")
            nextObservations.add("Observe whether providing 2 choice options reduces resistance.")
        } else if (descLower.contains("cry") || descLower.contains("tantrum") || descLower.contains("scream") || descLower.contains("frustrat")) {
            hypotheses.add("Emotional dysregulation: Feeling overwhelmed by emotions beyond current coping capacities.")
            hypotheses.add("Communication gap: Struggling to express complex internal feelings in words.")
            hypotheses.add("Sensory or environmental overload: Too much noise, stimulation, or tiredness.")
            emotionalNeeds.add("Co-regulation: A calm adult presence to help restore equilibrium.")
            emotionalNeeds.add("Validation that intense feelings are safe to express.")
            nextObservations.add("Observe how long recovery takes when you stay calm versus offering verbal explanations.")
            nextObservations.add("Track sensory triggers prior to emotional outbursts.")
        } else {
            hypotheses.add("Exploratory behaviour: Learning limits through trial and feedback.")
            hypotheses.add("Connection seeking: Attempting to engage adult attention or interaction.")
            hypotheses.add("Developmental milestone testing: Practicing emerging emotional or motor skills.")
            emotionalNeeds.add("Consistent, warm, and predictable adult presence.")
            emotionalNeeds.add("Clear, encouraging boundaries delivered without anger.")
            nextObservations.add("Notice what immediately precedes this behaviour during daily routines.")
            nextObservations.add("Check if quality one-on-one time earlier in the day impacts frequency.")
        }

        val calmResponse = when (situation.ageGroup) {
            AgeGroup.TODDLER -> "Get down to eye level, keep your voice soft and steady. Say: 'I see you're feeling big feelings. I am right here with you.' Avoid long lectures; use simple physical comfort or gentle redirection."
            AgeGroup.EARLY_CHILDHOOD -> "Acknowledge feelings first: 'It's tough when play time ends.' Offer a limited choice: 'Would you like to put away the red blocks or the blue blocks first?' Maintain the boundary firmly and calmly."
            AgeGroup.MIDDLE_CHILDHOOD -> "Pause and validate: 'I understand why you're upset about this limit.' Give them 5 minutes of cool-down time, then invite collaborative problem solving: 'How can we solve this together?'"
            AgeGroup.ADOLESCENT -> "Maintain respectful posture: 'I hear your point of view, even if we disagree on the rule.' Offer a calm pause: 'Let's talk about this in 15 minutes when we can both discuss it peacefully.'"
        }

        return GuidanceResult(
            title = "Behaviour Insight (${situation.ageGroup.label})",
            category = ParentingCategory.BEHAVIOUR,
            hypotheses = hypotheses,
            emotionalNeeds = emotionalNeeds,
            calmResponse = calmResponse,
            nextObservations = nextObservations,
            practicalTip = "Focus on connection before correction. Validate emotion while holding the behavioral boundary steady."
        )
    }

    override suspend fun getEmotionalGuidance(ageGroup: AgeGroup, emotionalContext: String): GuidanceResult {
        delay(500)
        return GuidanceResult(
            title = "Emotional Intelligence Coaching",
            category = ParentingCategory.EMOTIONAL,
            hypotheses = listOf(
                "Emotions precede logic: The emotional nervous system is triggered before verbal reasoning activates.",
                "Child may be feeling misunderstood or unable to label their internal state."
            ),
            emotionalNeeds = listOf(
                "Safety to express distress without punishment.",
                "Empathetic validation that all feelings are acceptable."
            ),
            calmResponse = "Name the feeling without judging: 'You feel disappointed because we can't go to the park today. It's okay to feel disappointed.' Sit close quietly.",
            nextObservations = listOf(
                "Notice if naming emotions helps lower physiological tension (breathing rate, posture).",
                "Observe if your child starts using emotion words self-reflexively over time."
            ),
            practicalTip = "Validation does not mean agreeing with unacceptable actions. You can accept feelings while stopping hurtful actions."
        )
    }

    override suspend fun getCommunicationScript(ageGroup: AgeGroup, scenario: String): GuidanceResult {
        delay(500)
        return GuidanceResult(
            title = "Respectful Communication Strategy",
            category = ParentingCategory.COMMUNICATION,
            hypotheses = listOf(
                "Long explanations often overwhelm children when they are elevated.",
                "Clear, affirmative statements ('Do X') work better than negative commands ('Don't do Y')."
            ),
            emotionalNeeds = listOf(
                "Clarity, respect, and non-judgmental dialogue.",
                "Reconnection after disagreements."
            ),
            calmResponse = "Try phrase: 'I love you, and the rule is hands are for helping, not hitting. Let's practice taking a deep breath together.' Follow up with repair when both of you are calm.",
            nextObservations = listOf(
                "Track how your tone of voice alters your child's defensive reaction.",
                "Check whether eye contact and proximity improve listening response."
            ),
            practicalTip = "Use the 'Say What You Want to See' rule: specify the positive action desired."
        )
    }

    override suspend fun getPositiveDisciplineStrategy(ageGroup: AgeGroup, conflictType: String): GuidanceResult {
        delay(500)
        return GuidanceResult(
            title = "Positive Discipline & Boundary Setting",
            category = ParentingCategory.DISCIPLINE,
            hypotheses = listOf(
                "Punishment causes fear or secrecy, whereas positive discipline teaches self-regulation.",
                "Boundaries provide emotional safety when enforced with consistency and warmth."
            ),
            emotionalNeeds = listOf(
                "Predictable, fair boundaries without humiliation or threats.",
                "Opportunities to make amends and learn from mistakes."
            ),
            calmResponse = "Establish a logical consequence: 'When toys are thrown unsafely, they rest in the closet until tomorrow. We can try again together in the morning.'",
            nextObservations = listOf(
                "Observe if consistent logical consequences reduce recurring conflict over time.",
                "Ensure consequences are related, respectful, and reasonable."
            ),
            practicalTip = "Firmness and kindness coexist. You can be loving and clear at the exact same moment."
        )
    }

    override suspend fun getLearningSupportStrategy(ageGroup: AgeGroup, goalArea: String): GuidanceResult {
        delay(500)
        return GuidanceResult(
            title = "Learning & Curiosity Nurturing",
            category = ParentingCategory.LEARNING,
            hypotheses = listOf(
                "Intrinsic motivation flourishes when children feel autonomous and supported.",
                "Frustration during learning often indicates a need to break tasks into smaller micro-steps."
            ),
            emotionalNeeds = listOf(
                "Encouragement of effort over perfection.",
                "A safe space to make mistakes as a natural part of growth."
            ),
            calmResponse = "Praise effort specifically: 'I noticed how hard you worked to solve that step!' Break learning sessions into 10-15 minute focused intervals.",
            nextObservations = listOf(
                "Observe what time of day your child displays the highest natural focus.",
                "Notice if playful or hands-on approaches reduce task resistance."
            ),
            practicalTip = "Focus on process praise ('You tried three different ways!') rather than fixed talent labels."
        )
    }

    override suspend fun sendCompanionQuery(message: String): Flow<ChatMessage> = flow {
        val queryLower = message.lowercase()
        delay(600)

        val responseText = when {
            queryLower.contains("who are you") || queryLower.contains("what are you") -> {
                "I am DALMIREN AI, your conversational parenting companion developed by DARCHUMS. My tagline is 'Understand. Guide. Nurture.' I help parents and guardians understand children's developmental needs, navigate everyday parenting challenges, strengthen parent-child relationships, and develop practical, positive parenting strategies. My guidance is supportive, evidence-informed, non-diagnostic, and focused on children's wellbeing and safety."
            }
            queryLower.contains("what can you help") || queryLower.contains("capabilities") -> {
                "I can help you with child development, positive discipline, emotional wellbeing, parent-child communication, learning support, family routines, digital parenting, and everyday parenting challenges. How can I support your family today?"
            }
            queryLower.contains("confidence") || queryLower.contains("confident") -> {
                "Building your child's confidence starts with helping them feel valued, capable, and safe to try new things.\n\nHere are five practical ways to begin:\n1. Recognise effort, not just achievement (praise persistence and problem-solving).\n2. Give age-appropriate responsibilities to build independence.\n3. Listen without immediately correcting or dismissing their feelings.\n4. Encourage manageable challenges and support them through difficulty.\n5. Avoid comparisons with other children.\n\nScript: 'I noticed how hard you worked on that. What did you learn from trying?'"
            }
            queryLower.contains("homework") || queryLower.contains("school") -> {
                "When a child refuses homework, they are often experiencing cognitive fatigue, frustration, or lack of autonomy. Try these steps:\n1. Establish a predictable transition routine after school (snack and quiet break first).\n2. Break tasks into manageable 15-minute intervals.\n3. Offer choices on where or in what order to start.\n4. Focus on effort and problem-solving rather than perfection."
            }
            queryLower.contains("anger") || queryLower.contains("mad") || queryLower.contains("frustrat") -> {
                "Managing anger and frustration begins with co-regulation:\n1. Stay calm and grounded as an emotional anchor.\n2. Validate the feeling: 'It is okay to feel angry, but it is not okay to hurt others.'\n3. Pause before problem-solving until physiological arousal subsides.\n4. Teach calming breathing techniques (e.g., 'flower breath' and 'candle breath')."
            }
            queryLower.contains("tantrum") || queryLower.contains("meltdown") -> {
                "During a tantrum, children's emotional brains are overloaded and verbal reasoning is offline:\n1. Ensure physical safety first.\n2. Stay calm and keep your voice low and steady.\n3. Avoid long lectures or reasoning while the child is elevated.\n4. Offer silent presence or gentle touch if welcomed, and address problem-solving later once calm."
            }
            queryLower.contains("teenager") || queryLower.contains("communication") || queryLower.contains("talk") -> {
                "Improving communication with adolescents requires respectful listening and lower pressure:\n1. Ask open-ended questions and listen without immediate judgment or interruption.\n2. Respect their growing need for privacy and autonomy.\n3. Focus on collaboration rather than unilateral control during disagreements."
            }
            queryLower.contains("refus") || queryLower.contains("no") -> {
                "When a child repeatedly refuses instructions, they are often exercising autonomy or struggling with transitions. Try connecting before directing, giving advance notice, and offering structured choices."
            }
            else -> {
                "Thank you for reaching out. Based on your question, here is practical guidance:\n\n1. **Acknowledge and Validate**: Connect with your child's emotional state before addressing behaviour.\n2. **Set Clear Boundaries**: Use calm, affirmative statements regarding expected actions.\n3. **Provide Choice**: Offer two positive options to foster autonomy.\n\nHow can I help you tailor this approach to your specific situation?"
            }
        }

        val suggested = listOf(
            "Help me set a calm boundary.",
            "How can I respond when my child gets frustrated?",
            "How can I talk to my child after an argument?",
            "How can I encourage my child's confidence?"
        )

        emit(
            ChatMessage(
                id = UUID.randomUUID().toString(),
                sender = MessageSender.ASSISTANT,
                text = responseText,
                suggestedPrompts = suggested
            )
        )
    }

    override fun getDailyReflectionTip(): String {
        val tips = listOf(
            "Connect before you correct: A calm eye-level glance builds trust before giving directions.",
            "All feelings are valid; not all behaviors are acceptable. Name the feeling, guide the action.",
            "Your emotional state sets the tone. Take a deep breath to co-regulate before responding.",
            "Praise process and effort, not perfection. Curiosity thrives in a safe learning space."
        )
        val dayIndex = (System.currentTimeMillis() / (1000 * 60 * 60 * 24) % tips.size).toInt()
        return tips[dayIndex]
    }
}
