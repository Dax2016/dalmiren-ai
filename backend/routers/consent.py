from fastapi import APIRouter, Depends, HTTPException
from pydantic import BaseModel, ConfigDict
from sqlalchemy.orm import Session

from database import get_db
from models.db_models import ConsentRecord, User
from routers.auth import get_current_user

router = APIRouter(prefix="/api/consent", tags=["Privacy & Consent"])

ALLOWED_CONSENT_TYPES = {"CLOUD_PROCESSING"}


class ConsentRequest(BaseModel):
    model_config = ConfigDict(extra="forbid")

    consent_type: str = "CLOUD_PROCESSING"
    granted: bool


@router.post("")
def update_consent(
    req: ConsentRequest,
    db: Session = Depends(get_db),
    current_user: User = Depends(get_current_user),
):
    if req.consent_type not in ALLOWED_CONSENT_TYPES:
        raise HTTPException(status_code=400, detail="Unsupported consent type.")

    consent = ConsentRecord(
        user_id=current_user.id,
        consent_type=req.consent_type,
        granted=req.granted,
    )
    db.add(consent)
    db.commit()

    return {
        "user_id": current_user.id,
        "consent_type": req.consent_type,
        "granted": req.granted,
        "status": "updated",
    }


@router.get("")
def get_consent(
    consent_type: str = "CLOUD_PROCESSING",
    db: Session = Depends(get_db),
    current_user: User = Depends(get_current_user),
):
    if consent_type not in ALLOWED_CONSENT_TYPES:
        raise HTTPException(status_code=400, detail="Unsupported consent type.")

    consent = (
        db.query(ConsentRecord)
        .filter(
            ConsentRecord.user_id == current_user.id,
            ConsentRecord.consent_type == consent_type,
        )
        .order_by(ConsentRecord.timestamp.desc())
        .first()
    )

    # Missing consent must never authorise cloud processing.
    granted = bool(consent.granted) if consent else False

    return {
        "user_id": current_user.id,
        "consent_type": consent_type,
        "granted": granted,
    }
