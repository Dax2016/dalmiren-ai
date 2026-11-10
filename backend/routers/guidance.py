import json
import re
import logging
from typing import Optional, List
from fastapi import APIRouter, Depends, HTTPException
from pydantic import BaseModel, Field
from sqlalchemy.orm import Session
from database import get_db
from models.db_models import GuidanceSession, ChildProfile, User
from routers.auth import get_current_user
from services.memory_nexus import MemoryNexus
from services.intelligence_loops import IntelligenceLoopsService
from providers.base import AIProviderRequest
from prompts.prompt_registry import PromptRegistry

router = APIRouter(prefix="/api/guidance", tags=["Specialist Guidance Modules"])
logger = logging.getLogger("SpecialistGuidance")


@router.get("/modules")
def get_guidance_modules():
    """Return the authoritative metadata and suggested questions for all modules."""
    module_ids = [
        "COMPANION",
        "BEHAVIOUR",
        "EMOTIONAL",
        "COMMUNICATION",
        "DISCIPLINE",
        "LEARNING"
    ]

    modules = []

    for module_id in module_ids:
        metadata = PromptRegistry.get_module_metadata(module_id)
        metadata["suggested_questions"] = (
            PromptRegistry.get_suggested_questions(module_id)
        )
        modules.append(metadata)

    return {
        "brand": "DALMIREN AI by DARCHUMS",
        "prompt_registry_version": PromptRegistry.get_version(),
        "total_modules": len(modules),
        "modules": modules
    }


class BehaviourRequest(BaseModel):
    child_id: Optional[str] = None
    age_group: str = "EARLY_CHILDHOOD"
    description: str
    triggers: Optional[str] = ""
    context: Optional[str] = ""
    observations: Optional[str] = ""

class GuidanceResponse(BaseModel):
    title: str
    category: str
    hypotheses: List[str]
    emotional_needs: List[str]
    calm_response: str
    next_observations: List[str]
    practical_tip: str
    understanding: str = ""
    contributing_factors: List[str] = Field(default_factory=list)
    developmental_context: str = ""
    practical_steps: List[str] = Field(default_factory=list)
    communication_scripts: List[str] = Field(default_factory=list)
    prevention_strategies: List[str] = Field(default_factory=list)
    what_to_avoid: List[str] = Field(default_factory=list)
    follow_up_plan: List[str] = Field(default_factory=list)
    professional_support_guidance: str = ""
    disclaimer: str = "Guidance provided by DALMIREN AI specialist agent. Not a medical or psychological diagnosis."


