import hashlib
import hmac
import time
from datetime import datetime
from urllib.parse import quote

import google.auth
from google.auth.transport.requests import AuthorizedSession

from config import settings


GOOGLE_PLAY_SCOPE = "https://www.googleapis.com/auth/androidpublisher"
GOOGLE_PLAY_API = "https://androidpublisher.googleapis.com/androidpublisher/v3"

SUPPORTED_PRODUCT_ID = "dalmiren_plus"
SUPPORTED_BASE_PLANS = {"plus-monthly"}


class GooglePlayVerificationError(Exception):
    pass


def hash_purchase_token(token: str) -> str:
    return hashlib.sha256(token.encode("utf-8")).hexdigest()


def obfuscated_account_id(user_id: str) -> str:
    secret = settings.GOOGLE_PLAY_ACCOUNT_BINDING_SECRET.strip()
    if not secret:
        raise GooglePlayVerificationError(
            "Google Play account binding is not configured."
        )
    return hmac.new(
        secret.encode("utf-8"),
        str(user_id).encode("utf-8"),
        hashlib.sha256,
    ).hexdigest()


def _authorized_session() -> AuthorizedSession:
    credential_file = settings.GOOGLE_PLAY_SERVICE_ACCOUNT_FILE.strip()
    if not credential_file:
        raise GooglePlayVerificationError(
            "Google Play service-account credentials are not configured."
        )

    try:
        credentials, _ = google.auth.load_credentials_from_file(
            credential_file,
            scopes=[GOOGLE_PLAY_SCOPE],
        )
        return AuthorizedSession(credentials)
    except Exception as exc:
        raise GooglePlayVerificationError(
            "Could not authenticate with Google Play."
        ) from exc


def _timestamp_ms(value):
    if not value:
        return None
    parsed = datetime.fromisoformat(value.replace("Z", "+00:00"))
    return int(parsed.timestamp() * 1000)


def _subscription_url(product_id: str, purchase_token: str) -> str:
    package_name = quote(settings.GOOGLE_PLAY_PACKAGE_NAME, safe="")
    product = quote(product_id, safe="")
    token = quote(purchase_token, safe="")
    return (
        f"{GOOGLE_PLAY_API}/applications/{package_name}/"
        f"purchases/subscriptions/{product}/tokens/{token}"
    )


def verify_subscription_purchase(purchase_token: str) -> dict:
    session = _authorized_session()

    try:
        token_path = quote(purchase_token, safe="")
        package_name = quote(settings.GOOGLE_PLAY_PACKAGE_NAME, safe="")
        url = (
            f"{GOOGLE_PLAY_API}/applications/{package_name}/"
            f"purchases/subscriptionsv2/tokens/{token_path}"
        )
        response = session.get(url, timeout=20)
    except Exception as exc:
        raise GooglePlayVerificationError(
            "Could not contact Google Play to verify the purchase."
        ) from exc

    if response.status_code != 200:
        raise GooglePlayVerificationError(
            f"Google Play verification failed (HTTP {response.status_code})."
        )

    try:
        payload = response.json()
        state = payload.get("subscriptionState", "")
        line_items = payload.get("lineItems", [])

        matching_item = next(
            (
                item for item in line_items
                if item.get("productId") == SUPPORTED_PRODUCT_ID
                and item.get("offerDetails", {}).get("basePlanId")
                in SUPPORTED_BASE_PLANS
            ),
            None,
        )

        if matching_item is None:
            raise GooglePlayVerificationError(
                "Purchase does not match a supported DALMIREN Premium plan."
            )

        expiry_ms = _timestamp_ms(matching_item.get("expiryTime"))
        now_ms = int(time.time() * 1000)

        normalized_states = {
            "SUBSCRIPTION_STATE_ACTIVE": "ACTIVE",
            "SUBSCRIPTION_STATE_IN_GRACE_PERIOD": "IN_GRACE_PERIOD",
            "SUBSCRIPTION_STATE_CANCELED": "CANCELED",
            "SUBSCRIPTION_STATE_ON_HOLD": "ON_HOLD",
            "SUBSCRIPTION_STATE_PAUSED": "PAUSED",
            "SUBSCRIPTION_STATE_EXPIRED": "EXPIRED",
            "SUBSCRIPTION_STATE_PENDING": "PENDING",
        }
        normalized_state = normalized_states.get(state, "UNKNOWN")

        entitled = (
            normalized_state in {"ACTIVE", "IN_GRACE_PERIOD", "CANCELED"}
            and expiry_ms is not None
            and expiry_ms > now_ms
        )

        acknowledgement = payload.get("acknowledgementState", "")
        external_ids = payload.get("externalAccountIdentifiers") or {}
        renewing_plan = matching_item.get("autoRenewingPlan") or {}

        return {
            "product_id": matching_item["productId"],
            "base_plan_id": matching_item["offerDetails"]["basePlanId"],
            "status": normalized_state,
            "expires_at": expiry_ms,
            "started_at": _timestamp_ms(payload.get("startTime")),
            "order_id": payload.get("latestOrderId"),
            "acknowledged": (
                acknowledgement == "ACKNOWLEDGEMENT_STATE_ACKNOWLEDGED"
            ),
            "auto_renew_enabled": bool(
                renewing_plan.get("autoRenewEnabled", False)
            ),
            "entitled": entitled,
            "external_account_id": external_ids.get(
                "obfuscatedExternalAccountId"
            ),
        }

    except GooglePlayVerificationError:
        raise
    except Exception as exc:
        raise GooglePlayVerificationError(
            "Google Play returned an invalid subscription response."
        ) from exc


def acknowledge_subscription_purchase(
    purchase_token: str,
    product_id: str,
) -> bool:
    if product_id != SUPPORTED_PRODUCT_ID:
        raise GooglePlayVerificationError(
            "Cannot acknowledge an unsupported DALMIREN product."
        )

    session = _authorized_session()
    url = _subscription_url(product_id, purchase_token)

    try:
        response = session.post(
            f"{url}:acknowledge",
            json={},
            timeout=20,
        )
    except Exception as exc:
        raise GooglePlayVerificationError(
            "Could not contact Google Play to acknowledge the purchase."
        ) from exc

    if response.status_code not in (200, 204):
        raise GooglePlayVerificationError(
            f"Google Play purchase acknowledgment failed "
            f"(HTTP {response.status_code})."
        )

    return True
