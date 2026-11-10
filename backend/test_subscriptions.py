import time
import uuid
import unittest
from unittest.mock import patch

from fastapi import FastAPI
from fastapi.testclient import TestClient

from database import SessionLocal, get_db
from models.db_models import User, UserEntitlement, UserSubscription
from routers.auth import get_current_user
from routers import subscriptions


class SubscriptionIntegrationTests(unittest.TestCase):
    def setUp(self):
        self.db = SessionLocal()
        suffix = uuid.uuid4().hex

        self.parent = User(
            email=f"subscription-owner-{suffix}@example.test",
            full_name="Subscription Owner",
        )
        self.other_parent = User(
            email=f"subscription-other-{suffix}@example.test",
            full_name="Other Subscription Owner",
        )
        self.db.add_all([self.parent, self.other_parent])
        self.db.commit()
        self.db.refresh(self.parent)
        self.db.refresh(self.other_parent)

        self.app = FastAPI()
        self.app.include_router(subscriptions.router)
        self.app.dependency_overrides[get_db] = lambda: self.db
        self.app.dependency_overrides[get_current_user] = lambda: self.parent
        self.client = TestClient(self.app)

    def tearDown(self):
        try:
            user_ids = [self.parent.id, self.other_parent.id]
            self.db.query(UserSubscription).filter(
                UserSubscription.user_id.in_(user_ids)
            ).delete(synchronize_session=False)
            self.db.query(UserEntitlement).filter(
                UserEntitlement.user_id.in_(user_ids)
            ).delete(synchronize_session=False)
            self.db.query(User).filter(
                User.id.in_(user_ids)
            ).delete(synchronize_session=False)
            self.db.commit()
        finally:
            self.client.close()
            self.db.close()

    def test_catalog_exposes_monthly_annual_and_seven_day_trial(self):
        response = self.client.get("/api/subscriptions/catalog")

        self.assertEqual(response.status_code, 200)
        data = response.json()
        self.assertEqual(data["product_id"], "dalmiren_premium")
        self.assertEqual(data["trial_days"], 7)
        self.assertEqual(
            [plan["base_plan_id"] for plan in data["plans"]],
            ["premium_monthly", "premium_annual"],
        )

    def test_free_account_gets_all_six_modules_and_safety_support(self):
        response = self.client.get("/api/subscriptions/me")

        self.assertEqual(response.status_code, 200)
        data = response.json()
        self.assertEqual(data["tier"], "FREE")
        self.assertTrue(data["features"]["all_six_modules"])
        self.assertTrue(data["features"]["limited_usage"])
        self.assertFalse(data["features"]["extended_memory"])
        self.assertFalse(data["features"]["advanced_progress_reports"])
        self.assertFalse(data["features"]["multiple_child_profiles"])
        self.assertTrue(data["safety_support_available"])

    def test_expired_premium_entitlement_does_not_unlock_premium(self):
        self.db.add(UserEntitlement(
            user_id=self.parent.id,
            tier="PREMIUM",
            status="ACTIVE",
            source="GOOGLE_PLAY",
            valid_until=int(time.time() * 1000) - 60_000,
        ))
        self.db.commit()

        response = self.client.get("/api/subscriptions/me")

        self.assertEqual(response.status_code, 200)
        data = response.json()
        self.assertEqual(data["tier"], "FREE")
        self.assertTrue(data["features"]["limited_usage"])
        self.assertFalse(data["features"]["extended_memory"])
        self.assertTrue(data["safety_support_available"])

    @patch("routers.subscriptions.verify_subscription_purchase")
    def test_google_verified_purchase_activates_premium(self, verify):
        expiry = int(time.time() * 1000) + 30 * 24 * 60 * 60 * 1000
        verify.return_value = {
            "product_id": "dalmiren_premium",
            "base_plan_id": "premium_monthly",
            "status": "ACTIVE",
            "expires_at": expiry,
            "started_at": int(time.time() * 1000),
            "order_id": "test-order-001",
            "acknowledged": True,
            "auto_renew_enabled": True,
            "entitled": True,
            "external_account_id": subscriptions.obfuscated_account_id(self.parent.id),
        }

        response = self.client.post(
            "/api/subscriptions/verify",
            json={"purchase_token": "test-purchase-token-123456789"},
        )

        self.assertEqual(response.status_code, 200)
        self.assertEqual(response.json()["tier"], "PREMIUM")
        verify.assert_called_once()

        entitlement = self.db.query(UserEntitlement).filter(
            UserEntitlement.user_id == self.parent.id
        ).first()
        self.assertEqual(entitlement.tier, "PREMIUM")
        self.assertEqual(entitlement.source, "GOOGLE_PLAY")

    @patch("routers.subscriptions.verify_subscription_purchase")
    def test_purchase_token_cannot_be_linked_to_another_account(self, verify):
        token = "test-purchase-token-owned-by-other"
        token_hash = subscriptions.hash_purchase_token(token)

        self.db.add(UserSubscription(
            user_id=self.other_parent.id,
            platform="GOOGLE_PLAY",
            product_id="dalmiren_premium",
            base_plan_id="premium_monthly",
            purchase_token_hash=token_hash,
            status="ACTIVE",
        ))
        self.db.commit()

        verify.return_value = {
            "product_id": "dalmiren_premium",
            "base_plan_id": "premium_monthly",
            "status": "ACTIVE",
            "expires_at": int(time.time() * 1000) + 86_400_000,
            "started_at": int(time.time() * 1000),
            "order_id": "test-order-002",
            "acknowledged": True,
            "auto_renew_enabled": True,
            "entitled": True,
            "external_account_id": subscriptions.obfuscated_account_id(self.parent.id),
        }

        with patch(
            "routers.subscriptions.acknowledge_subscription_purchase"
        ) as acknowledge:
            response = self.client.post(
                "/api/subscriptions/verify",
                json={"purchase_token": token},
            )

        self.assertEqual(response.status_code, 409)
        acknowledge.assert_not_called()

    @patch("routers.subscriptions.acknowledge_subscription_purchase")
    @patch("routers.subscriptions.verify_subscription_purchase")
    def test_acknowledgement_failure_preserves_verified_premium(
        self, verify, acknowledge
    ):
        expiry = int(time.time() * 1000) + 30 * 24 * 60 * 60 * 1000
        verify.return_value = {
            "product_id": "dalmiren_premium",
            "base_plan_id": "premium_monthly",
            "status": "ACTIVE",
            "expires_at": expiry,
            "started_at": int(time.time() * 1000),
            "order_id": "test-order-ack-failure",
            "acknowledged": False,
            "auto_renew_enabled": True,
            "entitled": True,
            "external_account_id": subscriptions.obfuscated_account_id(self.parent.id),
        }
        acknowledge.side_effect = subscriptions.GooglePlayVerificationError(
            "Google Play acknowledgement temporarily unavailable."
        )

        response = self.client.post(
            "/api/subscriptions/verify",
            json={"purchase_token": "test-unacknowledged-purchase-token"},
        )

        self.assertEqual(response.status_code, 200)
        data = response.json()
        self.assertEqual(data["tier"], "PREMIUM")
        self.assertFalse(data["acknowledged"])
        self.assertTrue(data["acknowledgement_required"])
        self.assertIn("temporarily unavailable", data["acknowledgement_error"])

        entitlement = self.db.query(UserEntitlement).filter(
            UserEntitlement.user_id == self.parent.id
        ).first()
        subscription = self.db.query(UserSubscription).filter(
            UserSubscription.user_id == self.parent.id
        ).first()
        self.assertEqual(entitlement.tier, "PREMIUM")
        self.assertFalse(subscription.acknowledged)
        acknowledge.assert_called_once()

    def test_subscription_endpoints_require_authentication(self):
        self.app.dependency_overrides.pop(get_current_user)
        try:
            response = self.client.get("/api/subscriptions/me")
            self.assertEqual(response.status_code, 401)
        finally:
            self.app.dependency_overrides[get_current_user] = (
                lambda: self.parent
            )




    @patch("routers.subscriptions.acknowledge_subscription_purchase")
    @patch("routers.subscriptions.verify_subscription_purchase")
    def test_mismatched_google_account_id_is_rejected(self, verify, acknowledge):
        verify.return_value = {
            "product_id": "dalmiren_premium",
            "base_plan_id": "premium_monthly",
            "status": "ACTIVE",
            "expires_at": int(time.time() * 1000) + 86_400_000,
            "started_at": int(time.time() * 1000),
            "order_id": "test-order-mismatch",
            "acknowledged": False,
            "auto_renew_enabled": True,
            "entitled": True,
            "external_account_id": subscriptions.obfuscated_account_id(self.other_parent.id),
        }
        response = self.client.post(
            "/api/subscriptions/verify",
            json={"purchase_token": "test-mismatched-account-token"},
        )
        self.assertEqual(response.status_code, 409)
        self.assertIsNone(self.db.query(UserSubscription).filter(
            UserSubscription.user_id == self.parent.id
        ).first())
        self.assertIsNone(self.db.query(UserEntitlement).filter(
            UserEntitlement.user_id == self.parent.id
        ).first())
        acknowledge.assert_not_called()

    @patch("routers.subscriptions.acknowledge_subscription_purchase")
    @patch("routers.subscriptions.verify_subscription_purchase")
    def test_missing_google_account_id_is_rejected(self, verify, acknowledge):
        verify.return_value = {
            "product_id": "dalmiren_premium",
            "base_plan_id": "premium_monthly",
            "status": "ACTIVE",
            "expires_at": int(time.time() * 1000) + 86_400_000,
            "started_at": int(time.time() * 1000),
            "order_id": "test-order-missing-account",
            "acknowledged": False,
            "auto_renew_enabled": True,
            "entitled": True,
            "external_account_id": None,
        }
        response = self.client.post(
            "/api/subscriptions/verify",
            json={"purchase_token": "test-missing-account-id-token"},
        )
        self.assertEqual(response.status_code, 409)
        self.assertIsNone(self.db.query(UserSubscription).filter(
            UserSubscription.user_id == self.parent.id
        ).first())
        self.assertIsNone(self.db.query(UserEntitlement).filter(
            UserEntitlement.user_id == self.parent.id
        ).first())
        acknowledge.assert_not_called()

    def test_obfuscated_account_id_is_stable_and_does_not_expose_user_id(self):
        first = subscriptions.obfuscated_account_id(self.parent.id)
        second = subscriptions.obfuscated_account_id(self.parent.id)
        self.assertEqual(first, second)
        self.assertEqual(len(first), 64)
        self.assertNotEqual(first, str(self.parent.id))
        self.assertNotIn(str(self.parent.id), first)

    def test_missing_account_binding_secret_fails_closed(self):
        settings = subscriptions.obfuscated_account_id.__globals__["settings"]
        with patch.object(settings, "GOOGLE_PLAY_ACCOUNT_BINDING_SECRET", ""):
            response = self.client.get("/api/subscriptions/me")
        self.assertEqual(response.status_code, 503)
        self.assertNotIn(str(self.parent.id), response.text)


if __name__ == "__main__":
    unittest.main()
