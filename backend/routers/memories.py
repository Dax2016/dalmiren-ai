from typing import Optional

from fastapi import APIRouter, Depends, HTTPException
from pydantic import BaseModel
from sqlalchemy.orm import Session

from database import get_db
from models.db_models import ChildProfile, User
from routers.auth import get_current_user
from services.memory_nexus import MemoryNexus

router = APIRouter(prefix="/api/memories", tags=["Memory Nexus"])


class MemoryCreate(BaseModel):
    child_id: Optional[str] = None
    memory_type: str = "SEMANTIC"
    category: str = "GENERAL"
    content: str


class MemoryUpdate(BaseModel):
    content: str


@router.get("")
def get_memories(
    child_id: Optional[str] = None,
    db: Session = Depends(get_db),
    current_user: User = Depends(get_current_user),
):
    if child_id:
        child = db.query(ChildProfile).filter(
            ChildProfile.id == child_id,
            ChildProfile.user_id == current_user.id
        ).first()

        if not child:
            raise HTTPException(status_code=404, detail="Child profile not found")

    nexus = MemoryNexus(db)
    memories = nexus.retrieve_active_memories(
        current_user.id,
        child_id=child_id
    )

    return [
        {
            "id": m.id,
            "user_id": m.user_id,
            "child_id": m.child_id,
            "memory_type": m.memory_type,
            "category": m.category,
            "content": m.content,
            "confidence": m.confidence,
            "created_at": m.created_at,
            "updated_at": m.updated_at
        }
        for m in memories
    ]


@router.post("")
def create_memory(
    req: MemoryCreate,
    db: Session = Depends(get_db),
    current_user: User = Depends(get_current_user),
):
    if req.child_id:
        child = db.query(ChildProfile).filter(
            ChildProfile.id == req.child_id,
            ChildProfile.user_id == current_user.id
        ).first()

        if not child:
            raise HTTPException(status_code=404, detail="Child profile not found")

    nexus = MemoryNexus(db)
    mem = nexus.create_memory(
        user_id=current_user.id,
        content=req.content,
        child_id=req.child_id,
        memory_type=req.memory_type,
        category=req.category
    )

    return {"id": mem.id, "content": mem.content, "status": "created"}


@router.put("/{memory_id}")
def update_memory(
    memory_id: str,
    req: MemoryUpdate,
    db: Session = Depends(get_db),
    current_user: User = Depends(get_current_user),
):
    nexus = MemoryNexus(db)
    mem = nexus.update_memory(
        memory_id,
        current_user.id,
        req.content
    )

    if not mem:
        raise HTTPException(status_code=404, detail="Memory not found")

    return {"id": mem.id, "content": mem.content, "status": "updated"}


@router.delete("/{memory_id}")
def delete_memory(
    memory_id: str,
    db: Session = Depends(get_db),
    current_user: User = Depends(get_current_user),
):
    nexus = MemoryNexus(db)
    success = nexus.delete_memory(memory_id, current_user.id)

    if not success:
        raise HTTPException(status_code=404, detail="Memory not found")

    return {"status": "deleted"}
