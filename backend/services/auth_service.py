import hashlib
import secrets
import time
import uuid

import jwt
from pwdlib import PasswordHash
from sqlalchemy.orm import Session

from config import settings
from models.db_models import RefreshToken


# Uses Argon2id when the recommended pwdlib configuration is available.
_password_hash = PasswordHash.recommended()


class AuthenticationConfigurationError(RuntimeError):
    """Raised when authentication cannot operate securely."""


class InvalidRefreshTokenError(ValueError):
    """Raised when a refresh token is invalid, expired, or revoked."""


def _jwt_secret() -> str:
    secret = settings.AUTH_JWT_SECRET

    if not secret or len(secret.encode("utf-8")) < 32:
        raise AuthenticationConfigurationError(
            "AUTH_JWT_SECRET must be configured with at least 32 bytes."
        )

    return secret


def hash_password(password: str) -> str:
    if not password or len(password) < 12:
        raise ValueError("Password must contain at least 12 characters.")

    if len(password) > 1024:
        raise ValueError("Password is too long.")

    return _password_hash.hash(password)


def verify_password(password: str, password_hash: str | None) -> bool:
    if not password or not password_hash:
        return False

    try:
        return _password_hash.verify(password, password_hash)
    except Exception:
        return False


def create_access_token(user_id: str) -> str:
    now = int(time.time())

    payload = {
        "sub": user_id,
        "iss": settings.AUTH_JWT_ISSUER,
        "aud": settings.AUTH_JWT_AUDIENCE,
        "iat": now,
        "exp": now + settings.AUTH_ACCESS_TOKEN_MINUTES * 60,
        "jti": str(uuid.uuid4()),
    }

    return jwt.encode(payload, _jwt_secret(), algorithm="HS256")


def decode_access_token(token: str) -> dict:
    try:
        payload = jwt.decode(
            token,
            _jwt_secret(),
            algorithms=["HS256"],
            issuer=settings.AUTH_JWT_ISSUER,
            audience=settings.AUTH_JWT_AUDIENCE,
            options={
                "require": ["sub", "iss", "aud", "iat", "exp", "jti"],
            },
        )

        if not isinstance(payload.get("sub"), str) or not payload["sub"]:
            raise ValueError("Invalid token subject.")

        return payload

    except jwt.InvalidTokenError as exc:
        raise ValueError("Invalid or expired access token.") from exc


def _hash_refresh_token(raw_token: str) -> str:
    return hashlib.sha256(raw_token.encode("utf-8")).hexdigest()


def create_refresh_token(db: Session, user_id: str) -> str:
    raw_token = secrets.token_urlsafe(64)
    token_hash = _hash_refresh_token(raw_token)

    now_ms = int(time.time() * 1000)
    expires_at = now_ms + settings.AUTH_REFRESH_TOKEN_DAYS * 24 * 60 * 60 * 1000

    record = RefreshToken(
        user_id=user_id,
        token_hash=token_hash,
        expires_at=expires_at,
    )

    db.add(record)
    db.flush()

    return raw_token


def rotate_refresh_token(db: Session, raw_token: str) -> tuple[str, str]:
    if not raw_token:
        raise InvalidRefreshTokenError("Refresh token is required.")

    token_hash = _hash_refresh_token(raw_token)

    record = (
        db.query(RefreshToken)
        .filter(RefreshToken.token_hash == token_hash)
        .with_for_update()
        .first()
    )

    if record is None:
        raise InvalidRefreshTokenError("Invalid refresh token.")

    if record.revoked_at is not None:
        # Reuse of an already-rotated token revokes the user's active sessions.
        now_ms = int(time.time() * 1000)
        (
            db.query(RefreshToken)
            .filter(
                RefreshToken.user_id == record.user_id,
                RefreshToken.revoked_at.is_(None),
            )
            .update(
                {RefreshToken.revoked_at: now_ms},
                synchronize_session=False,
            )
        )
        db.flush()
        raise InvalidRefreshTokenError("Refresh token reuse detected.")

    now_ms = int(time.time() * 1000)

    if record.expires_at <= now_ms:
        record.revoked_at = now_ms
        db.flush()
        raise InvalidRefreshTokenError("Refresh token has expired.")

    new_raw_token = secrets.token_urlsafe(64)
    new_hash = _hash_refresh_token(new_raw_token)

    new_record = RefreshToken(
        user_id=record.user_id,
        token_hash=new_hash,
        expires_at=now_ms + settings.AUTH_REFRESH_TOKEN_DAYS * 24 * 60 * 60 * 1000,
    )

    record.revoked_at = now_ms
    record.replaced_by_hash = new_hash

    db.add(new_record)
    db.flush()

    access_token = create_access_token(record.user_id)

    return access_token, new_raw_token


def revoke_refresh_token(
    db: Session,
    raw_token: str,
    expected_user_id: str,
) -> bool:
    if not raw_token or not expected_user_id:
        return False

    token_hash = _hash_refresh_token(raw_token)

    record = (
        db.query(RefreshToken)
        .filter(
            RefreshToken.token_hash == token_hash,
            RefreshToken.user_id == expected_user_id,
        )
        .first()
    )

    if record is None or record.revoked_at is not None:
        return False

    record.revoked_at = int(time.time() * 1000)
    db.flush()

    return True
