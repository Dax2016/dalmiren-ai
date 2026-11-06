import re
from typing import Optional

from fastapi import APIRouter, Depends, HTTPException, status
from fastapi.security import HTTPAuthorizationCredentials, HTTPBearer
from pydantic import BaseModel, Field
from sqlalchemy.exc import IntegrityError
from sqlalchemy.orm import Session

from database import get_db
from models.db_models import User
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


@router.get("/me")
def get_me(current_user: User = Depends(get_current_user)):
    return {
        "id": current_user.id,
        "email": current_user.email,
        "full_name": current_user.full_name,
    }
