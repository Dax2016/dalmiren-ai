# Hybrid Routing Engine & Health Architecture

## Core Principle: Zero User Model Selection
Parents using DALMIREN AI interact with a single, unified assistant. All model selection, health checks, and fallback mechanisms are managed automatically by `RoutingPolicyEngine` on the FastAPI backend.

## Health States
- `INITIALISING`: Provider startup in progress.
- `AVAILABLE`: Healthy and ready for requests.
- `DEGRADED`: Provider reachable but specified model missing.
- `UNAVAILABLE`: Offline, network unreachable, or timed out.
- `MISCONFIGURED`: Missing API key or invalid credentials.

## Routing Rules
- **Rule A (Routine Guidance)**: Prefers `LocalAI`. Falls back to `OpenAI` if `LocalAI` is unavailable.
- **Rule B (Summaries & Titles)**: Prefers `LocalAI` for cost & speed efficiency.
- **Rule C (Memory Retrieval)**: Deterministic database query executed prior to LLM routing.
- **Rule D (Complex Contextual Reasoning)**: Prefers `OpenAI` for synthesis across multiple historical conversations or complex parenting plans.
- **Rule E (Memory Extraction)**: Prefers `LocalAI`, validated deterministically before database insertion.
- **Rule F (Safety Sensitive)**: Immediate emergency safety checks bypass lengthy reasoning.
- **Rule G (Provider Failure & Fallback)**: If the selected primary provider fails, the system logs the failure and automatically retries using the alternative eligible provider.
