import hmac
import time

from fastapi import APIRouter, Depends, HTTPException
from sqlalchemy.orm import Session
from sqlalchemy.exc import IntegrityError
from pydantic import BaseModel, Field

from database import get_db
from models.db_models import User, UserEntitlement, UserSubscription
from routers.auth import get_current_user
from services.google_play import (
    GooglePlayVerificationError,
    hash_purchase_token,
    obfuscated_account_id,
    verify_subscription_purchase,
    acknowledge_subscription_purchase,
)

router = APIRouter(prefix="/api/subscriptions", tags=["Subscriptions"])

PREMIUM_PRODUCT_ID = "dalmiren_plus"
class PurchaseVerificationRequest(BaseModel):
    purchase_token: str = Field(min_length=20, max_length=4096)


def _get_or_create_entitlement(db: Session, user_id: str) -> UserEntitlement:
    entitlement = (
        db.query(UserEntitlement)
        .filter(UserEntitlement.user_id == user_id)
        .first()
    )
    if entitlement is None:
        entitlement = UserEntitlement(
            user_id=user_id,
            tier="FREE",
            status="ACTIVE",
            source="DEFAULT",
        )
        db.add(entitlement)
        db.commit()
        db.refresh(entitlement)
    return entitlement


def _effective_tier(entitlement: UserEntitlement) -> str:
    now = int(time.time() * 1000)
    if (
        entitlement.tier == "PREMIUM"
        and entitlement.status == "ACTIVE"
        and (entitlement.valid_until is None or entitlement.valid_until > now)
    ):
        return "PREMIUM"
    return "FREE"


@router.get("/catalog")
def get_subscription_catalog():
    return {
        "currency_source": "GOOGLE_PLAY_LOCALIZED_PRICING",
        "product_id": PREMIUM_PRODUCT_ID,
        "trial_days": 0,
        "plans": [
            {
                "base_plan_id": "plus-monthly",
                "billing_period": "P1M",
                "trial_available": False,
            },
        ],
        "note": "Prices and trial eligibility are supplied by Google Play Billing.",
    }


@router.get("/me")
@router.get("/entitlements")
def get_my_entitlements(
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db),
):
    entitlement = _get_or_create_entitlement(db, current_user.id)
    tier = _effective_tier(entitlement)

    active_subscription = (
        db.query(UserSubscription)
        .filter(
            UserSubscription.user_id == current_user.id,
            UserSubscription.status.in_(["ACTIVE", "IN_GRACE_PERIOD", "CANCELED"]),
        )
        .order_by(UserSubscription.updated_at.desc())
        .first()
    )

    try:
        account_binding_id = obfuscated_account_id(current_user.id)
    except GooglePlayVerificationError as exc:
        raise HTTPException(status_code=503, detail=str(exc)) from exc

    now = int(time.time() * 1000)
    if active_subscription and active_subscription.expires_at:
        if active_subscription.expires_at <= now:
            active_subscription = None

    return {
        "obfuscated_account_id": account_binding_id,
        "tier": tier,
        "status": entitlement.status if tier == "PREMIUM" else "ACTIVE",
        "source": entitlement.source,
        "valid_until": entitlement.valid_until if tier == "PREMIUM" else None,
        "subscription": (
            {
                "product_id": active_subscription.product_id,
                "base_plan_id": active_subscription.base_plan_id,
                "status": active_subscription.status,
                "expires_at": active_subscription.expires_at,
                "auto_renew_enabled": active_subscription.auto_renew_enabled,
            }
            if active_subscription
            else None
        ),
        "features": {
            "all_six_modules": True,
            "limited_usage": tier == "FREE",
            "extended_memory": tier == "PREMIUM",
            "advanced_progress_reports": tier == "PREMIUM",
            "multiple_child_profiles": tier == "PREMIUM",
        },
        "safety_support_available": True,
    }

@router.post("/verify")
@router.post("/restore")
def verify_purchase(
    payload: PurchaseVerificationRequest,
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db),
):
    token_hash = hash_purchase_token(payload.purchase_token)

    try:
        verified = verify_subscription_purchase(payload.purchase_token)
    except GooglePlayVerificationError as exc:
        raise HTTPException(status_code=422, detail=str(exc)) from exc

    try:
        expected_account_id = obfuscated_account_id(current_user.id)
    except GooglePlayVerificationError as exc:
        raise HTTPException(status_code=503, detail=str(exc)) from exc

    supplied_account_id = verified.get("external_account_id")
    if not supplied_account_id or not hmac.compare_digest(
        supplied_account_id,
        expected_account_id,
    ):
        raise HTTPException(
            status_code=409,
            detail=(
                "This Google Play purchase is not securely linked to "
                "the signed-in DALMIREN account."
            ),
        )

    existing = (
        db.query(UserSubscription)
        .filter(UserSubscription.purchase_token_hash == token_hash)
        .first()
    )
    if existing and existing.user_id != current_user.id:
        raise HTTPException(
            status_code=409,
            detail="This Google Play purchase is already linked to another account.",
        )

    now = int(time.time() * 1000)
    subscription = existing or UserSubscription(
        user_id=current_user.id,
        platform="GOOGLE_PLAY",
        purchase_token_hash=token_hash,
        created_at=now,
    )
    subscription.product_id = verified["product_id"]
    subscription.base_plan_id = verified["base_plan_id"]
    subscription.order_id = verified["order_id"]
    subscription.status = verified["status"]
    subscription.started_at = verified["started_at"]
    subscription.expires_at = verified["expires_at"]
    subscription.auto_renew_enabled = verified["auto_renew_enabled"]
    subscription.acknowledged = verified["acknowledged"]
    subscription.last_verified_at = now
    subscription.updated_at = now

    entitlement = (
        db.query(UserEntitlement)
        .filter(UserEntitlement.user_id == current_user.id)
        .first()
    )
    if entitlement is None:
        entitlement = UserEntitlement(user_id=current_user.id)
        db.add(entitlement)

    if verified["entitled"]:
        entitlement.tier = "PREMIUM"
        entitlement.status = "ACTIVE"
        entitlement.source = "GOOGLE_PLAY"
        entitlement.valid_until = verified["expires_at"]
    elif entitlement.source == "GOOGLE_PLAY":
        entitlement.tier = "FREE"
        entitlement.status = "ACTIVE"
        entitlement.source = "DEFAULT"
        entitlement.valid_until = None

    entitlement.updated_at = now
    db.add(subscription)

    try:
        db.commit()
    except IntegrityError as exc:
        db.rollback()
        raise HTTPException(
            status_code=409,
            detail="Purchase could not be linked. Please restore purchases or contact support.",
        ) from exc

    acknowledgement_error = None
    if verified["entitled"] and not verified["acknowledged"]:
        try:
            acknowledge_subscription_purchase(
                payload.purchase_token,
                verified["product_id"],
            )
            verified["acknowledged"] = True
            subscription.acknowledged = True
            db.add(subscription)
            db.commit()
        except GooglePlayVerificationError as exc:
            acknowledgement_error = str(exc)

    return {
        "verified": True,
        "tier": "PREMIUM" if verified["entitled"] else "FREE",
        "subscription_status": verified["status"],
        "product_id": verified["product_id"],
        "base_plan_id": verified["base_plan_id"],
        "expires_at": verified["expires_at"],
        "acknowledged": verified["acknowledged"],
        "acknowledgement_required": not verified["acknowledged"],
        "acknowledgement_error": acknowledgement_error,
    }