async def run_specialist_agent(
    db: Session,
    req: BehaviourRequest,
    user_id: str,
    specialist_name: str,
    category: str
) -> GuidanceResponse:
    # Validate child ownership before safety processing, memory access or AI.
    if req.child_id:
        child = db.query(ChildProfile).filter(
            ChildProfile.id == req.child_id,
            ChildProfile.user_id == user_id
        ).first()
        if not child:
            raise HTTPException(status_code=404, detail="Child profile not found.")

    from main import routing_engine

    if not routing_engine:
        raise HTTPException(status_code=503, detail="Routing engine not initialized")

    loops_service = IntelligenceLoopsService(db, routing_engine)

    # Screen for urgent safety concerns before any AI processing.
    safety_text = "\n".join([
        req.description or "",
        req.triggers or "",
        req.context or "",
        req.observations or ""
    ])
    safety_notice = loops_service.check_safety(safety_text)

    if safety_notice:
        return GuidanceResponse(
            title="Safety Guidance",
            category=category,
            hypotheses=["The situation may require immediate human support."],
            emotional_needs=[
                "Immediate safety, protection and appropriate professional support."
            ],
            calm_response=safety_notice,
            next_observations=[
                "Prioritise immediate safety.",
                "Follow guidance from local emergency or safeguarding professionals."
            ],
            practical_tip=(
                "Do not delay contacting appropriate local emergency "
                "or child-protection services."
            ),
            disclaimer=(
                "Safety notice only. DALMIREN AI is not an emergency, "
                "medical or safeguarding response service."
            )
        )

    memory_nexus = MemoryNexus(db)
    memory_contexts = memory_nexus.get_memory_context_strings(
        user_id,
        child_id=req.child_id
    )

    child_info = ""
    if req.child_id:
        child_info_record = db.query(ChildProfile).filter(
            ChildProfile.id == req.child_id,
            ChildProfile.user_id == user_id
        ).first()
        child_info = f"Child Name: {child_info_record.name}, Age Group: {child_info_record.age_group}."

    specialist_system = (
        PromptRegistry.get_module_system_prompt(category, child_info)
        + "\n\n"
        + "Return strictly one valid JSON object matching this schema. "
          "Do not include markdown fences or additional text:\n"
          "{"
          '"title":"string",'
          '"understanding":"string",'
          '"hypotheses":["string"],'
          '"contributing_factors":["string"],'
          '"emotional_needs":["string"],'
          '"developmental_context":"string",'
          '"calm_response":"string",'
          '"practical_steps":["string"],'
          '"communication_scripts":["string"],'
          '"next_observations":["string"],'
          '"prevention_strategies":["string"],'
          '"what_to_avoid":["string"],'
          '"follow_up_plan":["string"],'
          '"professional_support_guidance":"string",'
          '"practical_tip":"string"'
          "}\n\n"
        + "Distinguish reported facts from hypotheses; explain why each approach may help; "
          "adapt to the stated developmental stage; provide specific practical steps and "
          "natural example phrases; include prevention and follow-up; avoid diagnosis, "
          "blame, punishment and unsupported assumptions. State uncertainty when context is limited."
    )

    prompt_content = (
        f"Specialist Category: {category}\n"
        f"Age Group: {req.age_group}\n"
        f"Situation Description: {req.description}\n"
        f"Triggers: {req.triggers}\n"
        f"Context: {req.context}\n"
        f"Observations: {req.observations}\n\n"
        "Analyze this situation through your assigned specialist role. "
        "Offer practical, developmentally appropriate, non-diagnostic guidance. "
        "Do not assume facts that the caregiver has not provided."
    )

    ai_request = AIProviderRequest(
        prompt=prompt_content,
        system_prompt=specialist_system,
        memory_context=memory_contexts,
        temperature=0.4,
        max_tokens=1000
    )

    # Missing or declined consent means local-only processing.
    has_cloud_consent = loops_service.check_cloud_consent(user_id)

    try:
        ai_response = await routing_engine.execute_routed_request(
            ai_request,
            memory_count=len(memory_contexts),
            history_length=0,
            allow_cloud=has_cloud_consent,
            prefer_cloud=True
        )

        response_text = ai_response.text.strip()
        if response_text.startswith("```"):
            response_text = re.sub(
                r"^```(?:json)?\s*", "", response_text,
                count=1, flags=re.I
            )
            response_text = re.sub(r"\s*```$", "", response_text, count=1)

        data = json.loads(response_text)
        if not isinstance(data, dict):
            raise ValueError("Specialist response must be a JSON object.")

        title = str(
            data.get("title") or f"{specialist_name} Guidance ({req.age_group})"
        )
        hypotheses = data.get("hypotheses") or [
            "Consider the child's developmental stage and the surrounding circumstances."
        ]
        emotional_needs = data.get("emotional_needs") or [
            "Consistent and warm adult support.",
            "Emotional validation."
        ]
        calm_response = str(
            data.get("calm_response")
            or "Acknowledge feelings gently and maintain calm, consistent boundaries."
        )
        next_observations = data.get("next_observations") or [
            "Notice patterns, triggers and what helps the child feel supported."
        ]
        practical_tip = str(
            data.get("practical_tip") or "Focus on connection before correction."
        )

        understanding = str(data.get("understanding") or "")
        contributing_factors = data.get("contributing_factors") or []
        developmental_context = str(data.get("developmental_context") or "")
        practical_steps = data.get("practical_steps") or []
        communication_scripts = data.get("communication_scripts") or []
        prevention_strategies = data.get("prevention_strategies") or []
        what_to_avoid = data.get("what_to_avoid") or []
        follow_up_plan = data.get("follow_up_plan") or []
        professional_support_guidance = str(data.get("professional_support_guidance") or "")

        for field_name, field_value in (
            ("hypotheses", hypotheses),
            ("emotional_needs", emotional_needs),
            ("next_observations", next_observations),
            ("contributing_factors", contributing_factors),
            ("practical_steps", practical_steps),
            ("communication_scripts", communication_scripts),
            ("prevention_strategies", prevention_strategies),
            ("what_to_avoid", what_to_avoid),
            ("follow_up_plan", follow_up_plan)
        ):
            if not isinstance(field_value, list) or not all(
                isinstance(item, str) for item in field_value
            ):
                raise ValueError(f"Invalid {field_name} field.")

    except Exception as ex:
        logger.warning(
            "Specialist inference or response parsing failed for %s: %s",
            category,
            ex
        )
        return GuidanceResponse(
            title=f"{specialist_name} ? Basic Offline Guidance ({req.age_group})",
            category=category,
            hypotheses=[
                "There may be several contributing factors; more context is needed before drawing conclusions."
            ],
            emotional_needs=[
                "A calm, supportive adult presence.",
                "Patience, reassurance and predictable expectations."
            ],
            calm_response=(
                "Stay calm, listen without judgement, acknowledge the child's feelings "
                "and use a respectful, age-appropriate response."
            ),
            next_observations=[
                "Notice what happens before and after the situation.",
                "Observe patterns without labelling or blaming the child."
            ],
            practical_tip=(
                "Use connection before correction. This is basic offline guidance, "
                "not an AI-generated specialist assessment."
            ),
            disclaimer=(
                "Basic offline guidance only. AI inference was unavailable; "
                "this is not a medical or psychological diagnosis."
            )
        )

    # Keep database failures separate from AI inference failures.
    session = GuidanceSession(
        user_id=user_id,
        child_id=req.child_id,
        category=category,
        situation_desc=req.description,
        guidance_title=title,
        calm_response=calm_response,
        practical_tip=practical_tip
    )

    try:
        db.add(session)
        db.commit()
    except Exception:
        db.rollback()
        logger.exception(
            "Could not persist specialist guidance session for %s", category
        )
        raise HTTPException(
            status_code=500,
            detail="Guidance was generated but could not be saved."
        )

    return GuidanceResponse(
        title=title,
        category=category,
        hypotheses=hypotheses,
        emotional_needs=emotional_needs,
        calm_response=calm_response,
        next_observations=next_observations,
        practical_tip=practical_tip,
        understanding=understanding,
        contributing_factors=contributing_factors,
        developmental_context=developmental_context,
        practical_steps=practical_steps,
        communication_scripts=communication_scripts,
        prevention_strategies=prevention_strategies,
        what_to_avoid=what_to_avoid,
        follow_up_plan=follow_up_plan,
        professional_support_guidance=professional_support_guidance
    )


