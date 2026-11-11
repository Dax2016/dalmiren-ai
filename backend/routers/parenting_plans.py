import time
import uuid

from typing import Any, Literal, Optional

from fastapi import APIRouter, Depends, HTTPException
from pydantic import BaseModel, Field
from sqlalchemy.orm import Session

from database import get_db
from models.db_models import ChildProfile, ParentingPlan, User
from routers.auth import get_current_user

router = APIRouter(
    prefix="/api/profiles/parenting-plans",
    tags=["Saved Parenting Plans"],
)

PlanStatus = Literal["DRAFT", "ACTIVE", "PAUSED", "COMPLETED"]


class ParentingPlanCreate(BaseModel):
    child_id: Optional[str] = None
    title: str = Field(min_length=1, max_length=255)
    summary: Optional[str] = None
    status: PlanStatus = "DRAFT"
    plan_data: dict[str, Any]


class ParentingPlanUpdate(BaseModel):
    child_id: Optional[str] = None
    title: Optional[str] = Field(default=None, min_length=1, max_length=255)
    summary: Optional[str] = None
    status: Optional[PlanStatus] = None
    plan_data: Optional[dict[str, Any]] = None


class ProgressEntryCreate(BaseModel):
    notes: str = Field(min_length=1, max_length=10000)
    outcome: Optional[str] = Field(default=None, max_length=1000)


def validate_child_ownership(
    db: Session, child_id: Optional[str], user_id: str
) -> None:
    if child_id is None:
        return

    child = db.query(ChildProfile).filter(
        ChildProfile.id == child_id,
        ChildProfile.user_id == user_id,
    ).first()

    if not child:
        raise HTTPException(status_code=404, detail="Child profile not found")


def get_owned_plan(
    db: Session, plan_id: str, user_id: str
) -> ParentingPlan:
    plan = db.query(ParentingPlan).filter(
        ParentingPlan.id == plan_id,
        ParentingPlan.user_id == user_id,
    ).first()

    if not plan:
        raise HTTPException(status_code=404, detail="Parenting plan not found")

    return plan


def serialize_plan(plan: ParentingPlan) -> dict:
    return {
        "id": plan.id,
        "child_id": plan.child_id,
        "title": plan.title,
        "summary": plan.summary,
        "status": plan.status,
        "plan_data": plan.plan_data,
        "progress_entries": plan.progress_entries or [],
        "created_at": plan.created_at,
        "updated_at": plan.updated_at,
    }


@router.post("")
def create_parenting_plan(
    req: ParentingPlanCreate,
    db: Session = Depends(get_db),
    current_user: User = Depends(get_current_user),
):
    validate_child_ownership(db, req.child_id, current_user.id)

    title = req.title.strip()
    if not title:
        raise HTTPException(status_code=422, detail="Plan title cannot be blank")

    plan = ParentingPlan(
        user_id=current_user.id,
        child_id=req.child_id,
        title=title,
        summary=req.summary,
        status=req.status,
        plan_data=req.plan_data,
        progress_entries=[],
    )
    db.add(plan)
    db.commit()
    db.refresh(plan)

    return serialize_plan(plan)


@router.get("")
def list_parenting_plans(
    child_id: Optional[str] = None,
    status: Optional[PlanStatus] = None,
    db: Session = Depends(get_db),
    current_user: User = Depends(get_current_user),
):
    validate_child_ownership(db, child_id, current_user.id)

    query = db.query(ParentingPlan).filter(
        ParentingPlan.user_id == current_user.id
    )

    if child_id is not None:
        query = query.filter(ParentingPlan.child_id == child_id)

    if status is not None:
        query = query.filter(ParentingPlan.status == status)

    plans = query.order_by(ParentingPlan.updated_at.desc()).all()
    return [serialize_plan(plan) for plan in plans]


@router.get("/{plan_id}")
def get_parenting_plan(
    plan_id: str,
    db: Session = Depends(get_db),
    current_user: User = Depends(get_current_user),
):
    plan = get_owned_plan(db, plan_id, current_user.id)
    return serialize_plan(plan)


@router.put("/{plan_id}")
def update_parenting_plan(
    plan_id: str,
    req: ParentingPlanUpdate,
    db: Session = Depends(get_db),
    current_user: User = Depends(get_current_user),
):
    plan = get_owned_plan(db, plan_id, current_user.id)
    changes = req.model_dump(exclude_unset=True)

    if "child_id" in changes:
        validate_child_ownership(db, changes["child_id"], current_user.id)

    if "title" in changes:
        title = changes["title"].strip() if changes["title"] is not None else ""
        if not title:
            raise HTTPException(status_code=422, detail="Plan title cannot be blank")
        changes["title"] = title

    for field, value in changes.items():
        setattr(plan, field, value)

    db.commit()
    db.refresh(plan)
    return serialize_plan(plan)


@router.post("/{plan_id}/progress")
def add_progress_entry(
    plan_id: str,
    req: ProgressEntryCreate,
    db: Session = Depends(get_db),
    current_user: User = Depends(get_current_user),
):
    plan = get_owned_plan(db, plan_id, current_user.id)

    notes = req.notes.strip()
    if not notes:
        raise HTTPException(status_code=422, detail="Progress notes cannot be blank")

    entries = list(plan.progress_entries or [])
    entries.append({
        "id": uuid.uuid4().hex,
        "notes": notes,
        "outcome": req.outcome,
        "timestamp": int(time.time() * 1000),
    })

    plan.progress_entries = entries
    db.commit()
    db.refresh(plan)
    return serialize_plan(plan)


@router.delete("/{plan_id}")
def delete_parenting_plan(
    plan_id: str,
    db: Session = Depends(get_db),
    current_user: User = Depends(get_current_user),
):
    plan = get_owned_plan(db, plan_id, current_user.id)
    db.delete(plan)
    db.commit()
    return {"status": "deleted", "id": plan_id}
