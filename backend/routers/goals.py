from typing import Optional

from fastapi import APIRouter, Depends, HTTPException
from pydantic import BaseModel
from sqlalchemy.orm import Session

from database import get_db
from models.db_models import ChildProfile, FollowUpRecord, ParentingGoal, User
from routers.auth import get_current_user

router = APIRouter(prefix="/api/goals", tags=["Parenting Goals & Follow-ups"])


class GoalCreate(BaseModel):
    child_id: Optional[str] = None
    title: str
    description: Optional[str] = None
    status: str = "ACTIVE"


class FollowUpCreate(BaseModel):
    child_id: Optional[str] = None
    goal_id: Optional[str] = None
    notes: str
    outcome: Optional[str] = "IN_PROGRESS"


def validate_child_ownership(db: Session, child_id: Optional[str], user_id: str) -> None:
    if not child_id:
        return

    child = db.query(ChildProfile).filter(
        ChildProfile.id == child_id,
        ChildProfile.user_id == user_id
    ).first()

    if not child:
        raise HTTPException(status_code=404, detail="Child profile not found")


@router.post("")
def create_goal(
    req: GoalCreate,
    db: Session = Depends(get_db),
    current_user: User = Depends(get_current_user),
):
    validate_child_ownership(db, req.child_id, current_user.id)

    goal = ParentingGoal(
        user_id=current_user.id,
        child_id=req.child_id,
        title=req.title,
        description=req.description,
        status=req.status
    )
    db.add(goal)
    db.commit()
    db.refresh(goal)

    return {"id": goal.id, "title": goal.title, "status": goal.status}


@router.get("")
def get_user_goals(
    child_id: Optional[str] = None,
    db: Session = Depends(get_db),
    current_user: User = Depends(get_current_user),
):
    validate_child_ownership(db, child_id, current_user.id)

    query = db.query(ParentingGoal).filter(
        ParentingGoal.user_id == current_user.id
    )
    if child_id:
        query = query.filter(ParentingGoal.child_id == child_id)

    goals = query.order_by(ParentingGoal.created_at.desc()).all()

    return [
        {
            "id": g.id,
            "title": g.title,
            "description": g.description,
            "status": g.status,
            "created_at": g.created_at
        }
        for g in goals
    ]


@router.post("/followup")
def record_follow_up(
    req: FollowUpCreate,
    db: Session = Depends(get_db),
    current_user: User = Depends(get_current_user),
):
    validate_child_ownership(db, req.child_id, current_user.id)

    if req.goal_id:
        goal = db.query(ParentingGoal).filter(
            ParentingGoal.id == req.goal_id,
            ParentingGoal.user_id == current_user.id
        ).first()

        if not goal:
            raise HTTPException(status_code=404, detail="Goal not found")

        if req.child_id and goal.child_id and req.child_id != goal.child_id:
            raise HTTPException(
                status_code=404,
                detail="Goal not found for this child"
            )

    record = FollowUpRecord(
        user_id=current_user.id,
        child_id=req.child_id,
        goal_id=req.goal_id,
        notes=req.notes,
        outcome=req.outcome
    )
    db.add(record)
    db.commit()
    db.refresh(record)

    return {"id": record.id, "outcome": record.outcome, "status": "recorded"}


@router.get("/followup")
def get_follow_ups(
    goal_id: Optional[str] = None,
    db: Session = Depends(get_db),
    current_user: User = Depends(get_current_user),
):
    query = db.query(FollowUpRecord).filter(
        FollowUpRecord.user_id == current_user.id
    )

    if goal_id:
        goal = db.query(ParentingGoal).filter(
            ParentingGoal.id == goal_id,
            ParentingGoal.user_id == current_user.id
        ).first()

        if not goal:
            raise HTTPException(status_code=404, detail="Goal not found")

        query = query.filter(FollowUpRecord.goal_id == goal_id)

    records = query.order_by(FollowUpRecord.timestamp.desc()).all()

    return [
        {
            "id": r.id,
            "goal_id": r.goal_id,
            "notes": r.notes,
            "outcome": r.outcome,
            "timestamp": r.timestamp
        }
        for r in records
    ]
