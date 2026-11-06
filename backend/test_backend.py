from services.auth_service import revoke_refresh_token
from models.db_models import RefreshToken
from services.auth_service import create_refresh_token
from routers import auth as auth_router
from fastapi.testclient import TestClient
from fastapi import FastAPI
import unittest
from database import Base, engine, SessionLocal
from providers.base import ProviderState, AIProviderRequest, AIProviderResponse
from services.health_monitor import HealthMonitor
from services.routing_engine import RoutingPolicyEngine, TaskIntent
from services.memory_nexus import MemoryNexus
from models.db_models import ParentingGoal, FollowUpRecord, ConsentRecord
from prompts.prompt_registry import PromptRegistry
from services.intelligence_loops import IntelligenceLoopsService
from models.db_models import User

class MockProvider:
    def __init__(self, name):
        self._name = name
    @property
    def provider_name(self):
        return self._name
    def is_configured(self):
        return True
    async def health_check(self):
        return ProviderState.AVAILABLE
    async def generate_response(self, req):
        return AIProviderResponse(text="Mock response", provider_name=self._name, model_name="mock-model", latency_ms=10.0)
    async def generate_structured_output(self, req, schema):
        return {"response_summary": "Mock summary"}

class BackendTestCase(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        Base.metadata.create_all(bind=engine)

    def test_prompt_registry(self):
        sys_prompt = PromptRegistry.get_system_prompt("Child Name: Leo, Age Group: TODDLER")
        self.assertIn("DALMIREN AI", sys_prompt)
        self.assertIn("Leo", sys_prompt)
        self.assertEqual(PromptRegistry.get_version(), "1.0.0")

    def test_database_and_memory_nexus(self):
        db = SessionLocal()
        try:
            nexus = MemoryNexus(db)
            mem = nexus.create_memory(
                user_id="test_parent",
                content="Child loves building with blue Lego blocks.",
                category="PREFERENCE"
            )
            self.assertIsNotNone(mem.id)

            retrieved = nexus.retrieve_active_memories("test_parent")
            self.assertTrue(len(retrieved) > 0)
            self.assertIn("Lego", retrieved[0].content)

            updated = nexus.update_memory(mem.id, "test_parent", "Child loves building with Lego and wooden blocks.")
            self.assertEqual(updated.content, "Child loves building with Lego and wooden blocks.")

            deleted = nexus.delete_memory(mem.id, "test_parent")
            self.assertTrue(deleted)
        finally:
            db.close()

    def test_goals_and_followup_persistence(self):
        db = SessionLocal()
        try:
            goal = ParentingGoal(
                user_id="test_parent_goals",
                title="Establish calm bedtime routine",
                description="Read 2 books and turn off screens by 8pm",
                status="ACTIVE"
            )
            db.add(goal)
            db.commit()
            db.refresh(goal)
            self.assertIsNotNone(goal.id)

            followup = FollowUpRecord(
                user_id="test_parent_goals",
                goal_id=goal.id,
                notes="Child fell asleep within 15 minutes tonight.",
                outcome="SUCCESS"
            )
            db.add(followup)
            db.commit()
            db.refresh(followup)
            self.assertIsNotNone(followup.id)
            self.assertEqual(followup.outcome, "SUCCESS")
        finally:
            db.close()

    def test_routing_engine_intents(self):
        local_p = MockProvider("LocalAI")
        openai_p = MockProvider("OpenAI")
        hm = HealthMonitor({"LocalAI": local_p, "OpenAI": openai_p})

        hm.states["LocalAI"] = ProviderState.AVAILABLE
        hm.states["OpenAI"] = ProviderState.AVAILABLE

        router = RoutingPolicyEngine(hm, local_p, openai_p)

        intent_routine = router.classify_intent("How can I encourage my child to eat vegetables?")
        self.assertEqual(intent_routine, TaskIntent.ROUTINE_GUIDANCE)

        intent_complex = router.classify_intent("Synthesize a multi-step plan comparing last week's meltdown triggers.")
        self.assertEqual(intent_complex, TaskIntent.COMPLEX_REASONING)

        intent_safety = router.classify_intent("What if my child swallowed poison?")
        self.assertEqual(intent_safety, TaskIntent.SAFETY_SENSITIVE)

        # Without explicit consent, all intents must remain local.
        provider_routine, _ = router.select_provider(TaskIntent.ROUTINE_GUIDANCE)
        self.assertEqual(provider_routine.provider_name, "LocalAI")

        provider_complex, _ = router.select_provider(TaskIntent.COMPLEX_REASONING)
        self.assertEqual(provider_complex.provider_name, "LocalAI")

        provider_safety, _ = router.select_provider(TaskIntent.SAFETY_SENSITIVE)
        self.assertEqual(provider_safety.provider_name, "LocalAI")

        # Explicit consent permits OpenAI for complex reasoning.
        provider_complex_cloud, _ = router.select_provider(
            TaskIntent.COMPLEX_REASONING,
            allow_cloud=True
        )
        self.assertEqual(provider_complex_cloud.provider_name, "OpenAI")


class DatabaseConsentIntegrationTests(unittest.TestCase):
    def setUp(self):
        self.db = SessionLocal()

        self.parent_a = User(
            email=f"consent-test-a-{__import__('uuid').uuid4().hex}@example.test",
            full_name="Consent Test Parent A"
        )
        self.parent_b = User(
            email=f"consent-test-b-{__import__('uuid').uuid4().hex}@example.test",
            full_name="Consent Test Parent B"
        )

        self.db.add_all([self.parent_a, self.parent_b])
        self.db.commit()
        self.db.refresh(self.parent_a)
        self.db.refresh(self.parent_b)

        self.service = IntelligenceLoopsService(self.db, None)

    def tearDown(self):
        user_ids = [self.parent_a.id, self.parent_b.id]

        try:
            self.db.query(ConsentRecord).filter(
                ConsentRecord.user_id.in_(user_ids)
            ).delete(synchronize_session=False)

            self.db.query(User).filter(
                User.id.in_(user_ids)
            ).delete(synchronize_session=False)

            self.db.commit()
        finally:
            self.db.close()

    def test_missing_consent_denies_cloud_processing(self):
        self.assertFalse(
            self.service.check_cloud_consent(self.parent_a.id)
        )

    def test_grant_and_revocation_preserve_consent_history(self):
        self.assertFalse(
            self.service.check_cloud_consent(self.parent_a.id)
        )

        grant = ConsentRecord(
            user_id=self.parent_a.id,
            consent_type="CLOUD_PROCESSING",
            granted=True,
            timestamp=1000
        )
        self.db.add(grant)
        self.db.commit()

        self.assertTrue(
            self.service.check_cloud_consent(self.parent_a.id)
        )

        revocation = ConsentRecord(
            user_id=self.parent_a.id,
            consent_type="CLOUD_PROCESSING",
            granted=False,
            timestamp=2000
        )
        self.db.add(revocation)
        self.db.commit()

        self.assertFalse(
            self.service.check_cloud_consent(self.parent_a.id)
        )

        history_count = self.db.query(ConsentRecord).filter(
            ConsentRecord.user_id == self.parent_a.id,
            ConsentRecord.consent_type == "CLOUD_PROCESSING"
        ).count()

        self.assertEqual(history_count, 2)

    def test_consent_is_isolated_between_parents(self):
        self.db.add(
            ConsentRecord(
                user_id=self.parent_a.id,
                consent_type="CLOUD_PROCESSING",
                granted=True,
                timestamp=1000
            )
        )
        self.db.commit()

        self.assertTrue(
            self.service.check_cloud_consent(self.parent_a.id)
        )

        self.assertFalse(
            self.service.check_cloud_consent(self.parent_b.id)
        )

class CountingMockProvider(MockProvider):
    def __init__(self, name, should_fail=False):
        super().__init__(name)
        self.call_count = 0
        self.should_fail = should_fail

    async def generate_response(self, req):
        self.call_count += 1
        if self.should_fail:
            raise RuntimeError(f"{self._name} simulated failure")
        return await super().generate_response(req)


class RoutingExecutionTests(unittest.IsolatedAsyncioTestCase):
    def create_router(self, local_state, openai_state,
                      local_fail=False, openai_fail=False):
        local = CountingMockProvider("LocalAI", local_fail)
        openai = CountingMockProvider("OpenAI", openai_fail)

        monitor = HealthMonitor({
            "LocalAI": local,
            "OpenAI": openai
        })
        monitor.states["LocalAI"] = local_state
        monitor.states["OpenAI"] = openai_state

        router = RoutingPolicyEngine(monitor, local, openai)
        request = AIProviderRequest(
            prompt="Help me develop a calm bedtime routine."
        )
        return router, local, openai, request

    async def test_no_consent_uses_local_only(self):
        router, local, openai, request = self.create_router(
            ProviderState.AVAILABLE,
            ProviderState.AVAILABLE
        )

        response = await router.execute_routed_request(
            request,
            allow_cloud=False,
            prefer_cloud=True
        )

        self.assertEqual(response.provider_name, "LocalAI")
        self.assertEqual(local.call_count, 1)
        self.assertEqual(openai.call_count, 0)

    async def test_no_consent_blocks_cloud_fallback(self):
        router, local, openai, request = self.create_router(
            ProviderState.AVAILABLE,
            ProviderState.AVAILABLE,
            local_fail=True
        )

        with self.assertRaisesRegex(RuntimeError, "Cloud fallback is blocked"):
            await router.execute_routed_request(
                request,
                allow_cloud=False
            )

        self.assertEqual(local.call_count, 1)
        self.assertEqual(openai.call_count, 0)

    async def test_no_consent_and_local_unavailable_fails_safely(self):
        router, local, openai, request = self.create_router(
            ProviderState.UNAVAILABLE,
            ProviderState.AVAILABLE
        )

        with self.assertRaisesRegex(RuntimeError, "not permitted"):
            await router.execute_routed_request(
                request,
                allow_cloud=False
            )

        self.assertEqual(local.call_count, 0)
        self.assertEqual(openai.call_count, 0)

    async def test_explicit_consent_allows_openai(self):
        router, local, openai, request = self.create_router(
            ProviderState.AVAILABLE,
            ProviderState.AVAILABLE
        )

        response = await router.execute_routed_request(
            request,
            allow_cloud=True,
            prefer_cloud=True
        )

        self.assertEqual(response.provider_name, "OpenAI")
        self.assertEqual(openai.call_count, 1)
        self.assertEqual(local.call_count, 0)

    async def test_consent_with_openai_unavailable_uses_local(self):
        router, local, openai, request = self.create_router(
            ProviderState.AVAILABLE,
            ProviderState.UNAVAILABLE
        )

        response = await router.execute_routed_request(
            request,
            allow_cloud=True,
            prefer_cloud=True
        )

        self.assertEqual(response.provider_name, "LocalAI")
        self.assertEqual(local.call_count, 1)
        self.assertEqual(openai.call_count, 0)





class LogoutSecurityIntegrationTests(unittest.TestCase):
    def setUp(self):
        import uuid

        self.db = SessionLocal()

        self.parent = User(
            email=f"logout-owner-{uuid.uuid4().hex}@example.test",
            full_name="Logout Owner",
        )
        self.other_parent = User(
            email=f"logout-other-{uuid.uuid4().hex}@example.test",
            full_name="Other Parent",
        )

        self.db.add_all([self.parent, self.other_parent])
        self.db.commit()
        self.db.refresh(self.parent)
        self.db.refresh(self.other_parent)

        self.app = FastAPI()
        self.app.include_router(auth_router.router)

        self.app.dependency_overrides[auth_router.get_db] = (
            lambda: self.db
        )

        self.client = TestClient(self.app)

    def tearDown(self):
        try:
            self.db.query(RefreshToken).filter(
                RefreshToken.user_id.in_(
                    [self.parent.id, self.other_parent.id]
                )
            ).delete(synchronize_session=False)

            self.db.query(User).filter(
                User.id.in_(
                    [self.parent.id, self.other_parent.id]
                )
            ).delete(synchronize_session=False)

            self.db.commit()
        finally:
            self.client.close()
            self.db.close()

    def authenticate_as(self, user):
        self.app.dependency_overrides[
            auth_router.get_current_user
        ] = lambda: user

    def create_token(self, user):
        return create_refresh_token(self.db, user.id)

    def token_record(self, raw_token):
        import hashlib

        token_hash = hashlib.sha256(
            raw_token.encode("utf-8")
        ).hexdigest()

        return self.db.query(RefreshToken).filter(
            RefreshToken.token_hash == token_hash
        ).first()

    def test_logout_requires_authentication(self):
        response = self.client.post(
            "/api/auth/logout",
            json={"refresh_token": "x" * 64},
        )

        self.assertEqual(response.status_code, 401)

    def test_parent_can_revoke_own_refresh_token(self):
        self.authenticate_as(self.parent)
        raw_token = self.create_token(self.parent)

        response = self.client.post(
            "/api/auth/logout",
            json={"refresh_token": raw_token},
        )

        self.assertEqual(response.status_code, 200)
        self.assertEqual(
            response.json()["message"],
            "Signed out successfully.",
        )

        record = self.token_record(raw_token)
        self.assertIsNotNone(record.revoked_at)

    def test_parent_cannot_revoke_another_parents_token(self):
        self.authenticate_as(self.parent)
        raw_token = self.create_token(self.other_parent)

        response = self.client.post(
            "/api/auth/logout",
            json={"refresh_token": raw_token},
        )

        self.assertEqual(response.status_code, 200)

        record = self.token_record(raw_token)
        self.assertIsNotNone(record)
        self.assertIsNone(record.revoked_at)

    def test_unknown_and_already_revoked_tokens_are_safe(self):
        self.authenticate_as(self.parent)

        unknown_token = "u" * 64

        unknown_response = self.client.post(
            "/api/auth/logout",
            json={"refresh_token": unknown_token},
        )
        self.assertEqual(unknown_response.status_code, 200)

        raw_token = self.create_token(self.parent)
        self.assertTrue(
            revoke_refresh_token(
                self.db,
                raw_token,
                self.parent.id,
            )
        )
        self.db.commit()

        revoked_response = self.client.post(
            "/api/auth/logout",
            json={"refresh_token": raw_token},
        )

        self.assertEqual(revoked_response.status_code, 200)
        self.assertIsNotNone(
            self.token_record(raw_token).revoked_at
        )


if __name__ == "__main__":
    unittest.main()
