import re
from typing import Optional

from fastapi import APIRouter, Depends, HTTPException, status
from fastapi.security import HTTPAuthorizationCredentials, HTTPBearer
from pydantic import BaseModel, Field
from sqlalchemy.exc import IntegrityError
from sqlalchemy.orm import Session

from database import get_db
from models.db_models import (
    User, FamilyProfile, ChildProfile, Conversation, Message, Memory,
    ParentingGoal, GuidanceSession, FollowUpRecord, MemoryAuditEvent,
    ConsentRecord, RefreshToken, UserSubscription, UserEntitlement,
    PlayPurchaseEvent, ParentingPlan,
)
from services.auth_service import (
    InvalidRefreshTokenError,
    create_access_token,
    create_refresh_token,
    decode_access_token,
    hash_password,
    rotate_refresh_token,
    revoke_refresh_token,
    verify_password,
)

router = APIRouter(prefix="/api/auth", tags=["Authentication"])
bearer_scheme = HTTPBearer(auto_error=False)


class RegisterRequest(BaseModel):
    email: str = Field(min_length=5, max_length=255)
    password: str = Field(min_length=12, max_length=1024)
    full_name: Optional[str] = Field(default=None, max_length=255)


class LoginRequest(BaseModel):
    email: str = Field(min_length=1, max_length=255)
    password: str = Field(min_length=1, max_length=1024)


class RefreshRequest(BaseModel):
    refresh_token: str = Field(min_length=20, max_length=512)


class LogoutRequest(BaseModel):
    refresh_token: str = Field(min_length=20, max_length=512)


class DeleteAccountRequest(BaseModel):
    password: str = Field(min_length=1, max_length=1024)
    confirmation: str = Field(min_length=1, max_length=50)


def _normalize_email(email: str) -> str:
    normalized = email.strip().lower()

    if not re.fullmatch(r"[^@\s]+@[^@\s]+\.[^@\s]+", normalized):
        raise HTTPException(
            status_code=status.HTTP_422_UNPROCESSABLE_ENTITY,
            detail="A valid email address is required.",
        )

    return normalized


def _token_response(db: Session, user: User) -> dict:
    access_token = create_access_token(user.id)
    refresh_token = create_refresh_token(db, user.id)

    return {
        "token_type": "bearer",
        "access_token": access_token,
        "refresh_token": refresh_token,
        "expires_in": 900,
        "user": {
            "id": user.id,
            "email": user.email,
            "full_name": user.full_name,
        },
    }


@router.post("/register", status_code=status.HTTP_201_CREATED)
def register(payload: RegisterRequest, db: Session = Depends(get_db)):
    email = _normalize_email(payload.email)

    existing = db.query(User).filter(User.email == email).first()
    if existing:
        raise HTTPException(
            status_code=status.HTTP_409_CONFLICT,
            detail="An account with this email address already exists.",
        )

    user = User(
        email=email,
        password_hash=hash_password(payload.password),
        full_name=payload.full_name.strip() if payload.full_name else None,
    )

    try:
        db.add(user)
        db.flush()
        response = _token_response(db, user)
        db.commit()
        return response
    except IntegrityError:
        db.rollback()
        raise HTTPException(
            status_code=status.HTTP_409_CONFLICT,
            detail="An account with this email address already exists.",
        )
    except Exception:
        db.rollback()
        raise


@router.post("/login")
def login(payload: LoginRequest, db: Session = Depends(get_db)):
    email = _normalize_email(payload.email)

    user = db.query(User).filter(User.email == email).first()

    if not user or not verify_password(payload.password, user.password_hash):
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Invalid email or password.",
            headers={"WWW-Authenticate": "Bearer"},
        )

    try:
        response = _token_response(db, user)
        db.commit()
        return response
    except Exception:
        db.rollback()
        raise


@router.post("/refresh")
def refresh(payload: RefreshRequest, db: Session = Depends(get_db)):
    try:
        access_token, new_refresh_token = rotate_refresh_token(
            db, payload.refresh_token
        )

        db.commit()

        decoded = decode_access_token(access_token)
        user = db.query(User).filter(User.id == decoded["sub"]).first()

        if not user:
            raise HTTPException(
                status_code=status.HTTP_401_UNAUTHORIZED,
                detail="Authentication required.",
            )

        return {
            "token_type": "bearer",
            "access_token": access_token,
            "refresh_token": new_refresh_token,
            "expires_in": 900,
        }

    except InvalidRefreshTokenError as exc:
        # Commit revocations caused by expired-token handling or reuse detection.
        db.commit()
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Refresh token is invalid or expired. Please sign in again.",
        ) from exc
    except ValueError as exc:
        db.rollback()
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Refresh token is invalid or expired. Please sign in again.",
        ) from exc
    except Exception:
        db.rollback()
        raise


