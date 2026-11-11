import unittest
import uuid

from fastapi import FastAPI
from fastapi.testclient import TestClient

from database import Base, engine, SessionLocal
from models.db_models import User, ChildProfile, ParentingPlan
from routers import auth as auth_router
from routers import parenting_plans as plans_router


class ParentingPlanIntegrationTests(unittest.TestCase):

    @classmethod
    def setUpClass(cls):
        Base.metadata.create_all(bind=engine)

    def setUp(self):
        self.db = SessionLocal()
        suffix = uuid.uuid4().hex

        self.parent = User(
            email=f"plan-owner-{suffix}@example.test",
            full_name="Plan Owner",
        )
        self.other_parent = User(
            email=f"plan-other-{suffix}@example.test",
            full_name="Other Parent",
        )
        self.db.add_all([self.parent, self.other_parent])
        self.db.commit()
        self.db.refresh(self.parent)
        self.db.refresh(self.other_parent)

        self.child = ChildProfile(
            user_id=self.parent.id,
            name="Plan Test Child",
            age_group="SCHOOL_AGE",
        )
        self.other_child = ChildProfile(
            user_id=self.other_parent.id,
            name="Other Parent Child",
            age_group="TODDLER",
        )
        self.db.add_all([self.child, self.other_child])
        self.db.commit()
        self.db.refresh(self.child)
        self.db.refresh(self.other_child)

        self.app = FastAPI()
        self.app.include_router(plans_router.router)
        self.app.dependency_overrides[plans_router.get_db] = lambda: self.db
        self.client = TestClient(self.app)

    def tearDown(self):
        try:
            self.db.query(ParentingPlan).filter(
                ParentingPlan.user_id.in_(
                    [self.parent.id, self.other_parent.id]
                )
            ).delete(synchronize_session=False)

            self.db.query(ChildProfile).filter(
                ChildProfile.user_id.in_(
                    [self.parent.id, self.other_parent.id]
                )
            ).delete(synchronize_session=False)

            self.db.query(User).filter(
                User.id.in_([self.parent.id, self.other_parent.id])
            ).delete(synchronize_session=False)

            self.db.commit()
        finally:
            self.client.close()
            self.db.close()

    def authenticate_as(self, user):
        self.app.dependency_overrides[
            plans_router.get_current_user
        ] = lambda: user

    def create_plan(self):
        return self.client.post(
            "/api/profiles/parenting-plans",
            json={
                "child_id": self.child.id,
                "title": "Build a consistent bedtime routine",
                "summary": "A practical family bedtime plan.",
                "status": "DRAFT",
                "plan_data": {
                    "goals": ["Improve sleep consistency"],
                    "challenges": ["Bedtime resistance"],
                    "strategies": [
                        "Use a predictable bedtime routine",
                        "Offer calm transition reminders",
                    ],
                    "activities": {
                        "daily": ["Read together for 15 minutes"],
                        "weekly": ["Review the routine as a family"],
                    },
                    "expected_outcomes": [
                        "More predictable bedtime transitions"
                    ],
                },
            },
        )

    def test_authentication_is_required(self):
        response = self.client.get("/api/profiles/parenting-plans")
        self.assertEqual(response.status_code, 401)

    def test_parent_can_save_retrieve_and_record_progress(self):
        self.authenticate_as(self.parent)

        created = self.create_plan()
        self.assertEqual(created.status_code, 200, created.text)

        plan = created.json()
        self.assertEqual(plan["title"], "Build a consistent bedtime routine")
        self.assertEqual(plan["child_id"], self.child.id)
        self.assertEqual(
            plan["plan_data"]["goals"],
            ["Improve sleep consistency"],
        )

        progress = self.client.post(
            f"/api/profiles/parenting-plans/{plan['id']}/progress",
            json={
                "notes": "The child followed the routine on four evenings.",
                "outcome": "Improving",
            },
        )
        self.assertEqual(progress.status_code, 200, progress.text)
        self.assertEqual(len(progress.json()["progress_entries"]), 1)

        retrieved = self.client.get(
            f"/api/profiles/parenting-plans/{plan['id']}"
        )
        self.assertEqual(retrieved.status_code, 200)
        self.assertEqual(
            retrieved.json()["progress_entries"][0]["outcome"],
            "Improving",
        )

    def test_parent_can_update_plan_and_filter_list(self):
        self.authenticate_as(self.parent)
        created = self.create_plan()
        self.assertEqual(created.status_code, 200, created.text)
        plan_id = created.json()["id"]

        updated = self.client.put(
            f"/api/profiles/parenting-plans/{plan_id}",
            json={"status": "ACTIVE", "title": "Updated bedtime plan"},
        )
        self.assertEqual(updated.status_code, 200, updated.text)
        self.assertEqual(updated.json()["status"], "ACTIVE")

        filtered = self.client.get(
            "/api/profiles/parenting-plans",
            params={"child_id": self.child.id, "status": "ACTIVE"},
        )
        self.assertEqual(filtered.status_code, 200)
        self.assertEqual(len(filtered.json()), 1)

    def test_other_parent_cannot_read_update_or_delete_plan(self):
        self.authenticate_as(self.parent)
        created = self.create_plan()
        self.assertEqual(created.status_code, 200, created.text)
        plan_id = created.json()["id"]

        self.authenticate_as(self.other_parent)

        retrieved = self.client.get(
            f"/api/profiles/parenting-plans/{plan_id}"
        )
        updated = self.client.put(
            f"/api/profiles/parenting-plans/{plan_id}",
            json={"title": "Unauthorized change"},
        )
        deleted = self.client.delete(
            f"/api/profiles/parenting-plans/{plan_id}"
        )

        self.assertEqual(retrieved.status_code, 404)
        self.assertEqual(updated.status_code, 404)
        self.assertEqual(deleted.status_code, 404)

        self.authenticate_as(self.parent)
        still_exists = self.client.get(
            f"/api/profiles/parenting-plans/{plan_id}"
        )
        self.assertEqual(still_exists.status_code, 200)

    def test_parent_cannot_attach_another_parents_child(self):
        self.authenticate_as(self.parent)

        response = self.client.post(
            "/api/profiles/parenting-plans",
            json={
                "child_id": self.other_child.id,
                "title": "Invalid child association",
                "plan_data": {"goals": ["Test"]},
            },
        )
        self.assertEqual(response.status_code, 404)

    def test_parent_can_delete_own_plan(self):
        self.authenticate_as(self.parent)
        created = self.create_plan()
        self.assertEqual(created.status_code, 200, created.text)

        plan_id = created.json()["id"]
        deleted = self.client.delete(
            f"/api/profiles/parenting-plans/{plan_id}"
        )
        self.assertEqual(deleted.status_code, 200)

        retrieved = self.client.get(
            f"/api/profiles/parenting-plans/{plan_id}"
        )
        self.assertEqual(retrieved.status_code, 404)


if __name__ == "__main__":
    unittest.main()
