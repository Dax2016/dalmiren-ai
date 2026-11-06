import uuid
from typing import Optional, List
from fastapi import APIRouter, Depends, HTTPException
from pydantic import BaseModel, Field
from sqlalchemy.orm import Session
from database import get_db
from models.db_models import Conversation, Message, User
from routers.auth import get_current_user
from services.intelligence_loops import IntelligenceLoopsService
from prompts.prompt_registry import PromptRegistry

router = APIRouter(prefix="/api", tags=["Chat"])

class ChatRequest(BaseModel):
    message: str
    conversation_id: Optional[str] = None
    child_id: Optional[str] = None
    module: str = "COMPANION"

class ChatResponse(BaseModel):
    response_id: str = Field(default_factory=lambda: str(uuid.uuid4()))
    conversation_id: str
    message: str
    response_summary: str = ""
    empathy_statement: str = ""
    understanding: str = ""
    possible_contributing_factors: List[str] = Field(default_factory=list)
    developmental_context: str = ""
    recommendations: List[str] = Field(default_factory=list)
    practical_steps: List[str] = Field(default_factory=list)
    communication_scripts: List[str] = Field(default_factory=list)
    positive_discipline_guidance: str = ""
    what_to_avoid: List[str] = Field(default_factory=list)
    action_plan: List[str] = Field(default_factory=list)
    reflection_questions: List[str] = Field(default_factory=list)
    follow_up_question: str = ""
    professional_support_guidance: Optional[str] = None
    safety_flags: List[str] = Field(default_factory=list)
    provider: str
    model: str
    routing_reason: str
    is_fallback: bool = False
    latency_ms: Optional[float] = None
    prompt_version: str = Field(default_factory=PromptRegistry.get_version)

@router.post("/chat", response_model=ChatResponse)
async def send_chat_message(
    req: ChatRequest,
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db),
):
    from main import routing_engine

    if not routing_engine:
        raise HTTPException(status_code=503, detail="Routing engine not initialized")

    loops_service = IntelligenceLoopsService(db, routing_engine)

    try:
        res = await loops_service.process_chat(
            user_id=current_user.id,
            prompt=req.message,
            conversation_id=req.conversation_id,
            child_id=req.child_id,
            module=req.module,
        )
        return ChatResponse(**res)
    except LookupError as ex:
        raise HTTPException(status_code=404, detail="Conversation or child profile not found.") from ex
    except Exception as ex:
        raise HTTPException(
            status_code=500,
            detail="Unable to process the chat request.",
        ) from ex


@router.get("/conversations")
def get_user_conversations(
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db),
):
    convs = (
        db.query(Conversation)
        .filter(Conversation.user_id == current_user.id)
        .order_by(Conversation.updated_at.desc())
        .all()
    )
    return [
        {
            "id": c.id,
            "title": c.title,
            "created_at": c.created_at,
            "updated_at": c.updated_at,
        }
        for c in convs
    ]


@router.get("/conversations/{conversation_id}")
def get_conversation_messages(
    conversation_id: str,
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db),
):
    conv = (
        db.query(Conversation)
        .filter(
            Conversation.id == conversation_id,
            Conversation.user_id == current_user.id,
        )
        .first()
    )

    if not conv:
        raise HTTPException(status_code=404, detail="Conversation not found")

    messages = (
        db.query(Message)
        .filter(Message.conversation_id == conv.id)
        .order_by(Message.timestamp.asc())
        .all()
    )

    return {
        "id": conv.id,
        "title": conv.title,
        "messages": [
            {
                "id": m.id,
                "sender": m.sender,
                "text": m.text,
                "timestamp": m.timestamp,
                "provider_used": m.provider_used,
                "model_used": m.model_used,
            }
            for m in messages
        ],
    }