def get_current_user(
    credentials: Optional[HTTPAuthorizationCredentials] = Depends(bearer_scheme),
    db: Session = Depends(get_db),
) -> User:
    if credentials is None:
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Authentication required.",
            headers={"WWW-Authenticate": "Bearer"},
        )

    try:
        payload = decode_access_token(credentials.credentials)
    except ValueError as exc:
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Invalid or expired access token.",
            headers={"WWW-Authenticate": "Bearer"},
        ) from exc

    user = db.query(User).filter(User.id == payload["sub"]).first()

    if user is None:
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Authentication required.",
            headers={"WWW-Authenticate": "Bearer"},
        )

    return user


@router.post("/logout")
def logout(
    payload: LogoutRequest,
    db: Session = Depends(get_db),
    current_user: User = Depends(get_current_user),
):
    try:
        revoke_refresh_token(
            db,
            payload.refresh_token,
            current_user.id,
        )
        db.commit()
        return {"message": "Signed out successfully."}
    except Exception:
        db.rollback()
        raise


@router.post("/delete-account")
def delete_account(
    payload: DeleteAccountRequest,
    db: Session = Depends(get_db),
    current_user: User = Depends(get_current_user),
):
    if payload.confirmation != "DELETE MY ACCOUNT":
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail='Type "DELETE MY ACCOUNT" exactly to confirm account deletion.',
        )

    if not current_user.password_hash:
        raise HTTPException(
            status_code=status.HTTP_409_CONFLICT,
            detail="This account has no password credential. Contact support to complete verified account deletion.",
        )

    if not verify_password(payload.password, current_user.password_hash):
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="The password is incorrect.",
            headers={"WWW-Authenticate": "Bearer"},
        )

    user_id = current_user.id

    try:
        conversation_ids = [
            row[0] for row in db.query(Conversation.id).filter(
                Conversation.user_id == user_id
            ).all()
        ]
        subscription_token_hashes = [
            row[0] for row in db.query(UserSubscription.purchase_token_hash).filter(
                UserSubscription.user_id == user_id
            ).all()
        ]

        # Remove conversation content before its owning conversations.
        if conversation_ids:
            db.query(Message).filter(
                Message.conversation_id.in_(conversation_ids)
            ).delete(synchronize_session=False)

        db.query(Conversation).filter(
            Conversation.user_id == user_id
        ).delete(synchronize_session=False)

        # Remove dependent progress records before parenting goals.
        db.query(FollowUpRecord).filter(
            FollowUpRecord.user_id == user_id
        ).delete(synchronize_session=False)

        db.query(ParentingGoal).filter(
            ParentingGoal.user_id == user_id
        ).delete(synchronize_session=False)

        # Remove all remaining parent- and child-linked personal records.
        db.query(ParentingPlan).filter(
            ParentingPlan.user_id == user_id
        ).delete(synchronize_session=False)

        db.query(GuidanceSession).filter(
            GuidanceSession.user_id == user_id
        ).delete(synchronize_session=False)

        db.query(MemoryAuditEvent).filter(
            MemoryAuditEvent.user_id == user_id
        ).delete(synchronize_session=False)

        db.query(Memory).filter(
            Memory.user_id == user_id
        ).delete(synchronize_session=False)

        db.query(ConsentRecord).filter(
            ConsentRecord.user_id == user_id
        ).delete(synchronize_session=False)

        db.query(FamilyProfile).filter(
            FamilyProfile.parent_id == user_id
        ).delete(synchronize_session=False)

        # Remove the account's subscription association and entitlement.
        # Google Play billing itself is not cancelled by deleting this account.
        if subscription_token_hashes:
            db.query(PlayPurchaseEvent).filter(
                PlayPurchaseEvent.purchase_token_hash.in_(subscription_token_hashes)
            ).update(
                {
                    PlayPurchaseEvent.purchase_token_hash: None,
                },
                synchronize_session=False,
            )

        db.query(UserSubscription).filter(
            UserSubscription.user_id == user_id
        ).delete(synchronize_session=False)

        db.query(UserEntitlement).filter(
            UserEntitlement.user_id == user_id
        ).delete(synchronize_session=False)

        db.query(RefreshToken).filter(
            RefreshToken.user_id == user_id
        ).delete(synchronize_session=False)

        # Child profiles are deleted only after every child-linked record.
        db.query(ChildProfile).filter(
            ChildProfile.user_id == user_id
        ).delete(synchronize_session=False)

        db.query(User).filter(
            User.id == user_id
        ).delete(synchronize_session=False)

        db.commit()

        return {
            "message": "The account and associated personal data have been deleted.",
            "subscription_notice": (
                "Any Google Play subscription must be cancelled separately "
                "in Google Play."
            ),
        }
    except Exception:
        db.rollback()
        raise


@router.get("/me")
def get_me(current_user: User = Depends(get_current_user)):
    return {
        "id": current_user.id,
        "email": current_user.email,
        "full_name": current_user.full_name,
    }
