import time
import uuid
import json
import logging
from typing import List, Dict, Any, Optional
from sqlalchemy.orm import Session
from models.db_models import User, ChildProfile, Conversation, Message, GuidanceSession, FollowUpRecord, ParentingGoal, ConsentRecord
from providers.base import AIProviderRequest, AIProvider
from services.memory_nexus import MemoryNexus
from services.routing_engine import RoutingPolicyEngine, TaskIntent
from prompts.prompt_registry import PromptRegistry

logger = logging.getLogger("IntelligenceLoops")

class IntelligenceLoopsService:
    def __init__(self, db: Session, routing_engine: RoutingPolicyEngine):
        self.db = db
        self.routing_engine = routing_engine
        self.memory_nexus = MemoryNexus(db)

    # LOOP 1: Conversation Management Loop
    def get_or_create_conversation(self, user_id: str, conversation_id: Optional[str] = None, child_id: Optional[str] = None) -> Conversation:
        # The caller must supply an existing, authenticated user ID.
        user = self.db.query(User).filter(User.id == user_id).first()
        if not user:
            raise LookupError("Authenticated user does not exist.")

        # A child profile must belong to this authenticated parent.
        if child_id:
            child = (
                self.db.query(ChildProfile)
                .filter(
                    ChildProfile.id == child_id,
                    ChildProfile.user_id == user_id,
                )
                .first()
            )
            if not child:
                raise LookupError("Child profile not found.")

        if conversation_id:
            conv = (
                self.db.query(Conversation)
                .filter(
                    Conversation.id == conversation_id,
                    Conversation.user_id == user_id,
                )
                .first()
            )

            # Never silently create a new conversation for a foreign or unknown ID.
            if not conv:
                raise LookupError("Conversation not found.")

            if child_id and conv.child_id and conv.child_id != child_id:
                raise LookupError("Conversation belongs to a different child profile.")

            if child_id and not conv.child_id:
                conv.child_id = child_id
                self.db.commit()

            return conv

        conv = Conversation(
            user_id=user_id,
            child_id=child_id,
            title="Parenting Consultation",
        )
        self.db.add(conv)
        self.db.commit()
        self.db.refresh(conv)
        return conv

    def add_message(self, conversation_id: str, sender: str, text: str, provider_used: str = None, model_used: str = None, routing_reason: str = None) -> Message:
        msg = Message(
            conversation_id=conversation_id,
            sender=sender,
            text=text,
            provider_used=provider_used,
            model_used=model_used,
            routing_reason=routing_reason
        )
        self.db.add(msg)

        conv = self.db.query(Conversation).filter(Conversation.id == conversation_id).first()
        if conv:
            conv.updated_at = int(time.time() * 1000)

        self.db.commit()
        self.db.refresh(msg)
        return msg

    # LOOP 6: Safety & Safeguarding Check
    def check_safety(self, prompt: str) -> Optional[str]:
        text = prompt.lower()

        crisis_terms = (
            "suicide", "kill myself", "killing myself", "end my life",
            "self-harm", "self harm", "hurt myself", "want to die",
            "going to hurt", "going to kill", "threatened to kill",
            "child is being abused", "child abuse", "sexual abuse",
            "being exploited", "child exploitation", "severe neglect",
            "abandoned child", "physical violence", "domestic violence",
            "immediate danger", "unsafe right now"
        )

        emergency_terms = (
            "medical emergency", "poisoning", "swallowed poison",
            "overdose", "unconscious", "not breathing", "choking",
            "cannot breathe", "can't breathe", "severe bleeding",
            "severe injury", "seizure", "unresponsive"
        )

        if any(term in text for term in crisis_terms):
            return (
                "SAFETY AND SAFEGUARDING NOTICE: Your safety and your child's safety come first. "
                "If anyone is in immediate danger, move to a safer place if possible and contact "
                "your local emergency services now. If a child may be experiencing abuse, neglect "
                "or exploitation, contact the appropriate local child-protection or safeguarding "
                "authority. If you may harm yourself or someone else, seek immediate in-person "
                "help from emergency services or a trusted crisis professional. "
                "DALMIREN AI is not an emergency response service."
            )

        if any(term in text for term in emergency_terms):
            return (
                "MEDICAL EMERGENCY NOTICE: Contact your local emergency medical services immediately. "
                "For suspected poisoning, overdose, choking, breathing difficulty, unconsciousness "
                "or severe injury, do not wait for an AI response. Follow instructions from emergency "
                "professionals. DALMIREN AI cannot diagnose or manage medical emergencies."
            )

        return None

    def check_cloud_consent(self, user_id: str) -> bool:
        consent = self.db.query(ConsentRecord).filter(
            ConsentRecord.user_id == user_id,
            ConsentRecord.consent_type == "CLOUD_PROCESSING"
        ).order_by(ConsentRecord.timestamp.desc()).first()
        if consent:
            return bool(consent.granted)
        return False

    async def _structure_response_with_ai(
        self,
        ai_response_text: str,
        prompt: str,
        age_group: str,
        provider: AIProvider,
        request_obj: AIProviderRequest
    ) -> Dict[str, Any]:
        schema_instruction = (
            "Based on the parenting guidance advice provided above, output a STRICT JSON object with these exact keys:\n"
            "{\n"
            '  "response_summary": "string",\n'
            '  "empathy_statement": "string",\n'
            '  "understanding": "string",\n'
            '  "possible_contributing_factors": ["string"],\n'
            '  "developmental_context": "string",\n'
            '  "recommendations": ["string"],\n'
            '  "practical_steps": ["string"],\n'
            '  "communication_scripts": ["string"],\n'
            '  "positive_discipline_guidance": "string",\n'
            '  "what_to_avoid": ["string"],\n'
            '  "action_plan": ["string"],\n'
            '  "reflection_questions": ["string"],\n'
            '  "follow_up_question": "string",\n'
            '  "professional_support_guidance": "string or null",\n'
            '  "safety_flags": ["string"]\n'
            "}\n"
            "Do NOT use hardcoded generic defaults. Extract genuine insights directly from the AI response text."
        )

        struct_request = AIProviderRequest(
            prompt=f"Original Prompt: {prompt}\nAI Answer: {ai_response_text}\n\n{schema_instruction}",
            system_prompt=request_obj.system_prompt,
            temperature=0.2,
            max_tokens=1000
        )

        try:
            structured_data = await provider.generate_structured_output(struct_request, {})
            if isinstance(structured_data, dict) and (len(structured_data) > 1 and "response_summary" in structured_data or "recommendations" in structured_data):
                return {
                    "response_summary": structured_data.get("response_summary", ai_response_text[:120]),
                    "empathy_statement": structured_data.get("empathy_statement", "We hear your parenting journey and acknowledge your dedication."),
                    "understanding": structured_data.get("understanding", ai_response_text[:200]),
                    "possible_contributing_factors": structured_data.get("possible_contributing_factors", []),
                    "developmental_context": structured_data.get("developmental_context", f"Context for {age_group}"),
                    "recommendations": structured_data.get("recommendations", []),
                    "practical_steps": structured_data.get("practical_steps", []),
                    "communication_scripts": structured_data.get("communication_scripts", []),
                    "positive_discipline_guidance": structured_data.get("positive_discipline_guidance", ""),
                    "what_to_avoid": structured_data.get("what_to_avoid", []),
                    "action_plan": structured_data.get("action_plan", []),
                    "reflection_questions": structured_data.get("reflection_questions", []),
                    "follow_up_question": structured_data.get("follow_up_question", "How did your child respond to this approach?"),
                    "professional_support_guidance": structured_data.get("professional_support_guidance"),
                    "safety_flags": structured_data.get("safety_flags", [])
                }
        except Exception as ex:
            logger.warning(f"Structured output parsing failed: {ex}. Using safe fallback preserving original text.")

        return {
            "response_summary": ai_response_text[:120] if len(ai_response_text) > 120 else ai_response_text,
            "empathy_statement": "Parenting requires profound patience and presence. Thank you for seeking constructive guidance.",
            "understanding": ai_response_text,
            "possible_contributing_factors": [],
            "developmental_context": f"Developmental stage: {age_group}",
            "recommendations": [],
            "practical_steps": [],
            "communication_scripts": [],
            "positive_discipline_guidance": "",
            "what_to_avoid": [],
            "action_plan": [],
            "reflection_questions": [],
            "follow_up_question": "What worked well when you applied this guidance?",
            "professional_support_guidance": None,
            "safety_flags": []
        }

    async def process_chat(
        self,
        user_id: str,
        prompt: str,
        conversation_id: Optional[str] = None,
        child_id: Optional[str] = None,
        module: str = "COMPANION"
    ) -> Dict[str, Any]:
        response_uuid = str(uuid.uuid4())

        # Loop 6: Safety check
        safety_notice = self.check_safety(prompt)
        if safety_notice:
            conv = self.get_or_create_conversation(user_id, conversation_id, child_id)
            self.add_message(conv.id, "USER", prompt)
            self.add_message(conv.id, "ASSISTANT", safety_notice, provider_used="SafetyGuard", model_used="RuleEngine", routing_reason="Rule F Safety Intervention")
            return {
                "response_id": response_uuid,
                "conversation_id": conv.id,
                "message": safety_notice,
                "response_summary": "Safety & Safeguarding Guidance",
                "empathy_statement": "Your family's safety and well-being are paramount.",
                "understanding": "Immediate safety or emergency concern identified.",
                "possible_contributing_factors": ["Emergency or crisis situation"],
                "developmental_context": "Immediate safety prioritization.",
                "recommendations": ["Contact emergency services or medical professionals immediately."],
                "practical_steps": ["Contact local emergency services immediately if anyone is in immediate danger."],
                "communication_scripts": ["\"I need help right now.\""],
                "positive_discipline_guidance": "Focus strictly on immediate safety.",
                "what_to_avoid": ["Delaying professional medical or emergency response."],
                "action_plan": ["Reach out to emergency authorities."],
                "reflection_questions": [],
                "follow_up_question": "Are you and your child in a safe environment right now?",
                "professional_support_guidance": "Contact local emergency responders or crisis helplines.",
                "safety_flags": ["CRISIS_INTERVENTION"],
                "provider": "SafetyGuard",
                "model": "RuleEngine",
                "routing_reason": "Rule F Safety Intervention",
                "is_fallback": False,
                "latency_ms": 0.0,
                "prompt_version": PromptRegistry.get_version()
            }

        # Loop 1: Conversation setup & authentication check
        conv = self.get_or_create_conversation(user_id, conversation_id, child_id)
        self.add_message(conv.id, "USER", prompt)

        history_msgs = self.db.query(Message).filter(Message.conversation_id == conv.id).order_by(Message.timestamp.asc()).all()
        history_list = [{"role": ("user" if m.sender == "USER" else "assistant"), "content": m.text} for m in history_msgs[:-1]]

        # Loop 2: Retrieve memory context
        memory_contexts = self.memory_nexus.get_memory_context_strings(user_id, child_id=child_id)

        child_info = ""
        age_group = "EARLY_CHILDHOOD"
        if child_id:
            child = self.db.query(ChildProfile).filter(ChildProfile.id == child_id, ChildProfile.user_id == user_id).first()
            if child:
                child_info = f"Child Name: {child.name}, Age Group: {child.age_group}, Notes: {child.notes or 'None'}."
                age_group = child.age_group

        system_prompt = PromptRegistry.get_module_system_prompt(module, child_info)

        request = AIProviderRequest(
            prompt=prompt,
            system_prompt=system_prompt,
            conversation_history=history_list,
            memory_context=memory_contexts,
            temperature=0.7,
            max_tokens=1000
        )

        has_cloud_consent = self.check_cloud_consent(user_id)

        # Loop 3: Consent-aware routing. Missing consent means local-only.
        ai_response = await self.routing_engine.execute_routed_request(
            request,
            memory_count=len(memory_contexts),
            history_length=len(history_list),
            allow_cloud=has_cloud_consent
        )

        # Identify the provider that actually answered, including fallback.
        if ai_response.provider_name == "OpenAI":
            active_provider = self.routing_engine.openai_provider
        elif ai_response.provider_name == "LocalAI":
            active_provider = self.routing_engine.local_provider
        else:
            raise RuntimeError(
                f"Unrecognized AI provider response: {ai_response.provider_name}"
            )

        # Loop 1: Persist assistant message
        self.add_message(
            conv.id,
            "ASSISTANT",
            ai_response.text,
            provider_used=ai_response.provider_name,
            model_used=ai_response.model_name,
            routing_reason=ai_response.routing_reason
        )

        # Loop 2: Improved Memory Extraction
        p_lower = prompt.lower()
        if any(kw in p_lower for kw in ["my child loves", "is allergic to", "diagnosed with", "prefers", "always struggles with"]):
            self.memory_nexus.create_memory(
                user_id=user_id,
                child_id=child_id,
                memory_type="SEMANTIC",
                category="CHILD_PREFERENCE_OR_FACT",
                content=f"Confirmed parenting observation: '{prompt}'",
                confidence=0.9
            )

        # Loop 5: Persistence for Action Plan / Goals
        if "goal" in p_lower or "plan" in p_lower:
            goal = ParentingGoal(
                user_id=user_id,
                child_id=child_id,
                title=prompt[:60],
                description=prompt,
                status="ACTIVE"
            )
            self.db.add(goal)
            self.db.commit()

        structured = await self._structure_response_with_ai(
            ai_response.text,
            prompt,
            age_group,
            active_provider,
            request
        )

        result = {
            "response_id": response_uuid,
            "conversation_id": conv.id,
            "message": ai_response.text,
            "provider": ai_response.provider_name,
            "model": ai_response.model_name,
            "routing_reason": ai_response.routing_reason,
            "is_fallback": ai_response.is_fallback,
            "latency_ms": ai_response.latency_ms,
            "prompt_version": PromptRegistry.get_version()
        }
        result.update(structured)
        return result
