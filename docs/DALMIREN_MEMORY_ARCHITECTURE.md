# DALMIREN Persistent Memory Nexus

## Multi-Tier Memory Store
DALMIREN AI organizes parenting memory into 6 persistent tiers:
1. **Episodic Memory**: Specific parenting events, situations, and tantrums.
2. **Semantic Memory**: Core child facts, preferences, triggers, and interests.
3. **Parent & Child Profile Memory**: Age groups, birth dates, family structure, default preferences.
4. **Temporal Memory**: Historical progression, timeline of milestones, and recent mood shifts.
5. **Procedural Memory**: Agreed parenting strategies that have proven effective.
6. **Preference Memory**: Parent communication style preferences (e.g. detailed scripts vs concise tips).

## Database Schema
Supports SQLite for fast local development (`dalmiren.db`) and PostgreSQL with `pgvector` for production deployments. Tables include `users`, `child_profiles`, `conversations`, `messages`, `memories`, `guidance_sessions`, `parenting_goals`, `follow_up_records`, `memory_audit_events`, and `consent_records`.
