from typing import Optional, List
from fastapi import APIRouter, Depends, HTTPException
from pydantic import BaseModel
from sqlalchemy.orm import Session
from database import get_db
from models.db_models import ChildProfile, User
from routers.auth import get_current_user

router = APIRouter(prefix="/api/profiles", tags=["Profiles"])

class ChildProfileCreate(BaseModel):
    name: str
    age_group: str = "EARLY_CHILDHOOD"
    birth_date: Optional[str] = None
    notes: Optional[str] = None

@router.post("/children")
def create_child_profile(
    req: ChildProfileCreate,
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db)
):
    child = ChildProfile(
        user_id=current_user.id,
        name=req.name,
        age_group=req.age_group,
        birth_date=req.birth_date,
        notes=req.notes
    )
    db.add(child)
    db.commit()
    db.refresh(child)
    return {"id": child.id, "name": child.name, "age_group": child.age_group}

@router.get("/children")
def get_child_profiles(
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db)
):
    children = db.query(ChildProfile).filter(
        ChildProfile.user_id == current_user.id
    ).all()
    return [
        {"id": c.id, "name": c.name, "age_group": c.age_group, "notes": c.notes}
        for c in children
    ]