@router.post("/behaviour", response_model=GuidanceResponse)
async def analyze_behaviour(
    req: BehaviourRequest,
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db)
):
    return await run_specialist_agent(
        db, req, current_user.id, "Behaviour Intelligence", "BEHAVIOUR"
    )

@router.post("/emotional", response_model=GuidanceResponse)
async def get_emotional_guidance(
    req: BehaviourRequest,
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db)
):
    return await run_specialist_agent(
        db, req, current_user.id, "Emotional Intelligence Coaching", "EMOTIONAL"
    )

@router.post("/communication", response_model=GuidanceResponse)
async def get_communication_guidance(
    req: BehaviourRequest,
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db)
):
    return await run_specialist_agent(
        db, req, current_user.id, "Respectful Communication Strategy", "COMMUNICATION"
    )

@router.post("/discipline", response_model=GuidanceResponse)
async def get_positive_discipline_strategy(
    req: BehaviourRequest,
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db)
):
    return await run_specialist_agent(
        db, req, current_user.id, "Positive Discipline & Boundaries", "DISCIPLINE"
    )

@router.post("/learning", response_model=GuidanceResponse)
async def get_learning_guidance(
    req: BehaviourRequest,
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db)
):
    return await run_specialist_agent(
        db, req, current_user.id, "Learning & Curiosity Nurturing", "LEARNING"
    )
